(ns webtools.components.roles
  (:require [ajax.core :as ajax]
            [cemerick.url :refer [url-decode]]
            [re-frame.core :as rf]
            [webtools.components.forms :as forms]
            [webtools.components.tables :as tables]
            [webtools.handlers.api :as ajax-handlers]
            [webtools.util :as util]
            [webtools.cookies :refer [get-cookie]]
            [webtools.components.roles.fns :refer [fns-view]]))

(defn- error-message
  [error]
  (if error
    [:div.col-xs-12.text-center.mt-1
     [:p#err-msg.slow-fade {:style {:color "red"}}
      (str "Error: " (clojure.string/replace error "+" " "))]]))

(defn- certification []
  [:div.row
   [:div.col-xs-12.col-sm-10.offset-sm-1
    [forms/upload-form {:path (.-hash js/location)
                        :action "/webtools/upload/certification-csv"
                        :accept "csv"
                        :label "Upload CSV File"
                        :multiple false}]
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
  (let [error @(rf/subscribe [:error])]
    [:div.row
     [:div.col-xs-12.col-sm-10.offset-sm-1
      [forms/upload-form {:path (.-hash js/location)
                          :action "/webtools/upload/jva-pdf"
                          :accept ".pdf"
                          :label "Upload New JVA"
                          :multiple true}]]
     (error-message error)
     [:div.col-xs-12.col-sm-10.offset-sm-1
      [forms/jva-search]
      [tables/jva-list @(rf/subscribe [:jva-list])]]]))

(defn- procurement []
  (let [error @(rf/subscribe [:error])]
    [:div.row
     [:div.col-xs-12
      [forms/procurement-upload]]
     (error-message error)
     [:div.col-xs-12
      [tables/rfp-ifb-list @(rf/subscribe [:procurement-list])]]]))

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
    "Certification" (certification)
    "HRO" (hro)
    "Procurement" (procurement)
    "Manage Users" (manage-users)
    "Manage DB" (manage-db)
    "FNS" (fns-view)
    (if (not= ""  (first @(rf/subscribe [:roles])))
      [:div]
      [:div.no-role
       [:p.text-center "You have no assigned roles.  Please contact the Webmaster for more information."]])))
