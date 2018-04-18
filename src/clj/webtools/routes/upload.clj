(ns webtools.routes.upload
  (:require [clojure.tools.logging :as log]
            [compojure.core :refer [POST defroutes]]
            [ring.util.http-response :as response]
            [webtools.config :refer [env]]
            [webtools.error-handler.core :as handle-error]
            [webtools.routes.upload.certification :as cert]
            [webtools.routes.upload.hro :as hro]
            [webtools.routes.upload.fns-nap :as fns-nap]
            [webtools.routes.upload.procurement :as p]))

(defn- post-file-route
  "Handle and respond to a POST request which uploads a file.  Accepts a ring request map, a handler function to process the requests :params value, and the application role to which the user should be returned upon success."
  [r handler role]
  (let [params (get r :params)
        cookie-opts {:max-age 60 :path "/webtools" :http-only false}]
    (try
      (let [code (str (handler params))]
        (-> (response/found (str (env :server-uri) "#/app"))
            (response/set-cookie "wt-success" "true" cookie-opts)
            (response/set-cookie "wt-role" role cookie-opts)
            (response/set-cookie "wt-code" code cookie-opts)
            (response/header "Content-Type" "application/json")))
      (catch Exception ex
        (log/error ex)
        (-> (response/found (str (env :server-uri) "#/app"))
            (response/set-cookie "wt-success" "false" cookie-opts)
            (response/set-cookie "wt-error" (handle-error/msg ex) cookie-opts)
            (response/set-cookie "wt-code" (handle-error/code ex) cookie-opts)
            (response/set-cookie "wt-role" role cookie-opts)
            (response/header "Content-Type" "application/json"))))))

(defroutes upload-routes
  (POST "/upload/certification-csv" req
        (post-file-route req cert/process-cert-csv "Certification"))

  (POST "/upload/jva-pdf" req
        (post-file-route req hro/process-jva-pdf "HRO"))

  (POST "/upload/reannounce-jva" req
        (post-file-route req hro/process-reannouncement "HRO"))

  (POST "/upload/procurement-pdf" req
        (post-file-route req p/process-procurement-pdf "Procurement"))

  (POST "/upload/procurement-addendum" req
        (post-file-route req p/process-procurement-addendum "Procurement"))

  (POST "/upload/fns-nap" req
        (post-file-route req fns-nap/process-upload "FNS-NAP")))
