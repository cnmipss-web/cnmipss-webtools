(ns webtools.components.forms
  (:require [re-frame.core :as rf]
            [webtools.constants :as const]
            [webtools.handlers.events :as event-handlers]
            [webtools.util :as util]
            [webtools.util.dates :as util-dates]
            [webtools.cookies :as cookies]))

(def jq js/jQuery)

(defn login-form []
  [:form#login-form {:action "/webtools/oauth/oauth-init" :method "get"}
   [:div.form-group
    [:label {:for "oauth-button"} "Login with your CNMI PSS Email"]
    (when @(rf/subscribe [:bad-login])
      (if (cookies/get-cookie :timed-out)
        [:p.bad-login-text "Your session has timed out.  Please login again to continue."]
        [:p.bad-login-text "Sorry, that login attempt failed.  Please try again or contact the Webmaster."]))
    [:button#oauth-button.btn.btn-primary.form-control {:type "submit"} "Login"]]])


(defn upload-form
  "Multipurpose upload form"
  [{:keys [path action accept label multiple]}]
  [:form#upload-form {:action action :method "post" :enc-type "multipart/form-data"}
   [:div.form-inline.row
    [:div.form-group.col-xs-6
     [:label {:for "file"} label]
     [:br]
     [:input#upload-jva.form-control {:type "file" :id "file" :name "file" :accept accept :multiple multiple}]]
    [:div.form-group.col-xs-3
     [:input {:style {:display "none"} :on-change nil :type "text" :name "path" :value path}  ]
     [:button#upload-btn.btn.btn-primary.form-control {:type "submit"
                                                       :style {:width "100%"
                                                              :height "100%"}} "Upload"]]]])


(defn cert-search
  [placeholder]
  [:div#cert-search
   [:form {:role "search"}
    [:div.form-group
     [:label.sr-only {:for "search-certs"} placeholder]
     [:input.form-control {:type "search"
                           :id "search-certs"
                           :placeholder placeholder
                           :on-change event-handlers/search-certs
                           :ref "search-certified"}]]]])

(def cert-fields
  {:cert_no "Cert Number"
   :last_name "Last Name"
   :first_name "First Name"
   :mi "Middle Name"
   :cert_type "Cert Type"
   :start_date "Effective Date"
   :expiry_date "Expiration Date"})

(defn edit-cert [cert]
  [:form#edit-cert.edit-modal {:on-submit (event-handlers/edit-cert cert)}
   (for [[key val] cert]
     (let [field-name (key cert-fields)]
       [:div.form-group.form-inline {:key (str key)}
        [:label.bold {:for field-name} field-name]
        [:input.form-control {:type "text"
                              :id (name key)
                              :name field-name
                              :default-value val
                              :on-change #(->> (-> (str "#" (name key)) jq .val)
                                               (conj [:edit-cert key])
                                               (rf/dispatch))}]]))])


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

(defn admin-checkbox
  [id-stem default]
  [:div.form-group
   [:label.form-control "Admin"]
   [:input.form-control {:type "checkbox"
                         :id (str "admin-" id-stem)
                         :value "is admin"
                         :default-checked default}]])

(defn roles-checklist
  ([id-stem] (roles-checklist id-stem false false))
  ([id-stem user-roles admin]
   [:fieldset.form-group.d-flex.flex-row
    [:legend.sr-only "Roles"]
    (for [role const/role-list]
      (let [role-id (->> (re-seq #"\S" role)
                         (apply str))
            id (str role-id "-" id-stem)]
        [:div.form-group {:key (str "form-group-" role-id)}
         [:label.form-control {:for (str "role." role-id)} role]
         [:input.form-control {:type "checkbox"
                               :id id
                               :value role
                               :default-checked (if user-roles (some #{role} user-roles))}]]))
    [admin-checkbox id-stem admin]]))

(defn edit-user-roles [{:keys [email roles admin]}]
  (let [clean-email (->> email
                         (re-seq #"[\w]")
                         (apply str))
        user-roles (clojure.string/split roles #",")]
    [:form.edit-user-roles.form-inline {:id clean-email :on-submit (event-handlers/update-user email)}
     [roles-checklist clean-email user-roles admin]
     [:button.btn.btn-primary {:type "submit" :title "Save"} [:i.fa.fa-save] [:p.sr-only "Save"]]
     [:button.btn.btn-danger  {:title "Delete" :on-click (event-handlers/delete-user email)} [:i.fa.fa-trash-o] [:p.sr-only "Delete"]]]))

(defn invite-users []
  [:form.invite-users {:on-submit event-handlers/invite-user}
   [:div.form-group
    [:label "Email"]
    [:input.form-control {:id "new-user-email" :type "text" :placeholder "Email"}]]
   [:div.form-group.form-inline
    [:label "Roles"]
    [:br]
    [roles-checklist "new-user"]]
   [:button.btn.btn-primary {:type "submit"} "Invite"]])

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

(defn jva-search []
  [:div#jva-search
   [:form {:role "search"}
    [:div.form-group
     [:label.sr-only {:for "search-jvas"} "Search JVAS"]
     [:input.form-control {:type "search"
                           :id "search-jvas"
                           :placeholder "Search JVAS"
                           :on-change event-handlers/search-jvas
                           :ref "search-certified"}]]]])


(def procurement-fields
  {:number      "Number"
   :status      "Status"
   :open_date   "Opening Date"
   :close_date  "Closing Date"
   :description "Description"
   :title       "Title"})

(defn toggle-procurement-status
  [item]
  (fn [] ))

(defn update-pns-val
  [key]
  (fn []
    (->> (-> (str "#" (name key)) jq .val)
         (conj [:edit-procurement key])
         (rf/dispatch))))

(defn procurement-upload []
  (let [accept ".pdf"
        multiple false
        action "/webtools/upload/procurement-pdf"
        path (.-hash js/location)]
    [:form#upload-form {:action action :method "post" :enc-type "multipart/form-data"}
     [:div.form-inline.row
      [:div.form-group.col-xs-4
       [:label {:for "ann-file"} "Upload New RFP/IFB Announcement"]
       [:br]
       [:input#upload-jva.form-control {:type "file"
                                        :id "ann-file"
                                        :name "ann-file"
                                        :accept accept
                                        :multiple multiple}]]
      [:div.form-group.col-xs-4
       [:label {:for "spec-file"} "Upload Specs for this Announcement"]
       [:br]
       [:input#upload-jva.form-control {:type "file"
                                        :id "spec-file"
                                        :name "spec-file"
                                        :accept accept
                                        :multiple multiple}]]
      [:div.form-group.col-xs-4
       [:input {:style {:display "none"}
                :on-change nil
                :type "text"
                :name "path"
                :value path}]
       [:button#upload-btn.btn.btn-primary.form-control {:type "submit"
                                                         :style {:width "100%"
                                                                 :height "100%"}} "Upload"]]]]))

(defn edit-procurement [item]
  [:form#edit-procurement.edit-modal {:on-submit (event-handlers/edit-procurement item)
                                      :key (random-uuid)} ;;HACK: key forces :default-values to update
   (for [[key val] (->> item
                        (filter (fn [[key val]] (not= key :file_link)))
                        (filter (fn [[key val]] (not= key :spec_link)))
                        (filter (fn [[key val]] (not= key :type)))
                        (sort-by (fn [[key val]] (case key
                                                   :number 0
                                                   :title 1
                                                   :open_date 2
                                                   :close_date 3
                                                   :description 4
                                                   5))))]
     (let [field-name (key procurement-fields)
           opts-map {:type "text"
                     :id (name key)
                     :name field-name
                     :on-blur (update-pns-val key)}]
       (case key
         :status [:div {:key (str key (.random js/Math))}]

         :id [:div {:key (str key (.random js/Math))}]

         :description
         [:div.form-group {:key (str key)}
          [:label.bold {:for field-name} field-name]
          [:textarea.form-control (-> opts-map
                                      (assoc :default-value val)
                                      (dissoc :type))]]
         :open_date
         [:div.form-group {:key (str key)}
          [:label.bold {:for field-name} field-name]
          [:input.form-control (assoc opts-map :default-value (util-dates/print-date val))]]
         
         :close_date
         [:div.form-group {:key (str key)}
          [:label.bold {:for field-name} field-name]
          [:input.form-control (assoc opts-map :default-value (util-dates/print-date-at-time val))]]
         
         [:div.form-group {:key (str key)}
          [:label.bold {:for field-name} field-name]
          [:input.form-control (assoc opts-map :default-value val)]])))])

(defn procurement-addendum
  [item]
  [:form#procurement-addendum.edit-modal
   {:action "/webtools/upload/procurement-addendum" :method "post" :enc-type "multipart/form-data"}
   [:div.form-group
    [:label {:for "file"} "Upload Addendum"]
    [:input#upload-jva.form-control {:type "file" :id "file" :name "file" :accept ".pdf" :multiple false}]]
   [:div.form-group.sr-only
    [:input.form-control {:aria-hidden true :type "text" :name "id" :value (:id item)}]
    [:input.form-control {:aria-hidden true :type "text" :name "number" :value (:number item)}]
    [:input.form-control {:aria-hidden true :type "text" :name "type" :value (:type item)}]]
   [:div.form-group 
    [:input {:style {:display "none"} :on-change nil :type "text" :name "path" :value ""}  ]
    [:button#upload-btn.btn.btn-primary.form-control {:type "submit"} "Upload"]]])
