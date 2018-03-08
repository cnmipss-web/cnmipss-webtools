(ns webtools.core
  (:require
   [webtools.routes]
   [webtools.actions.reframe]
   [webtools.subscriptions.reframe-subs]
   [webtools.handlers.api :as ajax-handlers]
   [webtools.ajax :refer [load-interceptors!]]
   [webtools.components.forms :as forms]
   [webtools.components.nav :as nav]
   [webtools.components.roles :as roles]
   [webtools.components.modals :refer [all-modals]]
   [webtools.util :as util]
   [webtools.timeout :as timeout]
   [webtools.cookies :refer [get-cookie]]
   [webtools.procurement.front-end]
   [webtools.history :refer [hook-browser-navigation!]]
   [ajax.core :as ajax]
   [cemerick.url :refer [url-decode]]
   cljsjs.jquery
   [markdown.core :refer [md->html]]
   [reagent.core :as r]
   [re-frame.core :as rf]))

(defn main-view [& children]
  [:main#main-container {:on-click #(if @(rf/subscribe [:show-roles?])
                                      (rf/dispatch [:hide-roles]))}
   [:div.container
    (for [[i child] (map-indexed vector children)]
      [:div {:key (random-uuid)} child])]])

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
;; Initialize app
(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (load-interceptors!)
  (hook-browser-navigation!)
  (mount-components)
  (timeout/configure))
