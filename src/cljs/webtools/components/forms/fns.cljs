(ns webtools.components.forms.fns
  "Forms for users in \"FNS\" role, including private subcomponents."
  (:require [webtools.components.buttons :as btn]
            [webtools.components.forms.generic :as forms]))

(defn fns-upload-form []
  (let [accept ".xlsx"
        action "/webtools/upload/fns-nap"
        path   (.-hash js/location)]
    [:form.upload-form {:action action :method "post" :enc-type "multipart/form-data"}
     [:div.row
      [forms/upload-group {:id     "fns-file"
                           :accept accept
                           :label  "Upload FNS Registration Records"
                           :class  "col-xs-4"}]
      [forms/upload-group {:id     "nap-file"
                           :accept accept
                           :label  "Upload NAP Registration Records"
                           :class  "col-xs-4"}]
      [forms/csrf-token]
      [:div.form-group.col-xs-4
       [forms/hidden-input {:name  "path"
                            :value path}]
       [btn/submit-btn {:text "Upload"}]]]]))

