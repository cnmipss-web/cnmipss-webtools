(ns certification-db.routes.upload
  (:require [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as response]
            [clojure.data :refer [diff]]
            [clojure.data.json :as json]
            [clojure.data.csv :as csv]
            [certification-db.db.core :as db]
            [certification-db.util :refer :all]
            [certification-db.config :refer [env]]))

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

(defn process-file
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

(defroutes upload-routes
  (POST "/upload/certification-csv" request
        (let [file (:file (:params request))]
          (try
            (process-file file)
            (-> (response/found (str (env :server-uri) (get-in request [:params :path]) "&success=true"))
                (response/header "Content-Type" "application/json"))
            (catch Exception e
              (-> (response/found (str (env :server-uri) (get-in request [:params :path]) "&success=false"))
                    (response/header "Content-Type" "application/json")))))))
