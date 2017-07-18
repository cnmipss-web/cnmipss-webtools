(ns certification-db.components.forms
  (:require [re-frame.core :as rf]
            [certification-db.constants :as const]
            [certification-db.handlers.events :as event-handlers]
            [certification-db.util :as util]))

(defn login-form []
  [:form#login-form {:action "/webtools/oauth/oauth-init" :method "get"}
   [:div.form-group
    [:label {:for "oauth-button"} "Login with your CNMI PSS Email"]
    (if-let [bad-login @(rf/subscribe [:bad-login])]
      [:p.bad-login-text "Sorry, that login attempt failed.  Please try again or contact the Webmaster."])
    [:button#oauth-button.btn.btn-primary.form-control {:type "submit"} "Login"]]])

(defn upload-form [path]
  [:form#upload-form {:action "/webtools/upload/certification-csv" :method "post" :enc-type "multipart/form-data"}
   [:div.form-group
    [:label {:for "upload-csv"} "Upload CSV File"]
    [:input#upload-csv.form-control {:type "file" :name "file"}]]
   [:div.form-group
    [:input {:style {:display "none"} :on-change nil :type "text" :name "path" :value path}  ]
    [:button#upload-btn.btn.btn-primary.form-control {:type "submit"} "Upload"]]])

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
 
