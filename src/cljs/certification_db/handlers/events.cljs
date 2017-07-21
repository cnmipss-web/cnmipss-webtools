(ns certification-db.handlers.events
  (:require [re-frame.core :as rf]
            [ajax.core :as ajax]
            [certification-db.handlers.api :as ajax-handlers]
            [certification-db.util :as util]
            [certification-db.constants :as const]))

(def jq js/jQuery)

(defn set-active-role
  [role]
  (fn [event]
    (.preventDefault event)
    (rf/dispatch [:set-active-role role])))

(defn get-all-roles
  "For each role in const/role-list, check if the checkbox at #${role}-${id-stem} is checked.
  Return a comma seperated list of all check marked roles."
  [id-stem]
  (->> (for [role const/role-list]
         (if-let [checked (as-> (apply str (re-seq #"\S" role)) role-id
                            (str "#" role-id "-" id-stem)
                            (-> role-id util/jq (.is ":checked")))]
           role))
       (filter some?)
       (clojure.string/join ",")))

(defn update-user
  "Returns an event-handler function specific to a single user's email.  Event handler will grab the new roles and admin status of this user from their form and send them to the server to update the user."
  [email]
  (fn 
    [event]
    (.preventDefault event) 
    (let [clean-email (apply str (re-seq #"[\w]" email))
          roles (get-all-roles clean-email)
          admin (-> (str "#admin-" clean-email) util/jq (.is ":checked"))]
      (ajax/ajax-request {:uri "/webtools/api/update-user"
                          :method :post
                          :format (ajax/json-request-format)
                          :params {:email email :roles roles :admin admin}
                          :response-format (util/full-response-format ajax/json-response-format)
                          :handler ajax-handlers/all-users}))))

(defn invite-user
  [event]
  (.preventDefault event)
  (let [email (.val (util/jq "#new-user-email"))
        roles (get-all-roles "new-user")
        admin (-> (str "#admin-new-user") util/jq (.is ":checked"))]
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
    (ajax/ajax-request {:uri "/webtools/api/delete-user"
                        :method :post
                        :format (ajax/json-request-format)
                        :params {:email email}
                        :response-format (util/full-response-format ajax/json-response-format)
                        :handler ajax-handlers/all-users})))

(defn edit-jva [jva]
  (fn [e]
    (.preventDefault e)
    (println "Setting: " jva)
    (ajax/ajax-request {:uri "/webtools/api/update-jva"
                        :method :post
                        :format (ajax/json-request-format)
                        :params jva
                        :response-format (util/full-response-format ajax/json-response-format)
                        :handler ajax-handlers/all-jvas})))

(defn delete-jva [jva]
  (fn [e]
    (.preventDefault e)
    (ajax/ajax-request {:uri "/webtools/api/delete-jva"
                        :method :post
                        :format (ajax/json-request-format)
                        :params jva
                        :response-format (util/full-response-format ajax/json-response-format)
                        :handler ajax-handlers/all-jvas})))


(defn search-jvas []
  (let [searches (-> "#search-jvas" jq .val (clojure.string/split #" "))]
    (rf/dispatch [:set-jva-searches searches])))
