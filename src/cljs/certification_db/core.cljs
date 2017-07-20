(ns certification-db.core
  (:require [ajax.core :as ajax]
            cljsjs.jquery
            [certification-db.ajax :refer [load-interceptors!]]
            [certification-db.handlers.api :as ajax-handlers]
            [certification-db.components.forms :as forms]
            [certification-db.components.nav :as nav]
            [certification-db.handlers.reframe]
            [certification-db.handlers.reframe-subs]
            [certification-db.components.roles :as roles]
            [certification-db.components.modals :refer [all-modals]]
            [certification-db.util :as util]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [secretary.core :as secretary])
  (:import goog.History))

(defn main-view [& children]
  [:main#main-container {:on-click #(if @(rf/subscribe [:show-roles?])
                                      (rf/dispatch [:hide-roles]))}
   [:div.container 
    (map-indexed #(with-meta %2 {:key (str "main-view-children-" %1)})  children)]])

(defn login-page []
  [:main#main-container
   [:div.container
    [:div.row>div.col-xs-12.col-sm-10.offset-sm-1.col-md-8.offset-md-2.col-lg-6.offset-lg-3
     [forms/login-form]]]])
 
(defn main-page []
  [:div
   [nav/sidebar @(rf/subscribe [:roles])]
   [main-view
    (roles/display-role @(rf/subscribe [:active-role]))]])

(def pages
  {:login #'login-page
   :main #'main-page}) 

(defn page []
  [:div
   [all-modals]
   [nav/header]
   [(pages @(rf/subscribe [:page]))]])

;; -------------------------
;; Routes
(defn redirect-bad-login []
  (rf/dispatch [:bad-login])
  (set! (.-href js/location) "#/"))

(secretary/set-config! :prefix "/webtools/#")

(secretary/defroute "/" []
  (set! (.-href js/location) "#/login"))

(secretary/defroute "/login" []
  (if-let [matches (re-seq #"login_failed=true" (.-hash js/location))]
    (rf/dispatch [:bad-login]))
  (rf/dispatch [:set-active-page :login]))

(secretary/defroute "/app" []
  (if-let [matches (re-seq #"app\?token=(.*)\&email=(.*org)" (.-hash js/location))]
    (let [token (get (first matches) 1)
          email (get (first matches) 2)
          success (get (first (re-seq #"success=(.*)" (.-hash js/location))) 1)]
      (case success
        "true" (rf/dispatch [:action-success])
        "false" (rf/dispatch [:action-failed])
        "")
      (ajax/ajax-request {:uri "/webtools/api/verify-token"
                          :method :post
                          :format (ajax/json-request-format)
                          :params  {:email email :token token}
                          :response-format (util/full-response-format ajax/json-response-format)
                          :handler (ajax-handlers/verified-token? email token)
                          :error-handler #(.log js/console %)}))
    (redirect-bad-login)))

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
