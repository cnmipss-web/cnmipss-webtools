(ns webtools.components.forms.manage-users
  "Forms for Administrators in \"Manage Users\" role, including subcomponents"
  (:require [clojure.string :as cstr]
            [webtools.constants :as const]
            [webtools.handlers.events :as events]))


(defn- -checkbox-group [{:keys [key id label default-checked value]}]
  [:div.user-role-checklist__group {:key key}
   [:label.user-role-checklist__label
    {:for id}
    label]
   [:input.user-role-checklist__checkbox
    {:type "checkbox"
     :id id
     :value value
     :default-checked default-checked}]])

(defn- -admin-checkbox
  [id-stem default-checked]
  [-checkbox-group {:id (str "admin-" id-stem)
                    :label "Admin"
                    :value "is admin"
                    :default-checked default-checked}])

(defn- -roles-checklist
  ([id-stem] (-roles-checklist id-stem false false))
  ([id-stem user-roles admin]
   [:div.user-role-checklist
    {:role "group"
     :id (str "checklist-" id-stem)}
    [:legend.sr-only "User Roles"]
    (for [role const/role-list]
      (let [role-id (apply str(re-seq #"\S" role))
            id (str role-id "-" id-stem)]
        [-checkbox-group {:key (random-uuid)
                          :id id
                          :label role
                          :value role
                          :default-checked (if user-roles (some #{role} user-roles))}]))
    [-admin-checkbox id-stem admin]]))

(defn edit-user-roles [{:keys [email roles admin]}]
  "Form for editing a user's allowed roles, saving, and deleting the user."
  (let [clean-email (->> email
                         (re-seq #"[\w]")
                         (apply str))
        user-roles (cstr/split roles #",")]
    [:form.user-role-form {:id clean-email :on-submit (events/update-user email)}
     [-roles-checklist clean-email user-roles admin]
     [:div.user-role-form__btn-box
      [:button.btn.btn-primary.user-role-form__save-btn
       {:type "submit" :title "Save"}
       [:i.fa.fa-save]
       [:p.sr-only "Save"]]
      [:button.btn.btn-danger.user-role-form__del-btn
       {:title "Delete" :on-click (events/delete-user email)}
       [:i.fa.fa-trash-o]
       [:p.sr-only "Delete"]]]]))

(defn invite-users []
  "Form for inviting new users and assigning their allowed roles"
  [:form.invite-user-form {:on-submit events/invite-user}
   [:div.form-group
    [:label
     {:for "new-user-email"}
     "Email"]
    [:input.form-control
     {:id "new-user-email" :type "text" :placeholder "Email"}]]
   [:div.form-group
    [:label {:for "checklist-new-user"} "Roles"]
    [:br]
    [-roles-checklist "new-user"]]
   [:button.btn.btn-primary.invite-user-form__submit-btn
    {:type "submit"}
    "Invite"]])

