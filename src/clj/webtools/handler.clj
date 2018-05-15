(ns webtools.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [compojure.route :as route]
            [mount.core :as mount]
            [webtools.env :refer [defaults]]
            [webtools.layout :refer [error-page]]
            [webtools.middleware :as middleware]
            [webtools.routes.api :refer [api-routes
                                         api-routes-with-auth]]
            [webtools.routes.download :refer [download-routes]]
            [webtools.routes.home :refer [home-routes]]
            [webtools.routes.oauth :refer [oauth-routes]]
            [webtools.routes.upload :refer [upload-routes]]))

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
                   :title "Page not found"
                   :message "Sorry, that resource cannot be located."})))))


(defn app [] (middleware/wrap-base #'app-routes))
