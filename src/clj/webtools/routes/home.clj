(ns webtools.routes.home
  (:require [compojure.core :refer [GET defroutes]]
            [ring.util.http-response :as response]
            [clojure.string :as cstr]
            [webtools.config :refer [env]]
            [webtools.layout :as layout]
            [webtools.db.core :as db]
            [webtools.models.procurement.core :refer [make-uuid]]))

(defroutes home-routes
  (GET "/" request
       (let [host (get-in request [:headers "host"])
             path (:path-info request)]
         (layout/render "home.html")))

  (GET "/unsubscribed/:id" [id :as request]
       (let [raw-sub (db/get-subscription {:id (make-uuid id)})
             sub (assoc raw-sub :type (cstr/upper-case (:type raw-sub)))]
         (if-not (:active sub)
           (layout/render "unsubscribed.html" sub)
           (response/internal-server-error))))

  (GET "/webtools/" request
       (response/found (str (env :server-uri) "/#/app"))))

