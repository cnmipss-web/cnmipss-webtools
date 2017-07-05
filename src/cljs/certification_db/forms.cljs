(ns certification-db.forms)

(defn login-form
  []
  [:form.custom-form {:on-submit #(do (js/alert "Login!") (.preventDefault %))}
   [:div.form-group
    [:label {:for "login"} "Login With Your CNMI PSS Email"]
    [:button#login.btn.form-submit-btn.btn-primary.form-control {:type "submit"} "Login"]]])

(defn upload-form
  []
  [:form.custom-form {:on-submit #(do (js/alert "Upload!") (.preventDefault %))}
   [:div.form-group
    [:label {:for "upload"} "Upload CSV File"]
    [:input#upload.form-control {:type "file"}]]
   [:div.form-group
    [:button.btn.form-submit-btn.btn-primary.form-control {:type "submit"} "Upload"]]])

(defn backup-form
  []
  [:form.custom-form {:on-submit #(do (js/alert "Revert!") (.preventDefault %))}
   [:div.form-group
    [:label {:for "backup"} "Revert DB to Backup"]
    [:select#backup.form-control "Select Version"
     [:option "Option #1"]
     [:option "Option #2"]]]
   [:div.form-group
    [:button.btn.form-submit-btn.btn-primary.form-control {:type "submit"} "Revert DB"]]])
