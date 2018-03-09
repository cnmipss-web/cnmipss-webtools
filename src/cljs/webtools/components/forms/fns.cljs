(ns webtools.components.forms.fns
  (:require [webtools.components.forms.generic :refer [upload-group]]))

(defn fns-upload-form []
  (let [accept ".xlsx"
        multiple false
        action "/webtools/upload/fns-nap"
        path (.-hash js/location)]
    [:form.upload-form {:action action :method "post" :enc-type "multipart/form-data"}
     [:div.row
      [upload-group {:id "fns-file"
                     :accept accept
                     :multiple multiple
                     :label "Upload FNS Registration Records"
                     :class "col-xs-4"}]
      [upload-group {:id "nap-file"
                     :accept accept
                     :multiple multiple
                     :label "Upload NAP Registration Records"
                     :class "col-xs-4"}]

      [:div.form-group.col-xs-4
       [:input {:style {:display "none"}
                :on-change nil
                :type "text"
                :name "path"
                :value path}]
       [:button.btn.btn-primary.upload-form__submit
        {:type "submit"
         :style {:width "100%"
                 :height "100%"}} "Upload"]]]]))

