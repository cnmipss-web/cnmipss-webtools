(ns certification-db.components.forms
  (:require [re-frame.core :as rf]))

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
