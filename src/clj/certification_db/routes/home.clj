(ns certification-db.routes.home
  (:require [certification-db.layout :as layout]
            [certification-db.config :refer [env]]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]))

(defn home-page []
  (layout/render "home.html"))

(defroutes home-routes
  (GET "/" request
       (let [host (get-in request [:headers "host"])
             path (:path-info request)]
         (home-page)))
  (GET "/webtools" request
       (response/found (str (env :server-uri) "/#/app"))))

