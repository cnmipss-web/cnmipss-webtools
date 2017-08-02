(ns webtools.routes.upload
  (:require [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as response]
            [clojure.data :refer [diff]]
            [clojure.data.json :as json]
            [clojure.data.csv :as csv]
            [clojure.string :refer [split]]
            [clj-fuzzy.metrics :as measure]
            [webtools.db.core :as db]
            [webtools.util :refer :all]
            [webtools.json :refer :all]
            [webtools.config :refer [env]]
            [webtools.wordpress-api :as wp]
            [clj-time.core :as t]
            [clj-time.coerce :as coerce]
            [clj-time.format :as f])
  (:import [org.apache.pdfbox.pdmodel PDDocument]
           [org.apache.pdfbox.text PDFTextStripper]))

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
  (let [date-format (f/formatter "MMMM dd, YYYY")
        orig-start (->> orig-cert :start_date (f/parse date-format))
        new-start (->> new-cert :start_date (f/parse date-format))
        orig-expiry (->> orig-cert :expiry_date (f/parse date-format))
        new-expiry (->> new-cert :expiry_date (f/parse date-format))]
    (try
      (if-let [overlap (t/overlap (t/interval orig-start orig-expiry)
                                   (t/interval new-start new-expiry))]
        (> 365 (t/in-days overlap))
        true)
      (catch Exception e
        (println (.getMessage e) orig-start orig-expiry new-start new-expiry "\n\n")))))

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
  [{:keys [tempfile size filename]}]
  (let [data (->> tempfile slurp csv/read-csv (drop 1) (sort-by #(get % 7)))
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
   :salary #"(?i)SALARY\s*:\s*(.*\S)"
   :location #"(?i)^LOCATION\s*:\s*(.*\w)"})

(defn select-non-nil
  [a b]
  (or a b))

(defn line-parser
  [re line]
  (if-let [match (peek (re-find re line))]
    (clojure.string/trim match)
    nil))

(defn jva-reducer
  [jva next-line]
  (let [this-line (reduce merge (map (fn [[k re]] {k (line-parser re next-line)}) jva-regexes))]
    (merge-with select-non-nil this-line jva)))

(defn make-status
  [jva]
  (let [{:keys [close_date]} jva
        today (t/now)
        end (coerce/from-date close_date)]
    (if (nil? end)
      (assoc jva :status true)
      (if (t/before? today end)
        (assoc jva :status true)
        (assoc jva :status false)))))

(defn jva-desc
  [jva]
  (str "Job Vacancy Announcement for "
       (:position jva) " open from " (:open_date jva)
       " to " (if-let [close (:close_date jva)]
                close
                "until filled.")))

(defn process-jva-pdf
  [file-list]
  (if (= (type file-list) clojure.lang.PersistentArrayMap)
    (let [{:keys [tempfile size filename]} file-list
          jva (->> tempfile PDDocument/load (.getText (PDFTextStripper.)))
          text-list (split jva #"\n")
          jva-record (as-> (reduce jva-reducer {} text-list) jva
                       (db/make-sql-date jva :open_date)
                       (db/make-sql-date jva :close_date)                       
                       (make-status jva)
                       (assoc jva :id (java.util.UUID/randomUUID))
                       (assoc jva :file_link
                              (wp/create-media filename tempfile
                                               :title (:position jva)
                                               :alt_text (str "Job Vacancy Announcement for"
                                                              (:position jva))
                                               :description (jva-desc jva)
                                               :slug (:id jva))))]
      (db/create-jva! jva-record))
    (mapv process-jva-pdf file-list)))

(defmacro post-file-route
  [r p role]
  `(let [file# (get-in ~r [:params :file])
         cookie-opts# {:max-age 60 :path "/webtools" :http-only false}]
     (try
       (~p file#)
       (-> (response/found (str (env :server-uri) "#/app" "?role=" ~role ))
           (response/set-cookie "wt-success" "true" cookie-opts#)
           (response/header "Content-Type" "application/json"))
       (catch Exception e#
         (println (.getMessage e#))
         (-> (response/found (str (env :server-uri) "#/app" "?role=" ~role))
             (response/set-cookie "wt-success" (str "false_" (.getMessage e#)) cookie-opts#)
             (response/header "Content-Type" "application/json"))))))

(def rfp-regexes
  {:rfp_no #"(?i)PSS RFP\s*(\d\d\-\d\d\d)"
   :open_date #"(?i)^(\.*beginning\s*)(\w+\s\d\d,\s\d{4})"
   :close_date #"(?i)^(\.*later than\s*)(\w+\s\d\d,\s\d{4})"})

(defn rfp-reducer [rfp next-line]
  (let [this-line (reduce merge (map (fn [[k re]] {k (line-parser re next-line)}) rfp-regexes))]
    (merge-with select-non-nil this-line rfp)))

(defn process-rfp-pdf
  [file-list]
  (let [{:keys [tempfile size filename]} file-list
        rfp (->> tempfile PDDocument/load (.getText (PDFTextStripper.)))
        lines (split rfp #"\n")
        rfp-record (as-> (reduce rfp-reducer {} lines) rfp
                     (println rfp))]))

(defroutes upload-routes
  (POST "/upload/certification-csv" req
        (post-file-route req process-cert-csv "Certification"))
  (POST "/upload/jva-pdf" req
        (post-file-route req process-jva-pdf "HRO"))
  (POST "/upload/rfp-pdf" req
        (post-file-route req process-rfp-pdf "Procurement")))
