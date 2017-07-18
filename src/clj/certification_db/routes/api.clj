(ns certification-db.routes.api
  (:require [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as response]
            [clojure.data.json :as json]
            [certification-db.db.core :as db]
            [certification-db.util :refer :all]
            [certification-db.layout :refer [error-page]]))

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
  (GET "/api/all-users" request
       (try
         (build-response {:status 200
                          :users (db/get-all-users)})
         (catch Exception e
             (build-response {:status 500
                              :error e}))))
  (POST "/api/update-user" request
        (let [{:keys [email roles admin]} (request :body)]
          (try
            (db/set-user-roles! (keyed [email roles]))
            (db/set-user-admin! (keyed [email admin]))
            (build-response {:status 200
                             :users (db/get-all-users)})
            (catch Exception e
              (build-response {:status 500
                               :error e})))))
  (POST "/api/create-user" request
        (let [{:keys [email roles]} (request :body)
              admin (-> (get-in request [:body :admin])
                        #{"true" true}
                        some?)
              id (java.util.UUID/randomUUID)]
          (try
            (db/create-user! (keyed [email admin roles id]))
            (build-response {:status 200
                             :users (db/get-all-users)})
            (catch Exception e
              (build-response {:status 500
                               :error e})))))
  (POST "/api/delete-user" request
        (let [{:keys [email]} (request :body)]
          (try
            (db/delete-user! (keyed [email]))
            (build-response {:status 200
                             :users (db/get-all-users)})
            (catch Exception e
              (build-response {:status 500
                               :error e})))))
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
