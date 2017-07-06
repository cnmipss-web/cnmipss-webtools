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
   [:div.row>div.col-xs-12.col-sm-8.offset-sm-2.col-md-6.offset-md-3
    [forms/login-form]]])

(defn user-page []
  [:main.container
   [:div.row>div.col-xs-12.col-sm-8.offset-sm-2.col-md-6.offset-md-3
    [forms/upload-form]]])

(defn admin-page []
  [:main.container
   [:div.row>div.col-xs-12.col-sm-8.offset-sm-2.col-md-6.offset-md-3
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

(secretary/defroute "/user" []
  (rf/dispatch [:set-active-page :user]))

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
(defn fetch-docs! []
  (GET "/docs" {:handler #(rf/dispatch [:set-docs %])}))

(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (load-interceptors!)
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
