(ns webtools.components.roles.certification
  (:require [re-frame.core :as rf]
            [webtools.components.forms :as forms]
            [webtools.components.error :as error]
            [webtools.components.tables :as tables]
            [webtools.cookies :refer [get-cookie]]))

(defn certification-view []
  [:div.row
   [:div.col-xs-12
    [forms/upload-form {:path (.-hash js/location)
                        :action "/webtools/upload/certification-csv"
                        :accept "csv"
                        :label "Upload CSV File"
                        :multiple false}]]
   [:div.col-xs-12.text-center.mt-1
    [error/reporter]]
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
            (rf/dispatch [:error-list errors])))))]
   (if-let [errors @(rf/subscribe [:error-list])]
     [:div.col-xs-12
      (println "Errors: " errors)
      [tables/error-table errors]])
   [:div.col-xs-12
    [forms/cert-search "Search Certification Records"]
    [tables/existing-certifications]]])
