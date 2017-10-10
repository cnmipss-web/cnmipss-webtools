(ns webtools.components.roles
  (:require [ajax.core :as ajax]
            [cemerick.url :refer [url-decode]]
            [re-frame.core :as rf]
            [webtools.components.forms :as forms]
            [webtools.components.tables :as tables]
            [webtools.handlers.api :as ajax-handlers]
            [webtools.util :as util]
            [webtools.cookies :refer [get-cookie]]))

(defn- certification []
  [:div.row
   [:div.col-xs-12.col-sm-10.offset-sm-1
    [forms/manage-certifications-form (.-hash js/location)]
    [:div {:style {:margin-top "15px" :text-align "center"}}
     (let [wt-success (get-cookie "wt-success")]
       (when (string? wt-success)
         (let [matches (re-find #"(true|false)_?(.*)?" wt-success)
               success (second matches)
               errors (last matches)]
           (cond
             (= success "true")
             [:p "Your upload was successful"]
             (= success "false")
             [:p.bad-login-text "Your upload was unsuccessful. Please try again or contact the Webmaster"])
           (if errors
             (rf/dispatch [:error-list errors])))))]]
   (if-let [errors @(rf/subscribe [:error-list])]
     [:div.col-xs-12.col-sm-10.offset-sm-1
      (println "Errors: " errors)
      [tables/error-table errors]])
   [tables/existing-certifications]])

(defn- hro []
  [:div.row
   [:div.col-xs-12
    [forms/jva-search]
    [tables/jva-list @(rf/subscribe [:jva-list])]]
   [:div.col-xs-12
    [forms/jva-upload (.-hash js/location)]]])

(defn- procurement []
  [:div.row
   [:div.col-xs-12
    [forms/procurement-uploads]]
   [:div.col-xs-12
    [forms/rfp-ifb-search]
    [tables/rfp-ifb-list @(rf/subscribe [:procurement-list])]]])

(defn manage-users []
  [:div.row
   [tables/user-table @(rf/subscribe [:user-list])]
   [:div.col-xs-12
    [forms/invite-users]]])

(defn- manage-db []
  [:div])

(defn display-role [role]
  (case role
    "Certification" (certification)
    "HRO" (hro)
    "Procurement" (procurement)
    "Manage Users" (manage-users)
    "Manage DB" (manage-db)
    (if (not= ""  (first @(rf/subscribe [:roles])))
      [:div]
      [:div.no-role
       [:p.text-center "You have no assigned roles.  Please contact the Webmaster for more information."]])))
