(ns certification-db.components.roles
  (:require [re-frame.core :as rf]
            [certification-db.components.forms :as forms]))

(defn- certification []
  [:div.row>div.col-xs-12.col-sm-10.offset-sm-1.col-md-8.offset-md-2.col-lg-6.offset-lg-3
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

(defn- manage-users []
  [:div])

(defn- manage-db []
  [:div])

(defn display-role [role]
  (case role
    "Certification" (certification)
    "HRO" (hro)
    "Procurement" (procurement)
    "Manage Users" (manage-users)
    "Manage DB" (manage-db)
    [:div]))
