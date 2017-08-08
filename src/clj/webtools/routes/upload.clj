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
  [record]
  (let [{:keys [close_date]} record
        today (t/now)
        end (coerce/from-date close_date)]
    (if (nil? end)
      (assoc record :status true)
      (if (t/before? today end)
        (assoc record :status true)
        (assoc record :status false)))))

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

(defn process-reannouncement
  [file]
  (let [{:keys [tempfile size filename]} file
          jva (->> tempfile PDDocument/load (.getText (PDFTextStripper.)))
          text-list (split jva #"\n")
          jva-record (as-> (reduce jva-reducer {} text-list) jva
                       (db/make-sql-date jva :open_date)
                       (db/make-sql-date jva :close_date)                       
                       (make-status jva)
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

(def procurement-regexes
  {:ifb_no #"(?i)PSS IFB\#\:\s*(\d{2}\-\d{3})"
   :rfp_no #"(?i)PSS RFP\#\:\s*(\d{2}\-\d{3})"
   :open_date #"(?i)^(\.*OPEN\:\s*)(\w+\s\d{2},\s\d{4})"
   :close_date #"(?i)^(\.*CLOSE\:\s*)(\w+\s\d{2},\s\d{4}\s+at\s+\d{1,2}\:\d{2}\s+am|pm)"
   :title #"(?i)Title\:\s*([\p{L}\p{Z}\p{M}\p{P}\p{N}]+)"})

(defn procurement-reducer [rfp next-line]
  (let [this-line (reduce merge (map (fn [[k re]] {k (line-parser re next-line)}) procurement-regexes))]
    (merge-with select-non-nil this-line rfp)))

(defn process-procurement-pdf
  [file-list]
  (let [{:keys [tempfile size filename]} file-list
        announcement (->> tempfile PDDocument/load (.getText (PDFTextStripper.)))
        desc (-> (re-find #"(?i)Title\:\s*[\p{L}\p{M}\p{P}\n\s\d]*?\n([\p{L}\p{M}\p{P}\n\s\d]+?)\/s\/" announcement)
                 (last)
                 (clojure.string/trim))
        lines (split announcement #"\n")
        record (as->
                 (reduce procurement-reducer {} lines) rec
                 (filter (comp some? val) rec)
                 (map (fn [[k v]] [k (clojure.string/replace v #"\s+" " ")]) rec)
                 (into {} rec)
                 (assoc rec :description desc)
                 (db/make-sql-date rec :open_date)
                 (db/make-sql-datetime rec :close_date)
                 (make-status rec)
                 (assoc rec :id (java.util.UUID/randomUUID))
                 (assoc rec :file_link
                            (wp/create-media filename tempfile
                                             :title (:title rec)
                                             :alt_text (str "Announcement for "
                                                            (if (some? (:rfp_no rec))
                                                              (str "RFP# " (:rfp_no rec))
                                                              (str "IFB# " (:ifb_no rec)))
                                                            " " (:title rec))
                                             :description (-> (:description rec)
                                                              (#(re-find #"([\p{L}\p{Z}\p{P}\p{M}\n]*?)\n\p{Z}\n" %))
                                                              (last)
                                                              (cemerick.url/url-encode))
                                             :slug (:id rec))))]
    (if (some? (:rfp_no record))
      (db/create-rfp! record)
      (db/create-ifb! record))))

(defroutes upload-routes
  (POST "/upload/certification-csv" req
        (post-file-route req process-cert-csv "Certification"))
  (POST "/upload/jva-pdf" req
        (post-file-route req process-jva-pdf "HRO"))
  (POST "/upload/reannounce-jva" req
        (post-file-route req process-reannouncement "HRO"))
  (POST "/upload/rfp-pdf" req
        (post-file-route req process-procurement-pdf "Procurement"))
  (POST "/upload/ifb-pdf" req
        (post-file-route req process-procurement-pdf "Procurement")))
