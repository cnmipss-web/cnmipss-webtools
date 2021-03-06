(ns webtools.handlers.events
  (:require [ajax.core :as ajax]
            [clojure.string :as cstr]
            [re-frame.core :as rf]
            [webtools.constants :as const]
            [webtools.cookies :refer [set-cookie]]
            [webtools.handlers.api :as ajax-handlers]
            [webtools.models.procurement.core :as p]
            [webtools.util :as util]
            [webtools.util.dates :as util-dates]))

(def ^:private jq js/jQuery)

(defn set-active-role
  [role]
  (fn activate-role [event]
    (.preventDefault event)
    (rf/dispatch [:set-active-role role])
    (set-cookie :role role)))
 
(defn get-all-roles
  "For each role in const/role-list, check if the checkbox at #${role}-${id-stem} 
  is checked.  Returns a comma seperated list of all check marked roles."
  [id-stem]
  (->> (for [role const/role-list]
         (if-let [checked (as-> (cstr/join (re-seq #"\S" role)) role-id
                            (str "#" role-id "-" id-stem)
                            (-> role-id jq (.is ":checked")))]
           role))
       (filter some?)
       (cstr/join ",")))

(defn update-user
  "Returns an event-handler function specific to a single user's email.  Event 
  handler will grab the new roles and admin status of this user from their 
  form and send them to the server to update the user."
  [email]
  (fn 
    [event]
    (.preventDefault event) 
    (let [clean-email (cstr/join (re-seq #"[\w]" email))
          roles (get-all-roles clean-email)
          admin (-> (str "#admin-" clean-email) jq (.is ":checked"))]
      (ajax/ajax-request {:uri "/webtools/api/update-user"
                          :method :post
                          :format (ajax/json-request-format)
                          :params {:email email :roles roles :admin admin}
                          :response-format (util/full-response-format ajax/json-response-format)
                          :handler ajax-handlers/all-users}))))

(defn invite-user
  [event]
  (.preventDefault event)
  (let [email (.val (jq "#new-user-email"))
        roles (get-all-roles "new-user")
        admin (-> (str "#admin-new-user") jq (.is ":checked"))]
    (ajax/ajax-request {:uri "/webtools/api/create-user"
                          :method :post
                          :format (ajax/json-request-format)
                          :params {:email email :roles roles :admin admin}
                          :response-format (util/full-response-format ajax/json-response-format)
                          :handler ajax-handlers/all-users})))

(defn delete-user
  [email]
  (fn [event]
    (.preventDefault event)
    (if (js/confirm "Are you sure you want to DELETE this user?")
      (ajax/ajax-request {:uri "/webtools/api/delete-user"
                          :method :post
                          :format (ajax/json-request-format)
                          :params {:email email}
                          :response-format (util/full-response-format ajax/json-response-format)
                          :handler ajax-handlers/all-users}))))

(defn edit-jva [jva]
  (fn [e]
    (.preventDefault e)
    (ajax/ajax-request {:uri "/webtools/api/update-jva"
                        :method :post
                        :format (ajax/json-request-format)
                        :params jva
                        :response-format (util/full-response-format ajax/json-response-format)
                        :handler ajax-handlers/all-jvas})))

(defn delete-jva [jva]
  (fn [e]
    (.preventDefault e)
    (if (js/confirm "Are you sure you want to DELETE this jva? DELETE is permanent.")
      (ajax/ajax-request {:uri "/webtools/api/delete-jva"
                          :method :post
                          :format (ajax/json-request-format)
                          :params jva
                          :response-format (util/full-response-format ajax/json-response-format)
                          :handler ajax-handlers/all-jvas}))))


(defn search-jvas []
  (let [searches (-> "#search-jvas" jq .val (cstr/split #" "))]
    (rf/dispatch [:set-search-text searches])))

(defn search-certs []
  (let [searches (-> "#search-certs" jq .val (cstr/split #" "))]
    (rf/dispatch [:set-search-text searches])))

(defn edit-procurement [item]
  (fn [e]
    (.preventDefault e)
    (ajax/ajax-request {:uri "/webtools/api/update-procurement"
                        :method :post
                        :format (ajax/json-request-format)
                        :params (let [{:keys [id open_date close_date] :as pns} item]
                                  (-> (assoc pns :id (str id))
                                      (assoc :open_date (util-dates/print-date open_date))
                                      (assoc :close_date (util-dates/print-date-at-time close_date))))
                        :response-format (util/full-response-format ajax/json-response-format)
                        :handler ajax-handlers/all-procurement})))

(defn delete-procurement [item]
  (let [type (-> item :type name)
        type-caps (cstr/upper-case type)
        {:keys [id close_date open_date]} item]
    (fn delete-procurement-item [e]
      (.preventDefault e)
      (if (js/confirm (str "WARNING: Deleting this " type-caps
                           " will delete ALL related data including pdf documents, addendums,"
                           " and records of contractor subscriptions.  The data will be permanently"
                           " deleted.  Make sure you have saved any data you wish to keep before deleting this "
                           type-caps ".  Are you sure you want to delete this " type-caps "?"))
        (ajax/ajax-request {:uri (str "/webtools/api/delete-" type)
                            :method :post
                            :format (ajax/json-request-format)
                            :params (-> (assoc item :id (str id))
                                        (assoc :close_date (util-dates/print-date-at-time close_date))
                                        (assoc :open_date (util-dates/print-date open_date)))
                            :response-format (util/full-response-format ajax/json-response-format)
                            :handler ajax-handlers/all-procurement})))))

(defn edit-cert [cert]
  (fn [e]
    (.preventDefault e)
    (ajax/ajax-request {:uri "/webtools/api/update-cert"
                        :method :post
                        :format (ajax/json-request-format)
                        :params cert
                        :response-format (util/full-response-format ajax/json-response-format)
                        :handler ajax-handlers/all-certs})))

(defn delete-cert [cert]
  (fn [e]
    (.preventDefault e)
    (if (js/confirm "Are you sure you want to DELETE this certification record?  DELETING a record is permanent.")
      (ajax/ajax-request {:uri "/webtools/api/delete-cert"
                          :method :post
                          :format (ajax/json-request-format)
                          :params cert
                          :response-format (util/full-response-format ajax/json-response-format)
                          :handler ajax-handlers/all-certs}))))

(defn delete-fns-nap [record]
  (fn delete-fns-record [e]
    (.preventDefault e)
    (if (js/confirm "Are you sure you want to DELETE these documents?  DELETEING is permanent.")
      (ajax/ajax-request {:uri "/webtools/api/delete-fns-nap"
                          :method :post
                          :format (ajax/json-request-format)
                          :params record
                          :response-format (util/full-response-format ajax/json-response-format)
                          :handler ajax-handlers/fns-nap}))))

(defn get-collision-list []
  (ajax/ajax-request {:uri "/webtools/api/recent-collision-list"
                      :method :get
                      :format (ajax/json-request-format)
                      :response-format (util/full-response-format ajax/json-response-format)
                      :handler ajax-handlers/cert-collision-list}))
