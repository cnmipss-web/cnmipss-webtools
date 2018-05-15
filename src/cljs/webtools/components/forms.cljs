(ns webtools.components.forms
  (:require [re-frame.core :as rf]
            [webtools.constants :as const]
            [webtools.handlers.events :as event-handlers]
            [webtools.util :as util]
            [webtools.util.dates :as util-dates]
            [webtools.cookies :as cookies]
            [webtools.components.forms.generic :as gforms]
            [webtools.components.forms.manage-users :as m-users]
            [webtools.components.forms.fns :as fns]
            [webtools.components.forms.certification :as cforms]))

(def jq js/jQuery)

;; -----------------------------------------------
;; Forms for Administrators in "Manage Users" Role
;;------------------------------------------------

(def edit-user-roles m-users/edit-user-roles)
(def invite-users m-users/invite-users)

;; -----------------------------------------------
;; Forms for Certification Role
;; -----------------------------------------------

(def cert-search cforms/search-certification-records)
(def edit-cert cforms/edit-certification-record)

;; -----------------------------------------------
;; Forms for FNS Role
;; -----------------------------------------------

(def fns-upload-form fns/fns-upload-form)

;; -----------------------------------------------
;; Other Forms
;;------------------------------------------------

(def upload-form gforms/upload-form)


(defn login-form []
  [:form#login-form {:action "/webtools/oauth/oauth-init" :method "get"}
   [:div.form-group
    [:label {:for "oauth-button"} "Login with your CNMI PSS Email"]
    (when @(rf/subscribe [:bad-login])
      (if (cookies/get-cookie :timed-out)
        [:p.bad-login-text "Your session has timed out.  Please login again to continue."]
        [:p.bad-login-text "Sorry, that login attempt failed.  Please try again or contact the Webmaster."]))
    [:button#oauth-button.btn.btn-primary.form-control {:type "submit"} "Login"]]])

(defn revert-backup-form []
  [:form#revert-form
   [:div.form-group
    [:label {:for "revert-backup"} "Revert DB to version: "]
    [:select#revert-backup.form-control
     [:option "Option #1"]
     [:option "Option #2"]
     [:option "Option #3"]]]
   [:div.form-group
    [:label.sr-only {:for "submit-btn"} "Revert"]
    [:button#submit-btn.btn.btn-primary.form-control "Revert DB"]]])

(defn replace-jva [jva]
  [:form#replace-jva.edit-modal
   {:action "/webtools/upload/reannounce-jva" :method "post" :enc-type "multipart/form-data"}
   [:div.form-group
    [:label {:for "file"} "Reannounce JVA"]
    [:input#upload-jva.form-control {:type "file" :id "file" :name "file" :accept ".pdf" :multiple false}]]
   [:div.form-group 
    [:input {:style {:display "none"} :on-change nil :type "text" :name "path" :value ""}  ]
    [:button#upload-btn.btn.btn-primary.form-control {:type "submit"} "Upload"]]])

(def jva-fields
  {:announce_no "Announcement Number"
   :position "Position"
   :status "Status"
   :open_date "Opening Date"
   :close_date "Closing Date"
   :salary "Salary"
   :location "Location"})

(defn toggle-jva-status [jva]
  (fn []
    (rf/dispatch [:toggle-jva-status jva])))

(defn edit-jva [jva]
  [:form#edit-jva.edit-modal {:on-submit (event-handlers/edit-jva jva)}
   (for [[key val] (filter (fn [[key val]] (not= key :file_link)) jva)]
     (let [field-name (key jva-fields)]
       (if (= key :status)
         [:fieldset.form-group.form-inline {:key (str key val)}
          [:legend.bold field-name]
          [:div.form-check
           [:label.form-check-label 
            [:input.form-check-input {:type "radio"
                                      :name "status"
                                      :id "status-open"
                                      :checked val
                                      :value true
                                      :on-change (toggle-jva-status jva)}]
            " Open"]
           [:label.form-check-label 
            [:input.form-check-input {:type "radio"
                                      :name "status"
                                      :id "status-closed"
                                      :checked (not val)
                                      :value false
                                      :on-change (toggle-jva-status jva)}]
            " Closed"]]]
         [:div.form-group {:key (str key)}
          [:label.bold {:for field-name} field-name]
          [:input.form-control {:type "text"
                                :id (name key)
                                :name field-name
                                :value val
                                :on-change #(->> (-> (str "#" (name key)) jq .val)
                                                 (conj [:edit-jva key])
                                                 (rf/dispatch))}]])))])
