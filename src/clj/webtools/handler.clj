(ns webtools.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [webtools.layout :refer [error-page]]
            [webtools.routes.home :refer [home-routes]]
            [webtools.routes.oauth :refer [oauth-routes]]
            [webtools.routes.api :refer [api-routes api-routes-with-auth]]
            [webtools.routes.upload :refer [upload-routes]]
            [webtools.routes.download :refer [download-routes]]
            [compojure.route :as route]
            [webtools.env :refer [defaults]]
            [mount.core :as mount]
            [webtools.middleware :as middleware]))

(mount/defstate init-app
                :start ((or (:init defaults) identity))
                :stop  ((or (:stop defaults) identity)))

(def app-routes
  (routes
    (-> #'home-routes
        (wrap-routes middleware/wrap-csrf)
        (wrap-routes middleware/wrap-formats))
    #'oauth-routes
    (-> #'api-routes
        (wrap-routes middleware/wrap-api))
    (-> #'api-routes-with-auth
        (wrap-routes middleware/wrap-webtools-auth)
        (wrap-routes middleware/wrap-api))
    (-> #'upload-routes
        (wrap-routes middleware/wrap-webtools-auth)
        (wrap-routes middleware/wrap-uploads))
    (-> #'download-routes
        (wrap-routes middleware/wrap-webtools-auth)
        (wrap-routes middleware/wrap-uploads))
    (route/not-found
     (:body
      (error-page {:status 404
                   :title "page not found"})))))


(defn app [] (middleware/wrap-base #'app-routes))
