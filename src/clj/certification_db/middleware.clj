(ns certification-db.middleware
  (:require [certification-db.env :refer [defaults]]
            [clojure.tools.logging :as log]
            [certification-db.layout :refer [*app-context* error-page]]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.middleware.webjars :refer [wrap-webjars]]
            [ring.middleware.json :refer [wrap-json-body]]
            [muuntaja.middleware :refer [wrap-format wrap-params]]
            [certification-db.config :refer [env]]
            [ring.middleware.flash :refer [wrap-flash]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [immutant.web.middleware :refer [wrap-session]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.cors :refer [wrap-cors]])
  (:import [javax.servlet ServletContext]))

(defn wrap-context [handler]
  (fn [request]
    (binding [*app-context*
              (if-let [context (:servlet-context request)]
                ;; If we're not inside a servlet environment
                ;; (for example when using mock requests), then
                ;; .getContextPath might not exist
                (try (.getContextPath ^ServletContext context)
                     (catch IllegalArgumentException _ context))
                ;; if the context is not specified in the request
                ;; we check if one has been specified in the environment
                ;; instead
                (:app-context env))]
      (handler request))))

(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t)
        (error-page {:status 500
                     :title "Something very bad has happened!"
                     :message "We've dispatched a team of highly trained gnomes to take care of the problem."})))))

(defn wrap-csrf [handler]
  (wrap-anti-forgery
    handler
    {:error-response
     (error-page
       {:status 403
        :title "Invalid anti-forgery token"})}))

(defn wrap-formats [handler]
  (let [wrapped (-> handler wrap-params wrap-format)]
    (fn [request]
      ;; disable wrap-formats for websockets
      ;; since they're not compatible with this middleware
      ((if (:websocket? request) handler wrapped) request))))

(defn wrap-uploads [handler]
  (-> handler
      wrap-params
      wrap-multipart-params))

(defn wrap-api [handler]
  (-> handler
      (wrap-cors :access-control-allow-origin #"https?://(localhost.test|cnmipss).*"
                 :access-control-allow-methods [:get])))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      (wrap-json-body {:keywords? true})
      wrap-webjars
      wrap-flash
      (wrap-session {:cookie-attrs {:http-only false}})
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] false)
            (dissoc :session)))
      wrap-context
      wrap-internal-error))
