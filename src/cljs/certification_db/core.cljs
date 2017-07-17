(ns certification-db.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :as ajax]
            [certification-db.ajax :refer [load-interceptors!]]
            [certification-db.handlers]
            [certification-db.subscriptions]
            [certification-db.components.forms :as forms]
            [certification-db.components.nav :as nav]
            [certification-db.components.roles :as roles]
            [certification-db.util :as util])
  (:import goog.History))

(defn main-view [& children]
  [:main.col-sm-9
   [:div.container
    (map-indexed #(with-meta %2 {:key (str "main-view-children-" %1)})  children)]])

(defn login-page []
  [main-view
   [:div.row>div.col-xs-12.col-sm-10.offset-sm-1.col-md-8.offset-md-2.col-lg-6.offset-lg-3
    [forms/login-form]]])
 
(defn user-page []
  [:div.container-fluid>div.row.d-flex.flex-row
   [nav/user-sidebar @(rf/subscribe [:roles])]
   [main-view
    (roles/display-role @(rf/subscribe [:active-role]))]])

(defn admin-page []
  [:div.container-fluid>div.row.d-flex.flex-row
   [nav/admin-sidebar]
   [main-view
    (roles/display-role @(rf/subscribe [:active-role]))]])

(def pages
  {:home #'login-page
   :user #'user-page
   :admin #'admin-page}) 

(defn page []
  [:div
   [nav/navbar]
   [(pages @(rf/subscribe [:page]))]])

;; -------------------------
;; Routes
(defn redirect-bad-login []
  (rf/dispatch [:bad-login])
  (set! (.-href js/location) "#/"))

(defn verified-token?
  [email token]
  (fn [[ok response]]
    (let [admin (get-in response [:body "is-admin"])
          user (get-in response [:body "user"])
          roles (get user "roles")]
      (if ok
        (do
          (rf/dispatch [:set-session {:account token
                                      :email email
                                      :admin admin}])
          (if admin
            (rf/dispatch [:set-active-page :admin])
            (do
              (rf/dispatch [:set-roles (-> roles
                                           (clojure.string/split #","))])
              (rf/dispatch [:set-active-page :user]))))
        (redirect-bad-login)))))

(secretary/set-config! :prefix "/webtools/#")

(secretary/defroute "/" []
  (if-let [matches (re-seq #"login_failed=true" (.-hash js/location))]
    (rf/dispatch [:bad-login]))
  (rf/dispatch [:set-active-page :home]))

(secretary/defroute "/users" []
  (if-let [matches (re-seq #"users\?token=(.*)\&email=(.*org)" (.-hash js/location))]
    (let [token (get (first matches) 1)
          email (get (first matches) 2)
          success (get (first (re-seq #"success=(.*)" (.-hash js/location))) 1)]
      (cond
        (= success "true") (rf/dispatch [:action-success])
        (= success "false") (rf/dispatch [:action-failed]))
      (ajax/ajax-request {:uri "/webtools/api/verify-token"
                          :method :post
                          :format (ajax/json-request-format)
                          :params  {:email email :token token}
                          :response-format (util/full-response-format ajax/json-response-format)
                          :handler (verified-token? email token)
                          :error-handler #(.log js/console %)}))
    (redirect-bad-login)))

(secretary/defroute "/admin" []
  (rf/dispatch [:set-active-page :admin]))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      HistoryEventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (load-interceptors!)
  (hook-browser-navigation!)
  (mount-components))
