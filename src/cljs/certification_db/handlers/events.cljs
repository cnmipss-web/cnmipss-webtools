(ns certification-db.handlers.events
  (:require [re-frame.core :as rf]
            [ajax.core :as ajax]
            [certification-db.handlers.api :as ajax-handlers]
            [certification-db.util :as util]
            [certification-db.constants :as const]))

(defn set-active-role
  [role]
  (fn [event]
    (.preventDefault event)
    (rf/dispatch [:set-active-role role])))

(defn update-user
  [email]
  (fn [event]
    (.preventDefault event)
    (let [clean-email (apply str (re-seq #"[\w]" email))
          roles (atom "")
          admin (-> (str "#admin-" clean-email) util/jq (.is ":checked"))]
      (dorun (for [role const/role-list]
               (if-let [checked (as-> (re-seq #"\S" role) role-id
                                  (apply str role-id)
                                  (str "#" role-id "-" clean-email)
                                  (-> role-id util/jq (.is ":checked")))]
                 (swap! roles str "," role))))
      (swap! roles subs 1)
      (ajax/ajax-request {:uri "/webtools/api/update-user"
                          :method :post
                          :format (ajax/json-request-format)
                          :params {:email email :roles @roles :admin admin}
                          :response-format (util/full-response-format ajax/json-response-format)
                          :handler ajax-handlers/all-users}))))
