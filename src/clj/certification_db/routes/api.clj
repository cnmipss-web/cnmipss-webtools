(ns certification-db.routes.api
  (:require [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :as resp]
            [clojure.data.json :as json]
            [certification-db.db.core :as db]
            [certification-db.config :refer [env]]
            [certification-db.util :refer :all]
            [certification-db.json :refer :all]
            [certification-db.layout :refer [error-page]]
            [certification-db.constants :as const]
            [certification-db.wordpress-api :as wp]
            [clojure.tools.logging :as log]))

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
  before returning the results of a query).

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

  (GET "/api/all-procurement" [] (query-route (fn []
                                                {:rfps (db/get-all-rfps)
                                                 :ifbs (db/get-all-ifbs)})))
  
  (POST "/api/verify-token" request
        (if-let [token (get-in request [:cookies "wt-token" :value])]
          (let [email (get-in request [:cookies "wt-email" :value])
                user-email (keyed [email])
                correct-token ((db/get-user-token user-email) :token)
                user (-> (db/get-user-info user-email)
                         (dissoc :id))
                is-admin ((db/is-user-admin? user-email) :admin)]
            (if (= token correct-token)
              (json-response resp/ok (keyed [user is-admin]))
              (resp/forbidden)))
          (resp/forbidden)))
  
  (GET "/logout" request
       (-> (resp/found (str (env :server-uri) "#/login"))
           (resp/set-cookie "wt-token" "" {:max-age 1 :path "/webtools"})
           (resp/set-cookie "wt-email" "" {:max-age 1 :path "/webtools"}))))

(defroutes api-routes-with-auth
  (GET "/api/user" request
       (if-let [email (get-in request [:cookies "wt-email" :value])]
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
                       (db/delete-user! (keyed [email])))))

  (POST "/api/update-jva" {:keys [body]}
        (let [jva (-> body
                      (db/make-sql-date :open_date)
                      (db/make-sql-date :close_date))]
          (query-route db/get-all-jvas (db/update-jva! jva))))

  (POST "/api/delete-jva" {:keys [body]}
        (query-route db/get-all-jvas
                     (try
                       (-> (db/jva-id body)
                           :id
                           .toString
                           (wp/delete-media))
                       (catch Exception e
                         (log/error e)))
                     (db/delete-jva! body)))

  (POST "/api/update-rfp" {:keys [body]}
        (let [rfp (-> body
                      (db/make-sql-date :open_date)
                      (db/make-sql-date :close_date))]
          (query-route db/get-all-rfps (db/update-rfp rfp))))

  (POST "/api/delete-rfp" {:keys [body]}
        (query-route db/get-all-rfps
                     (db/delete-rfp! body)))

  (POST "/api/update-ifb" {:keys [body]}
        (let [ifb (-> body
                      (db/make-sql-date :open_date)
                      (db/make-sql-date :close_date))]
          (query-route db/get-all-ifbs (db/update-ifb ifb))))

  (POST "/api/delete-ifb" {:keys [body]}
        (query-route db/get-all-ifbs (db/delete-ifb! body))))
