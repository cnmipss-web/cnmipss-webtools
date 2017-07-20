(ns certification-db.routes.api
  (:require [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as resp]
            [clojure.data.json :as json]
            [certification-db.db.core :as db]
            [certification-db.util :refer :all]
            [certification-db.json :refer :all]
            [certification-db.layout :refer [error-page]]))

(def truthy (comp some? #{"true" true}))

(defn json-response
  [res body]
  (-> (edn->json body)
      res
      (resp/header "Content-Type" "application/json")))

(defmacro query-route
  "Performs a db query and places the results in the response object as {:body results} in 
  JSON format with JSON headers. 

  Will perform body parameter before making querying (useful for post routes that modify the db
  before returning a query).

  If there is an error, the response object becomes {:body {:error error}}
  in JSON format with JSON headers."
  ([q] `(query-route ~q nil))
  ([q & body]
   `(try
      ~@body
      (json-response resp/ok (~q))
      (catch Exception e#
        (println e#)
        (json-response resp/internal-server-error e#)))))

(defroutes api-routes
  (GET "/api/all-certs" [] (query-route db/get-all-certs))

  (GET "/api/all-jvas" [] (query-route db/get-all-jvas))


  
  (POST "/api/verify-token" request
        (let [{:keys [token email]} (request :body)
              user-email (keyed [email])
              correct-token ((db/get-user-token user-email) :token)
              user (-> (db/get-user-info user-email)
                       (dissoc :id))
              is-admin ((db/is-user-admin? user-email) :admin)]
          (if (= token correct-token)
            (json-response resp/ok (keyed [user is-admin]))
            (resp/forbidden)))))

(defroutes api-routes-with-auth
  (GET "/api/user" request
       (if-let [email (get-in request [:query-params "email"])]
         (if-let [user (-> (db/get-user-info (keyed [email]))
                           (dissoc :id))]
           (json-response resp/ok {:user user})
           (resp/not-found))
         (resp/bad-request)))
  
  (GET "/api/all-users" [] (query-route db/get-all-users))
  
  (POST "/api/update-user" request
        (let [{:keys [email roles admin]} (request :body)]
          (query-route db/get-all-users
                       (db/set-user-roles! (keyed [email roles]))
                       (db/set-user-admin! (keyed [email admin])))))
  
  (POST "/api/create-user" request
        (let [{:keys [email roles]} (request :body)
              admin (-> (get-in request [:body :admin]) truthy)
              id (java.util.UUID/randomUUID)]
          (query-route db/get-all-users
                       (db/create-user! (keyed [email admin roles id])))))
  
  (POST "/api/delete-user" request
        (let [{:keys [email]} (request :body)]
          (query-route db/get-all-users
                       (db/delete-user! (keyed [email]))))))
