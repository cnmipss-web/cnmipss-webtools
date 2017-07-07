(ns certification-db.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]
            [certification-db.ajax :refer [load-interceptors!]]
            [certification-db.handlers]
            [certification-db.subscriptions]
            [certification-db.components.forms :as forms])
  (:import goog.History))

(defn nav-link [uri title page collapsed?]
  (let [selected-page (rf/subscribe [:page])]
    [:li.nav-item
     {:class (when (= page @selected-page) "active")}
     [:a.nav-link
      {:href uri
       :on-click #(reset! collapsed? true)} title]]))

(defn navbar []
  [:nav.navbar.navbar-light.bg-faded
   [:div.navbar-header
    [:a.navbar-brand {:href "#/"} "BOE Certification DB"]]])

(defn login-page []
  [:main.container
   [:div.row>div.col-xs-12.col-sm-10.offset-sm-1.col-md-8.offset-md-2.col-lg-6.offset-lg-3
    [forms/login-form]]])

(defn user-page []
  [:main.container
   [:div.row>div.col-xs-12.col-sm-10.offset-sm-1.col-md-8.offset-md-2.col-lg-6.offset-lg-3
    [forms/upload-form]]])

(defn admin-page []
  [:main.container
   [:div.row>div.col-xs-12.col-sm-10.offset-sm-1.col-md-8.offset-md-2.col-lg-6.offset-lg-3
    [forms/upload-form]
    [forms/revert-backup-form]]])

(def pages
  {:home #'login-page
   :user #'user-page
   :admin #'admin-page})

(defn page []
  [:div
   [navbar]
   [(pages @(rf/subscribe [:page]))]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (rf/dispatch [:set-active-page :home]))

(secretary/defroute "/users" []
  (if-let [matches (re-seq #"users\?account=(.*)\&email=(.*)$" (.-hash js/location))]
    (do
      (rf/dispatch [:set-session {:account (get matches 1)
                                  :email (get matches 2)}])
      (rf/dispatch [:set-active-page :user]))
    (do
      (rf/dispatch [:bad-login])
      (set! (.-href js/location) "#/"))))

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
