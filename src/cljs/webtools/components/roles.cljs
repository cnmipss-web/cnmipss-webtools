(ns webtools.components.roles
  (:require [ajax.core :as ajax]
            [cemerick.url :refer [url-decode]]
            [re-frame.core :as rf]
            [webtools.components.forms :as forms]
            [webtools.components.tables :as tables]
            [webtools.handlers.api :as ajax-handlers]
            [webtools.util :as util]
            [webtools.cookies :refer [get-cookie]]
            [webtools.components.roles.fns :refer [fns-view]]
            [webtools.components.roles.certification :refer [certification-view]]
            [webtools.components.roles.hro :refer [hro-view]]
            [webtools.components.roles.procurement :refer [pns-view]]))

(defn- error-message
  [error]
  (if error
    [:div.col-xs-12.text-center.mt-1
     [:p#err-msg.slow-fade {:style {:color "red"}}
      (str "Error: " (clojure.string/replace error "+" " "))]]))

(defn manage-users []
  [:div.row
   [:div.col-xs-12
    [tables/user-table @(rf/subscribe [:user-list])]]
   [:div.col-xs-12
    [forms/invite-users]]])

(defn- manage-db []
  [:div])

(defn display-role [role]
  (case role
    "Certification" [certification-view]
    "HRO"           [hro-view]
    "Procurement"   [pns-view]
    "Manage Users"  [manage-users]
    "Manage DB"     [manage-db]
    "FNS"           [fns-view]
    (if (not= ""  (first @(rf/subscribe [:roles])))
      [:div]
      [:div.no-role
       [:p.text-center "You have no assigned roles.  Please contact the Webmaster for more information."]])))
