(ns certification-db.routes.upload
  (:require [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as response]
            [clojure.data :refer [diff]]
            [clojure.data.json :as json]
            [clojure.data.csv :as csv]
            [certification-db.db.core :as db]
            [certification-db.util :refer :all]
            [certification-db.json :refer :all]
            [certification-db.config :refer [env]]
            [certification-db.wordpress-api :as wp]
            [clj-time.core :as t]
            [clj-time.coerce :as coerce])
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

(defn process-cert-csv
  [{:keys [tempfile size filename]}]
  (let [data (->> tempfile slurp csv/read-csv (drop 1) (sort-by #(get % 7)))
        existing-certs (db/get-all-certs)]
    (loop [current (first data) rem (next data)]
      (let [[_ last-name first-name mi _ _ type cert-no start expiry _] current
            fresh-cert (create-new-cert current)]
        (if-let [cert (some (match-cert? cert-no) existing-certs)]
          (if (cert-changed? fresh-cert cert)
            (db/update-cert! fresh-cert))
          (db/create-cert! fresh-cert)))
      (if (> (count rem) 0)
        (recur (first rem) (next rem))))))

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
  (str "Job Vacancy Announcement for " (:position jva) " open from " (:open_date jva) " to " (if-let [close (:close_date jva)]
                                                                                               close
                                                                                               "until filled.")))

(defn process-jva-pdf
  [file-list]
  (if (= (type file-list) clojure.lang.PersistentArrayMap)
    (let [{:keys [tempfile size filename]} file-list
          jva (->> tempfile PDDocument/load (.getText (PDFTextStripper.)))
          text-list (clojure.string/split jva #"\n")
          jva-record (as-> (reduce jva-reducer {} text-list) jva
                       ((fn [j] (println j) j) jva)
                       (db/make-sql-date jva :open_date)
                       (db/make-sql-date jva :close_date)                       
                       ((fn [j] (println j) j) jva)
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
  `(let [file# (get-in ~r [:params :file])]
     (try
       (~p file#)
       (-> (response/found (str (env :server-uri) "#/app" "?success=true" "&role=" ~role ))
           (response/header "Content-Type" "application/json"))
       (catch Exception e#
         (println e#)
         (-> (response/found (str (env :server-uri) "#/app" "?success=false" "&role=" ~role))
             (response/header "Content-Type" "application/json"))))))

(defroutes upload-routes
  (POST "/upload/certification-csv" req
        (post-file-route req process-cert-csv "Certification"))
  (POST "/upload/jva-pdf" req
        (post-file-route req process-jva-pdf "HRO")))
