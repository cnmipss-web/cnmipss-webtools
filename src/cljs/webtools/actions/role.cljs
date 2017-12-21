(ns webtools.actions.role
    (:require
     [webtools.handlers.api :as ajax-handlers]
     [webtools.util :as util]
     [ajax.core :as ajax]
     [re-frame.core :refer [reg-event-db]]))

(defn- ajax-get
  [opts]
  (let [defaults {:method :get
                  :format (ajax/json-request-format)
                  :response-format (util/full-response-format ajax/json-response-format)}]
    (ajax/ajax-request (merge defaults opts))))

(reg-event-db
 :set-roles
 (fn [db [_ roles]]
   (assoc db :roles roles)))
    
(reg-event-db
 :set-active-role
 (fn [db [_ role]]
   (case role
     "Certification" (ajax-get {:uri "/webtools/api/all-certs"
                                :handler ajax-handlers/all-certs})

     "HRO" (ajax-get {:uri "/webtools/api/all-jvas"
                      :handler ajax-handlers/all-jvas})

     "Procurement" (ajax-get {:uri "/webtools/api/all-procurement"
                              :handler ajax-handlers/all-procurement})

     "Manage Users" (ajax-get {:uri "/webtools/api/all-users"
                               :handler ajax-handlers/all-users})

     nil)
   (assoc db :active-role role)))

(reg-event-db
 :toggle-roles
 (fn [db [_ _]]
   (update db :show-roles not)))

(reg-event-db
 :show-roles
 (fn [db [_ _]]
   (assoc db :show-roles true)))

(reg-event-db
 :hide-roles
 (fn [db [_ _]]
   (assoc db :show-roles false)))
