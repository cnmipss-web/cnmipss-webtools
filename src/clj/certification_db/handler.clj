(ns certification-db.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [certification-db.layout :refer [error-page]]
            [certification-db.routes.home :refer [home-routes]]
            [certification-db.routes.oauth :refer [oauth-routes]]
            [compojure.route :as route]
            [certification-db.env :refer [defaults]]
            [mount.core :as mount]
            [certification-db.middleware :as middleware]))

(mount/defstate init-app
                :start ((or (:init defaults) identity))
                :stop  ((or (:stop defaults) identity)))

(def app-routes
  (routes
    (-> #'home-routes
        (wrap-routes middleware/wrap-csrf)
        (wrap-routes middleware/wrap-formats))
    #'oauth-routes
    (route/not-found
      (:body
        (error-page {:status 404
                     :title "page not found"})))))


(defn app [] (middleware/wrap-base #'app-routes))
