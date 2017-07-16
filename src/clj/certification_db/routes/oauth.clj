(ns certification-db.routes.oauth
  (:require [certification-db.db.core :as db]
            [certification-db.config :refer [env]]
            [certification-db.auth :as auth]
            [certification-db.util :refer :all]
            [clj-http.client :as http]
            [cemerick.url :refer [url-decode]]
            [clj-oauth2.client :as oauth2]
            [clojure.data.json :as json]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as respond]
            [clojure.java.io :as io]))

(defn config<-
  [env]
  (merge auth/default-config {:client-id (:google-client-id env)
                              :client-secret (:google-secret-id env)
                              :redirect-uri (:google-callback-uri env)}))

(defroutes oauth-routes
  (GET "/oauth/oauth-init" []
       (-> (config<- env)
           (auth/request-auth-url)
           (respond/found)))
  (GET "/oauth/oauth-callback" request
       (let [token (-> (config<- env)
                       (auth/get-tokens (get-in request [:params :code]))
                       (get :access_token))
             email (-> token
                       (#(str "https://www.googleapis.com/oauth2/v1/userinfo?"
                              "fields=email%2Cname&access_token=" %))
                       (http/get)
                       (get :body)
                       (json/read-str)
                       (get "email"))
             user (db/get-user-info (keyed [email]))]
         (if (re-seq #"cnmipss.org$" email)
           (do
             (if user
               (db/set-user-token! (keyed [email token]))
               (let [admin false
                     id (java.util.UUID/randomUUID)]
                 (db/create-user! (keyed [email token admin id]))))
             (-> (respond/found (str "/webtools/#/users?token=" token
                                     "&email=" email))
                 (respond/set-cookie "token" token)
                 (respond/set-cookie "email" email)))
           (respond/found "/webtools/#/?login_failed=true")))))
