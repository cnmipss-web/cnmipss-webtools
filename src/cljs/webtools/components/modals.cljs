(ns webtools.components.modals
  (:require [re-frame.core :as rf]
            [webtools.components.forms :as forms]
            [webtools.components.tables :as tables]))

(defn cert-modal [cert]
  [:div#cert-modal.modal.fade {:role "dialog"
                              :tabIndex "-1"
                              :aria-labelledby "cert-modal-label"
                              :aria-hidden "true"}
   [:div.modal-dialog {:role "document"}
    [:div.modal-content
     [:div.modal-header
      [:h5#cert-modal-label.modal-title (str "Editing Certifiction for: " (:cert_no cert))]
      [:button.close {:data-dismiss "modal"
                      :aria-label "Close"}
       [:span {:aria-hidden "true"} "\u00d7"]]]
     [:div.modal-body
      [forms/edit-cert cert]]
     [:div.modal-footer
      [:div.col-xs-2]
      [:div.col-xs-4]
      [:div.col-xs-6
       [:button.btn.btn-secondary {:data-dismiss "modal"} "Exit"]
       [:button.btn.btn-primary {:type "submit" :form "edit-cert"} "Save Changes"]]]]]])

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
       (if @(rf/subscribe [:edit-jva])
         [:button.btn.btn-danger.jva-reannounce {:on-click #(rf/dispatch [:set-edit-jva false])} "Re-announce"])]
      [:div.col-xs-4]
      [:div.col-xs-6
       [:button.btn.btn-secondary {:data-dismiss "modal"
                                   :on-click #(rf/dispatch [:set-edit-jva true])} "Exit"]
       [:button.btn.btn-primary {:type "submit" :form "edit-jva"} "Save Changes"]]]]]])

(defn procurement-type
  [item]
  (if (= :rfp (:type item))
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
      [:h2#procurement-modal-label.modal-title (str (procurement-type item)  ": " (:title item))]
      [:button.close {:data-dismiss "modal"
                      :aria-label "Close"}
       [:span {:aria-hidden "true"} "\u00d7"]]]
     [:div.modal-body
      (if @(rf/subscribe [:add-addendum])
        [:div
         [tables/existing-addenda item]
         [forms/procurement-addendum item]]
        [forms/edit-procurement item])]
     [:div.modal-footer
      [:div.col-xs-3
       (if (and (not @(rf/subscribe [:add-addendum]))
                (not @(rf/subscribe [:change-specs])))
         [:button.btn.btn-danger.procurement-addendum {:on-click #(rf/dispatch [:add-addendum true])} "Addendum"])]
      [:div.col-xs-3
       (if (and (not @(rf/subscribe [:add-addendum]))
                (not @(rf/subscribe [:change-specs])))
         [:button.btn.btn-info.procurement-addendum {:on-click #(rf/dispatch [:change-specs true])} "Change Specs"])]

      [:div.col-xs-6
       [:button.btn.btn-secondary {:data-dismiss "modal"
                                   :on-click #(do (rf/dispatch [:add-addendum false])
                                                  (rf/dispatch [:change-specs false]))} "Exit"]
       (if (not @(rf/subscribe [:add-addendum]))
         [:button.btn.btn-primary {:type "submit" :form "edit-procurement"} "Save Changes"])]]]]])

(def reduce-subscriber-emails
  (partial reduce (fn [prev next]
                    (if (string? prev)
                      (str prev "," (:email next))
                      (str (:email prev) "," (:email next))))))

(defn pns-subscriber-modal
  [[item subscribers]]
  [:div#pns-subscriber-modal.modal.fade {:role "dialog"
                                         :tabIndex "-1"
                                         :aria-labelledby "pns-subscriber-modal-label"
                                         :aria-hidden "true"}
   [:div.modal-dialog {:role "document"}
    [:div.modal-content
     [:div.modal-header
      [:h2#pns-subscriber-modal-label.modal-title (str (procurement-type item)  ": " (:title item))]
      [:button.close {:on-click #(rf/dispatch [:clear-subscriber-modal])
                      :data-dismiss "modal"
                      :aria-label "Close"}
       [:span {:aria-hidden "true"} "\u00d7"]]]
     [:div.modal-body
      [:p (str "There are " (count subscribers) " subscribers to this announcement.  Click below to email all subscribers or to download the list as a spreadsheet file.")]
      [tables/pns-subscribers subscribers]]
     [:div.modal-footer
      [:div.col-xs-2
       [:a {:href (str "mailto:"
                       (reduce-subscriber-emails subscribers))}
        [:button.btn.btn-success {:on-click #(rf/dispatch [:email-subscribers item subscribers])}
         [:i.fa.fa-envelope] " Mail All"]]]
      [:div.col-xs-4]
      [:div.col-xs-6
       [:button.btn.btn-secondary {:data-dismiss "modal"
                                   :on-click #(rf/dispatch [:clear-subscriber-modal])} " Exit"]
       [:a {:href (str "/webtools/download/subscribers/" (:id item))}
        [:button.btn.btn-primary
         [:i.fa.fa-download] " Download"]]]]]]])

(defn all-modals
  []
  [:div
   [jva-modal @(rf/subscribe [:jva-modal])]
   [procurement-modal @(rf/subscribe [:procurement-modal])]
   [pns-subscriber-modal @(rf/subscribe [:subscriber-modal])]
   [cert-modal @(rf/subscribe [:cert-modal])]])
