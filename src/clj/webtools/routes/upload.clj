(ns webtools.routes.upload
  (:require [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as response]
            [clojure.data :refer [diff]]
            [clojure.data.json :as json]
            [clojure.data.csv :as csv]
            [clojure.string :refer [split]]
            [clojure.tools.logging :as log]
            [clj-fuzzy.metrics :as measure]
            [webtools.db.core :as db]
            [webtools.util :as util]
            [webtools.util.dates :as util-dates]
            [webtools.json :refer :all]
            [webtools.config :refer [env]]
            [webtools.wordpress-api :as wp]
            [webtools.procurement.core :refer :all]
            [webtools.procurement.server :refer [create-pns-from-file]]
            [webtools.email :as email]
            [clj-time.core :as t]
            [clj-time.coerce :as coerce]
            [clj-time.format :as f])
  (:import [org.apache.pdfbox.pdmodel PDDocument]
           [org.apache.pdfbox.text PDFTextStripper]
           [webtools.procurement.core PSAnnouncement]))

(defn create-new-cert
  [current]
  (let [[_ last-name first-name mi _ _ type cert-no start expiry _] current]
    {:cert_no cert-no
     :cert_type type
     :start_date start
     :expiry_date expiry
     :first_name first-name
     :last_name last-name
     :mi mi}))

(defn cert-changed?
  [new orig]
  (let [diffs (diff new orig)]
    (not (nil? (or (first diffs)
                   (second diffs))))))

(defn match-cert?
  [cert-no]
  (fn [cert]
    (if (= cert-no (:cert_no cert))
      cert
      false)))

(defn is-renewal?
  [orig-cert new-cert]
  (let [orig-start (->> orig-cert :start_date util-dates/parse-date)
        new-start (->> new-cert :start_date util-dates/parse-date)
        orig-expiry (->> orig-cert :expiry_date util-dates/parse-date)
        new-expiry (->> new-cert :expiry_date util-dates/parse-date)]
    (try
      (if-let [overlap (t/overlap (t/interval orig-start orig-expiry)
                                   (t/interval new-start new-expiry))]
        (> 365 (t/in-days overlap))
        true)
      (catch Exception e
        (log/error (.getMessage e) orig-start orig-expiry new-start new-expiry "\n\n")))))

(defn renew-cert!
  [cert]
  (let [{:keys [cert_no start_date expiry_date]} cert
        existing-certs (->> (db/get-all-certs)
                            (filter (fn [old-cert]
                                      (re-seq (re-pattern cert_no) (:cert_no old-cert)))))
        renewal-no (count existing-certs)]
    (if (not-any? #(and (= start_date (:start_date %))
                        (= expiry_date (:expiry_date %))) existing-certs)
        (-> (assoc cert :cert_no (str cert_no "-renewal-" renewal-no))
            db/create-cert!))))


(defn handle-collision
  [new-cert orig-cert errors]
  (let [[new-only orig-only joint] (diff new-cert orig-cert)
        same-name? (not-any? nil? (map joint [:first_name :last_name])) 
        same-cert? (some? (:cert_type joint))
        same-dates? (not-any? nil? (map joint [:start_date :expiry_date])) ]
    (if (not (and same-name? same-cert?))
      (swap! errors conj (.getMessage (Exception. (str orig-cert "\n" new-cert "\n\n"))))
      (if (not same-dates?)
        (if (is-renewal? orig-cert new-cert)
          (renew-cert! new-cert)
          (db/update-cert! new-cert))))))

(defn process-cert-csv
  [params]
  (let [{:keys [file]} params
        {:keys [tempfile size filename]} file
        data (->> tempfile slurp csv/read-csv (drop 1) (sort-by #(get % 7)))
        existing-certs (db/get-all-certs)]
    (loop [current (first data) rem (next data) errors (atom [])]
      (let [[_ last-name first-name mi _ _ type cert-no start expiry _] current
            fresh-cert (create-new-cert current)]
        (if-let [cert (some (match-cert? cert-no) (db/get-all-certs))] ;existing-certs vs db/get-all-certs
          (if (cert-changed? fresh-cert cert)
            (handle-collision fresh-cert cert errors))
          (db/create-cert! fresh-cert)))
      (if (> (count rem) 0)
        (recur (first rem) (next rem) errors)
        (if (> (count @errors) 0)
          (throw (Exception. (apply str (take 5 @errors)))))))))

(def jva-regexes
  {:announce_no #"(?i)ANNOUNCEMENT\s*NO\.?:\s*(.*\S)"
   :position #"(?i)(POSITION/TITLE)\s*:\s*(.*\S)"
   :open_date #"(?i)OPEN(ING)?\s*DATE\s*:\s*(.*)\s*CL"
   :close_date #"(?i)CLOSE?(ING)?\s*DATE:\s*(.*\S)"
   :salary #"(?i)(SALARY|DIFFERENTIAL)\s*:\s*(.*\S)"
   :location #"(?i)^LOCATION\s*:\s*(.*\w)"})

(defn jva-reducer
  [jva next-line]
  (let [this-line (reduce merge (map (fn [[k re]] {k (util/line-parser re next-line)}) jva-regexes))]
    (merge-with util/select-non-nil this-line jva)))

(defn jva-desc
  [jva]
  (str "Job Vacancy Announcement for "
       (:position jva) " open from " (:open_date jva)
       " to " (if-let [close (:close_date jva)]
                close
                "until filled.")))

(defn process-jva-pdf
  [params]
  (let [{:keys [file]} params]
    (if (= (type file) clojure.lang.PersistentArrayMap)
      (let [{:keys [tempfile size filename]} file
            jva (->> tempfile PDDocument/load (.getText (PDFTextStripper.)))
            text-list (split jva #"\n")
            jva-record (as-> (reduce jva-reducer {} text-list) jva
                         (db/make-sql-date jva :open_date)
                         (db/make-sql-date jva :close_date)                       
                         (util/make-status jva)
                         (assoc jva :id (java.util.UUID/randomUUID))
                         (assoc jva :file_link
                                (wp/create-media filename tempfile
                                                 :title (:position jva)
                                                 :alt_text (str "Job Vacancy Announcement for "
                                                                (:position jva))
                                                 :description (jva-desc jva)
                                                 :slug (:id jva))))]
        (try
          (db/create-jva! jva-record)
          (catch java.sql.BatchUpdateException e
            (log/error e))))
      (mapv (comp process-jva-pdf #(into {} [[:file %]])) file))))

(defn process-reannouncement
  [params]
  (let [{:keys [file]} params
        {:keys [tempfile size filename]} file
          jva (->> tempfile PDDocument/load (.getText (PDFTextStripper.)))
          text-list (split jva #"\n")
          jva-record (as-> (reduce jva-reducer {} text-list) jva
                       (db/make-sql-date jva :open_date)
                       (db/make-sql-date jva :close_date)                       
                       (util/make-status jva)
                       (assoc jva :id (java.util.UUID/randomUUID)))
        existing-jva (db/get-jva jva-record)]
    (db/delete-jva! existing-jva)
    (wp/delete-media (str (:id existing-jva)))
    (->
     (assoc jva-record :file_link
            (wp/create-media filename tempfile
                             :title (:position jva-record)
                             :alt_text (str "Job Vacancy Announcement for"
                                            (:position jva-record))
                             :description (jva-desc jva-record)
                             :slug (:id jva-record)))
     (db/create-jva!))))

(defmacro post-file-route
  [r p role]
  `(let [params# (get ~r :params)
         cookie-opts# {:max-age 60 :path "/webtools" :http-only false}]
     (try
       (~p params#)
       (-> (response/found (str (env :server-uri) "#/app" "?role=" ~role ))
           (response/set-cookie "wt-success" "true" cookie-opts#)
           (response/header "Content-Type" "application/json"))
       (catch Exception e#
         ;(println (.getMessage e#))
         (-> (response/found (str (env :server-uri) "#/app" "?role=" ~role))
             (response/set-cookie "wt-success" (str "false_" (.getMessage e#)) cookie-opts#)
             (response/header "Content-Type" "application/json"))))))

(defn process-procurement-pdf
  [params]
  (let [{:keys [file]} params]
    (if (= (type file) clojure.lang.PersistentArrayMap)
      (let [pns (create-pns-from-file file)]
        (save-to-db pns))
      (mapv (comp process-procurement-pdf #(into {} [[:file %]])) file))))

(defn process-procurement-addendum
  [params]
  (let [{:keys [file id number type]} params
        {:keys [tempfile size filename]} file
        uuid (make-uuid id)
        slug (java.util.UUID/randomUUID)
        existing-addenda (filter #(= id (:proc_id %)) (db/get-all-addenda))
        file_link (wp/create-media filename tempfile
                                   :title (str "Addendum " (inc (count existing-addenda))
                                               " for "
                                               (clojure.string/upper-case (name type))
                                               " " number)
                              :slug slug)]
    (when (some? file_link)
      (future (email/notify-subscribers :addenda (get-pns-from-db uuid) {:file_link file_link
                                                                         :proc_id uuid}))
      (db/create-addendum! {:id slug
                            :file_link file_link
                            :proc_id uuid
                            :number (inc (count existing-addenda))}))
    (if (nil? file_link) (throw (Exception. "Error uploading addendum to Wordpress")))))

(defroutes upload-routes
  (POST "/upload/certification-csv" req
        (post-file-route req process-cert-csv "Certification"))
  (POST "/upload/jva-pdf" req
        (post-file-route req process-jva-pdf "HRO"))
  (POST "/upload/reannounce-jva" req
        (post-file-route req process-reannouncement "HRO"))
  (POST "/upload/procurement-pdf" req
        (post-file-route req process-procurement-pdf "Procurement"))
  (POST "/upload/procurement-addendum" req
        (post-file-route req process-procurement-addendum "Procurement")))
