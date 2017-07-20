(ns certification-db.components.modals
  (:require [re-frame.core :as rf]
            [certification-db.components.forms :as forms]))

(defn jva-modal [jva]
  [:div#jva-modal.modal.fade {:role "dialog"
                              :tabIndex "-1"
                              :aria-labelledby "jva-modal-label"
                              :aria-hidden "true"}
   [:div.modal-dialog {:role "document"}
    [:div.modal-content
     [:div.modal-header
      [:h5#jva-moda-label.modal-title (str "Editing JVA for: " (:position jva))]
      [:button.close {:data-dismiss "modal"
                      :aria-label "Close"}
       [:span {:aria-hidden "true"} "\u00d7"]]]
     [:div.modal-body
      [forms/edit-jva jva]]
     [:div.modal-footer
      [:button.btn.btn-secondary {:data-dismiss "modal"} "Close"]
      [:button.btn.btn-primary {:type "submit" :form "edit-jva"} "Save Changes"]]]]])

(defn all-modals
  []
  [:div
   [jva-modal @(rf/subscribe [:jva-modal])]])
