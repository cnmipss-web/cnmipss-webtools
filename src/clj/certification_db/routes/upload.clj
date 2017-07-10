(ns certification-db.routes.upload
  (:require [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as response]
            [clojure.data.json :as json]
            [clojure.data.csv :as csv]
            [certification-db.db :as db]
            [certification-db.util :refer :all]))

(defn process-file
  [{:keys [tempfile size filename]}]
  (println tempfile size filename)
  (let [data (-> (slurp tempfile)
                 (csv/read-csv))]
    (loop [current (second data)
           rem (next (next data))]
      (let [[_ last-name first-name mi _ _ type cert-no start expiry _] current
            fresh-cert {:certification/cert-no cert-no
                        :certification/type type
                        :certification/start-date start
                        :certification/expiry-date expiry
                        :certification/first-name first-name
                        :certification/last-name last-name
                        :certification/mi mi}]
        (if-let [cert (db/get-cert cert-no)]
          (do
            (println "Updating..." cert)
            (db/update-cert (merge {:db/id [:certification/cert-no cert-no]}
                                   fresh-cert)))
          (db/post-cert fresh-cert)))
      (if (> (count rem) 0)
        (recur (first rem) (next rem))))))

(defroutes upload-routes
  (POST "/upload/certification-csv" request
        (let [file (:file (:params request))]
          (process-file file)
          (-> (response/ok "")
              (response/header "Content-Type" "application/json")))))
