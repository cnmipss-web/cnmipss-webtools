(ns certification-db.routes.oauth
  (:require [certification-db.layout :as layout]
            [certification-db.auth :as auth]
            [certification-db.db :as db]
            [clj-http.client :as http]
            [cemerick.url :refer [url-decode]]
            [clj-oauth2.client :as oauth2]
            [clojure.data.json :as json]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as respond]
            [clojure.java.io :as io]))

(def oauth-config (merge auth/default-config
                         {:client-id "766605916043-6gds3iktlhvq10jnvtqfjdffrf7ms1ug.apps.googleusercontent.com"
                          :client-secret "IZG-sgbL1uJKhPNU9lw8iv4c"
                          :redirect-uri "http://localhost:3000/oauth/oauth-callback"}))

(defroutes oauth-routes
  (GET "/oauth/oauth-init" []
       (respond/found (auth/request-auth-url oauth-config)))
  (GET "/oauth/oauth-callback" request
       (let [code (get-in request [:params :code])
             token ((auth/get-tokens oauth-config code) :access_token)
             acc-info (json/read-str (:body (http/get (str "https://www.googleapis.com/oauth2/v1/userinfo?"
                              "fields=email%2Cname&access_token="
                              token))))
             email (get-in acc-info ["email"])]
         (if (re-seq #"cnmipss.org$" email)
           (do
             (db/set-user-token email token)
             (respond/found (str "/#/users?token=" token
                                 "&email=" email)))
           (respond/found "/#/?login_failed=true")))))
