(ns webtools.components.roles.certification
  (:require [re-frame.core :as rf]
            [webtools.components.grid :as grid]
            [webtools.components.error :as error]
            [webtools.components.forms.certification :as cforms]
            [webtools.components.forms.generic :as gforms]
            [webtools.components.tables.certification :as tables]
            [webtools.cookies :refer [get-cookie]]))

(defn certification-view []
  [grid/row
   [grid/full-width-column
    [gforms/upload-form {:path     (.-hash js/location)
                         :action   "/webtools/upload/certification-csv"
                         :accept   "csv"
                         :label    "Upload CSV File"
                         :multiple false}]]
   [grid/full-width-column
    [error/reporter]]
   [:div {:style {:margin-top "15px" :text-align "center"}}
    (let [wt-success (get-cookie "wt-success")]
      (when (string? wt-success)
        (let [matches (re-find #"(true|false)_?(.*)?" wt-success)
              success (second matches)
              errors  (last matches)]
          (cond
            (= success "true")
            [:p "Your upload was successful"]
            (= success "false")
            [:p.bad-login-text "Your upload was unsuccessful. Please try again or contact the Webmaster"])
          (if errors
            (rf/dispatch [:error-list errors])))))]
   (if-let [errors @(rf/subscribe [:error-list])]
     [:div.col-xs-12.col-sm-10.offset-sm-1
      [tables/error-table errors]])
   [:div.col-xs-12.col-sm-10.offset-sm-1
    [cforms/search-certification-records]
    [tables/existing-certifications]]])
