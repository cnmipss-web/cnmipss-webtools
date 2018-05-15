(ns webtools.routes
  (:require [ajax.core :as ajax]
            [cemerick.url :refer [url-decode]]
            [re-frame.core :as rf]
            [secretary.core :as secretary]
            [webtools.cookies :refer [get-cookie]]
            [webtools.util :as util]
            [webtools.handlers.api :as ajax-handlers]))

;; -------------------------
;; Routes
(secretary/set-config! :prefix "/webtools/#")

(secretary/defroute "/" []
  (set! (.-href js/location) "#/app"))

(secretary/defroute "/login" []
  (if-let [matches (re-seq #"login_failed=true" (.-hash js/location))]
    (rf/dispatch [:bad-login true])
    (rf/dispatch [:bad-login false]))
  (rf/dispatch [:set-active-page :login]))

(secretary/defroute "/app" []
  (let [token (get-cookie :wt-token)
        email (url-decode (get-cookie :wt-email))
        success (get-cookie :wt-success)
        current-role (get-cookie :wt-role)
        error (get-cookie :wt-error)
        code (get-cookie :wt-code)]
    
    (case success
      "true" (rf/dispatch [:action-success]) 
      "false" (rf/dispatch [:action-failed])
      "")
    
    (if current-role
      (do 
        (rf/dispatch [:set-active-role current-role])
        (rf/dispatch [:hide-roles]))
      (rf/dispatch [:set-active-role nil]))

    (if error
      (rf/dispatch [:display-error error]))
      
    (ajax/ajax-request {:uri "/webtools/api/verify-token"
                        :method :post
                        :format (ajax/json-request-format)
                        :params {:email email :token token}
                        :response-format (util/full-response-format ajax/json-response-format)
                        :handler ajax-handlers/verified-token?
                        :error-handler #(.log js/console %)})))

