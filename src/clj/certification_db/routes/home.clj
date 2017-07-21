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
       (home-page))
  (GET "/webtools/" request
       (response/found (env :server-uri))))

