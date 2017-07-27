(ns certification-db.components.roles
  (:require [ajax.core :as ajax]
            [re-frame.core :as rf]
            [certification-db.components.forms :as forms]
            [certification-db.components.tables :as tables]
            [certification-db.handlers.api :as ajax-handlers]
            [certification-db.util :as util]))

(defn ajax-get
  [opts]
  (let [defaults {:method :get
                  :format (ajax/json-request-format)
                  :response-format (util/full-response-format ajax/json-response-format)}]
    (ajax/ajax-request (merge defaults opts))))

(defn- certification []
  [:div.col-xs-12.col-sm-10.offset-sm-1.col-md-8.offset-md-2.col-lg-6.offset-lg-3
   [forms/upload-form (.-hash js/location)]
   [:div {:style {:margin-top "15px" :text-align "center"}}
    (let [success @(rf/subscribe [:success])]
      (cond
        (= success true)
        [:p "Your upload was successful"]
        (= success false)
        [:p.bad-login-text "Your upload was unsuccessful. Please try again or contact the Webmaster"]))]])

(defn- hro []
  (ajax-get {:uri "/webtools/api/all-jvas"
             :handler ajax-handlers/all-jvas})
  [:div
   [:div.col-xs-12
    [forms/jva-search]
    [tables/jva-list @(rf/subscribe [:jva-list])]]
   [:div.col-xs-12
    [forms/jva-upload (.-hash js/location)]]])

(defn- procurement []
  (ajax-get {:uri "/webtools/api/all-procurement"
             :handler ajax-handlers/all-procurement})
  [:div
   [:div.col-xs-12
    [forms/procurement-uploads]]
   [:div.col-xs-12
    [forms/rfp-ifb-search]
    [tables/rfp-ifb-list @(rf/subscribe [:rfp-ifb-list])]]])

(defn manage-users []
  (ajax-get {:uri "/webtools/api/all-users"
             :handler ajax-handlers/all-users})
  [:div
   [tables/user-table @(rf/subscribe [:user-list])]
   [:div.col-xs-12
    [forms/invite-users]]])

(defn- manage-db []
  [:div])

(defn display-role [role]
  [:div.row
   (case role
     "Certification" (certification)
     "HRO" (hro)
     "Procurement" (procurement)
     "Manage Users" (manage-users)
     "Manage DB" (manage-db)
     [:div])])
