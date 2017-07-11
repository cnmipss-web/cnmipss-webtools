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

(defroutes oauth-routes
  (GET "/oauth/oauth-init" []
       (let [oauth-config (merge auth/default-config
                                 {:client-id (:google-client-id env)
                                  :client-secret (:google-secret-id env)
                                  :redirect-uri "http://localhost:3000/oauth/oauth-callback"})]
         (respond/found (auth/request-auth-url oauth-config))))
  (GET "/oauth/oauth-callback" request
       (let [oauth-config (merge auth/default-config
                                 {:client-id (:google-client-id env)
                                  :client-secret (:google-secret-id env)
                                  :redirect-uri "http://localhost:3000/oauth/oauth-callback"})
             code (get-in request [:params :code])
             token ((auth/get-tokens oauth-config code) :access_token)
             acc-info (json/read-str (:body (http/get (str "https://www.googleapis.com/oauth2/v1/userinfo?"
                              "fields=email%2Cname&access_token="
                              token))))
             email (get-in acc-info ["email"])
             user (db/get-user-info (keyed [email]))]
         (if (re-seq #"cnmipss.org$" email)
           (do
             (if user
               (db/set-user-token! (keyed [email token]))
               (let [admin false
                     id (java.util.UUID/randomUUID)]
                   (db/create-user! (keyed [email token admin id]))))
             (-> (respond/found (str "/#/users?token=" token
                                     "&email=" email))
                 (respond/set-cookie "token" token)
                 (respond/set-cookie "email" email)))
           (respond/found "/#/?login_failed=true")))))
