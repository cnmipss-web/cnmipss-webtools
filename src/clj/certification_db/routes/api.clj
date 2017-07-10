(ns certification-db.routes.api
  (:require [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as response]
            [clojure.data.json :as json]
            [certification-db.db :as db]
            [certification-db.util :refer :all]))

(defn build-response
  [body]
  (-> (edn->json body)
      (response/ok)
      (response/header "Content-Type" "application/json")))

(defroutes api-routes
  (GET "/api/lookup" []
       (let [certs (db/get-all-certs)]
         (build-response certs)))
  (GET "/api/user" request
       (if-let [email (get-in request [:query-params "email"])]
         (if-let [user (db/get-user-info email)]
           (build-response {:status 200
                            :user user})
           (response/not-found))
         (response/bad-request)))
  (POST "/api/verify-token" request
        (let [body (request :body)
              {:keys [token email]} body
              correct-token (db/get-user-token email)
              user (db/get-user-info email)
              is-admin (> (count (db/user-admin? email)) 0)]
          (if (= token correct-token)
            (build-response {:user user
                             :isAdmin is-admin})
            (response/forbidden)))))
