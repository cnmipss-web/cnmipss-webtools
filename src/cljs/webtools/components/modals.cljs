(ns webtools.components.modals
  (:require [re-frame.core :as rf]
            [webtools.components.forms :as forms]))

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
                      :aria-label "Close"
                      :on-click #(rf/dispatch [:set-edit-jva true])}
       [:span {:aria-hidden "true"} "\u00d7"]]]
     [:div.modal-body
      (if @(rf/subscribe [:edit-jva])
        [forms/edit-jva jva]
        [forms/replace-jva jva])]
     [:div.modal-footer
      [:div.col-xs-2
       [:button.btn.btn-danger.jva-reannounce {:on-click #(rf/dispatch [:set-edit-jva false])} "Re-announce"]
       ]
      [:div.col-xs-4]
      [:div.col-xs-6
       [:button.btn.btn-secondary {:data-dismiss "modal"
                                   :on-click #(rf/dispatch [:set-edit-jva true])} "Exit"]
       [:button.btn.btn-primary {:type "submit" :form "edit-jva"} "Save Changes"]]]]]])

(defn procurement-type
  [item]
  (if (:rfp_no item)
    "Request for Proposal"
    "Invitation for Bid"))

(defn procurement-modal [item]
  [:div#procurement-modal.modal.fade {:role "dialog"
                              :tabIndex "-1"
                              :aria-labelledby "procurement-modal-label"
                              :aria-hidden "true"}
   [:div.modal-dialog {:role "document"}
    [:div.modal-content
     [:div.modal-header
      [:h5#procurement-moda-label.modal-title (str (procurement-type item)  ": " (:title item))]
      [:button.close {:data-dismiss "modal"
                      :aria-label "Close"}
       [:span {:aria-hidden "true"} "\u00d7"]]]
     [:div.modal-body
      [forms/edit-rfp-ifb item]]
     [:div.modal-footer
      [:div.col-xs-2
       ;[:button.btn.btn-danger.procurement-reannounce "Re-announce"]
       ]
      [:div.col-xs-4]
      [:div.col-xs-6
       [:button.btn.btn-secondary {:data-dismiss "modal"} "Exit"]
       [:button.btn.btn-primary {:type "submit" :form "edit-procurement"} "Save Changes"]]]]]])

(defn all-modals
  []
  [:div
   [jva-modal @(rf/subscribe [:jva-modal])]
   [procurement-modal @(rf/subscribe [:procurement-modal])]])
