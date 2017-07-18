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

(defn edit-user-roles [{:keys [email roles admin]}]
  (let [clean-email (->> email
                         (re-seq #"[\w]")
                         (apply str))
        user-roles (clojure.string/split roles #",")]
    [:form.edit-user-roles.form-inline {:id clean-email :on-submit (event-handlers/update-user email)}
     [:fieldset.form-group
      [:legend.sr-only "Roles"]
      (for [role const/role-list]
        (let [role-id (->> (re-seq #"\S" role)
                           (apply str))
              id (str role-id "-" clean-email)]
          [:div.form-group {:key (str "form-group-" role-id)}
           [:label.form-control {:for (keyword (str "role." role-id))} role]
           [:input.form-control {:type "checkbox"
                                 :id id
                                 :value role
                                 :default-checked (some #{role} user-roles)}]]))]
     [:fieldset.form-group
      [:label.form-control "Admin"]
      [:input.form-control {:type "checkbox"
                            :id (str "admin-" clean-email)
                            :value "is admin"
                            :default-checked admin}]]
     [:button.btn.btn-primary {:type "submit"} "Save"]]))

(defn invite-users []
  [:div])
 
