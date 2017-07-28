(ns webtools.routes.home
  (:require [webtools.layout :as layout]
            [webtools.config :refer [env]]
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

