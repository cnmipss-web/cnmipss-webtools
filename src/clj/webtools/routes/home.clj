(ns webtools.routes.home
  (:require [compojure.core :refer [GET defroutes]]
            [ring.util.http-response :as response]
            [webtools.config :refer [env]]
            [webtools.layout :as layout]))

(defn home-page []
  (layout/render "home.html"))

(defroutes home-routes
  (GET "/" request
       (let [host (get-in request [:headers "host"])
             path (:path-info request)]
         (home-page)))
  (GET "/webtools" request
       (response/found (str (env :server-uri) "/#/app"))))

