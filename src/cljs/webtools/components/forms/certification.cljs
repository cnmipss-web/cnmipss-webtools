(ns webtools.components.forms.certification
  (:require [re-frame.core :as rf]
            [webtools.components.forms.generic :as gforms]
            [webtools.handlers.events :as events]))

(def jq js/jQuery)

(defn search-certification-records
  [label]
  [:form.search-form {:role "search"}
   [gforms/search-group {:id "search-certs"
                         :label label
                         :on-change events/search-certs
                         :hide-label true}]])

(def ^:private cert-fields
  {:cert_no     "Cert Number"
   :last_name   "Last Name"
   :first_name  "First Name"
   :mi          "Middle Name"
   :cert_type   "Cert Type"
   :start_date  "Effective Date"
   :expiry_date "Expiration Date"})

(defn edit-certification-record
  [cert]
  [:form#edit-cert.edit-modal {:on-submit (events/edit-cert cert)}
   (for [[key val] cert] 
     (let [field-name (key cert-fields)
           id (str "input-" (name key))]
       [:div.form-group.form-inline {:key (random-uuid)}
        [:label.bold {:for id} field-name]
        [:input.form-control {:type "text"
                              :id id
                              :name field-name
                              :value val
                              :on-change (fn store-cert-edit []
                                           (rf/dispatch [:edit-cert
                                                         key (.val (jq (str "#" id)))]))}]]))])
