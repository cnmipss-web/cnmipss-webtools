(ns certification-db.routes.upload
  (:require [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as response]
            [clojure.data.json :as json]
            [clojure.data.csv :as csv]
            [certification-db.db.core :as db]
            [certification-db.util :refer :all]))

(defn process-file
  [{:keys [tempfile size filename]}]
  (println tempfile size filename)
  (let [data (-> (slurp tempfile)
                 (csv/read-csv))]
    (loop [current (second data)
           rem (next (next data))]
      (let [[_ last-name first-name mi _ _ type cert-no start expiry _] current
            fresh-cert {:cert_no cert-no
                        :cert_type type
                        :start_date start
                        :expiry_date expiry
                        :first_name first-name
                        :last_name last-name
                        :mi mi}]
        (if-let [cert (db/get-cert {:cert_no cert-no})]
          (do
            (db/update-cert! fresh-cert))
          (db/create-cert! fresh-cert)))
      (if (> (count rem) 0)
        (recur (first rem) (next rem))))))

(defroutes upload-routes
  (POST "/upload/certification-csv" request
        (let [file (:file (:params request))]
          (println (:params request))
          (try
            ;(process-file file)
            (-> (response/found (str "/" (get-in request [:params :path]) "&success=true"))
                (response/header "Content-Type" "application/json"))
            (catch Exception e
              (-> (response/found (str "/" (get-in request [:params :path]) "&success=false"))
                    (response/header "Content-Type" "application/json")))))))
