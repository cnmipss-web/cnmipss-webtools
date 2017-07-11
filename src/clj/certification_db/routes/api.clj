(ns certification-db.routes.api
  (:require [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as response]
            [clojure.data.json :as json]
            [certification-db.db.core :as db]
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
         (if-let [user (-> (db/get-user-info (keyed [email]))
                           (dissoc :id))]
           (build-response {:status 200
                            :user user})
           (response/not-found))
         (response/bad-request)))
  (POST "/api/verify-token" request
        (let [{:keys [token email]} (request :body)
              user-email (keyed [email])
              correct-token ((db/get-user-token user-email) :token)
              user (-> (db/get-user-info user-email)
                       (dissoc :id))
              is-admin ((db/is-user-admin? user-email) :admin)]
          (if (= token correct-token)
            (build-response (keyed [user is-admin]))
            (response/forbidden)))))
