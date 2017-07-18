(ns certification-db.components.roles
  (:require [ajax.core :as ajax]
            [re-frame.core :as rf]
            [certification-db.components.forms :as forms]
            [certification-db.components.tables :as tables]
            [certification-db.handlers.api :as ajax-handlers]
            [certification-db.util :as util]))

(defn- certification []
  [:div.col-xs-12.col-sm-10.offset-sm-1.col-md-8.offset-md-2.col-lg-6.offset-lg-3
   [forms/upload-form (.-hash js/location)]
   [:div {:style {:margin-top "15px" :text-align "center"}}
    (let [success @(rf/subscribe [:success])]
      (cond
        (= success true)
        [:p "Your action was successful"]
        (= success false)
        [:p.bad-login-text "Your action was unsuccessful. Please try again or contact the Webmaster"]))]])

(defn- hro []
  [:div])

(defn- procurement []
  [:div])

(defn manage-users []
  (ajax/ajax-request {:uri "/webtools/api/all-users"
                      :method :get
                      :format (ajax/json-request-format)
                      :response-format (util/full-response-format ajax/json-response-format)
                      :handler ajax-handlers/all-users})
  [:div
   [:div.col-xs-12
    [tables/user-table @(rf/subscribe [:user-list])]]
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
