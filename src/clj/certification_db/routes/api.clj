(ns certification-db.routes.api
  (:require [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as respond]
            [clojure.data.json :as json]
            [certification-db.db :as db]))

(defroutes api-routes
  (GET "/api/user" request
       (do
         (println (request :query-params))
         ;(db/)
         (-> (json/write-str {:status "Success!"})
             (respond/ok)
             (respond/header "Content-Type" "application/json"))))
  (POST "/api/user/token" request
        (let [body (json/read-str (request :body))
              token (body :token)
              email (body :email)
              user (db/get-user-info email)]
          ())))
