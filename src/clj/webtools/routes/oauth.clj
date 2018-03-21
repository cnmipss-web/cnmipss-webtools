(ns webtools.routes.oauth
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [compojure.core :refer [GET defroutes]]
            [ring.util.http-response :as respond]
            [webtools.auth :as auth]
            [webtools.config :refer [env]]
            [webtools.constants :refer [max-cookie-age]]
            [webtools.db.core :as db]
            [webtools.util :refer :all]))

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
       (try
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
               user (db/get-user-info (keyed [email]))
               cookie-opts {:http-only true :max-age max-cookie-age :path "/webtools"}]
           (if (re-seq #"cnmipss.org$" email)
             (do
               (if user
                 (db/set-user-token! (keyed [email token]))
                 (let [admin false
                       id (java.util.UUID/randomUUID)
                       roles nil]
                   (db/create-user! (keyed [email token admin id roles]))))
               (-> (respond/found (str (env :server-uri) "#/app"))
                   (respond/set-cookie "wt-token" token cookie-opts)
                   (respond/set-cookie "wt-email" email cookie-opts)))
             (respond/found "/#/login?login_failed=true")))
         (catch Exception e
           (log/error e)
           (respond/internal-server-error {:message (.getMessage e)})))))
