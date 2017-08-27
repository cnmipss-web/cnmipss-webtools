(ns webtools.handlers.reframe
  (:require [webtools.db :as db]
            [webtools.constants :as const]
            [webtools.util :as util]
            [webtools.handlers.api :as ajax-handlers]
            [webtools.procurement.core :as p]
            [re-frame.core :refer [dispatch reg-event-db]]
            [ajax.core :as ajax]))

(defn ajax-get
  [opts]
  (let [defaults {:method :get
                  :format (ajax/json-request-format)
                  :response-format (util/full-response-format ajax/json-response-format)}]
    (ajax/ajax-request (merge defaults opts))))

(reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

(reg-event-db
  :set-active-page
  (fn [db [_ page]]
    (assoc db :page page)))

(reg-event-db
 :set-session
 (fn [db [_ session]]
   (assoc db :session session)))

(reg-event-db
 :bad-login
 (fn [db [_ _]]
   (assoc db :bad-login true)))

(reg-event-db
 :verified-token
 (fn [db [_ email admin roles]]
   (as-> (assoc db :bad-login false) db
       (assoc db :session (util/keyed [email admin]))
       (if admin
         (assoc db :roles const/role-list)
         (assoc db :roles (clojure.string/split roles #","))))))

(reg-event-db
 :action-success
 (fn [db [_ _]]
   (assoc db :success true)))

(reg-event-db
 :action-failed
 (fn [db [_ _]]
   (assoc db :success false)))

(reg-event-db
 :set-roles
 (fn [db [_ roles]]
   (assoc db :roles roles)))

(reg-event-db
 :set-active-role
 (fn [db [_ role]]
   (case role
     "HRO" (ajax-get {:uri "/webtools/api/all-jvas"
                      :handler ajax-handlers/all-jvas})
     "Procurement" (ajax-get {:uri "/webtools/api/all-procurement"
                              :handler ajax-handlers/all-procurement})
     "Manage Users" (ajax-get {:uri "/webtools/api/all-users"
                               :handler ajax-handlers/all-users})
     nil)
   (assoc db :active-role role)))

(reg-event-db
 :store-users
 (fn [db [_ users]]
   (let [{:keys [email]} (db :session)
         current-user (first (filter #(= email (:email %)) users))]
     (-> db
         (assoc :user-list users)
         (assoc-in [:session :admin] (:admin current-user))
         (assoc :roles (clojure.string/split (:roles current-user) #","))))))

(reg-event-db
 :toggle-roles
 (fn [db [_ _]]
   (assoc db :show-roles (not (:show-roles db)))))

(reg-event-db
 :hide-roles
 (fn [db [_ _]]
   (assoc db :show-roles false)))

(reg-event-db
 :store-jvas
 (fn [db [_ jvas]]
   (assoc db :jva-list jvas)))

(reg-event-db
 :set-jva-searches
 (fn [db [_ searches]]
   (assoc db :jva-searches searches)))

(reg-event-db
 :set-jva-modal
 (fn [db [_ jva]]
   (assoc db :jva-modal jva)))

(reg-event-db
 :set-edit-jva
 (fn [db [_ setting]]
   (assoc db :edit-jva setting)))

(reg-event-db
 :toggle-jva-status
 (fn [db [_ jva]]
   (assoc db :jva-modal (assoc jva :status (not (:status jva))))))

(reg-event-db
 :edit-jva
 (fn [db [_ key val]]
   (assoc db :jva-modal (assoc (:jva-modal db) key val))))

(reg-event-db
 :store-procurement-list
 (fn [db [_ list]]
  (println )
  (assoc db :procurement-list {:rfps (->> (:pnsa list)
                                          (filter #(= "rfp" (:type %)))
                                          (map p/pns-from-map))
                               :ifbs (->> (:pnsa list)
                                          (filter #(= "ifb" (:type %)))
                                          (map p/pns-from-map))
                               :addenda (:addenda list)
                               :subscriptions (:subscriptions list)})))

(reg-event-db
 :set-procurement-modal
 (fn [db [_ item]]
   (assoc db :procurement-modal item)))

(reg-event-db
 :edit-procurement
 (fn [{:keys [procurement-modal] :as db} [_ key val]]
   (assoc db :procurement-modal (assoc procurement-modal key val))))

(reg-event-db :error-list
 (fn [db [_ errors]]
   (println (type errors))
   (assoc db :error-list (-> errors cemerick.url/url-decode
                             (clojure.string/replace "+" " ")
                             (clojure.string/split #"\n\n")))))

(reg-event-db :add-addendum
 (fn [db [_ setting]]
   (assoc db :add-addendum setting)))

(reg-event-db :set-subscriber-modal
 (fn [db [_ {:keys [id] :as item}]]
   (let [subscriptions (filter #(= id (:proc_id %)) (get-in db [:procurement-list :subscriptions]))]
     (assoc db :subscriber-modal [item subscriptions]))))

(reg-event-db :clear-subscriber-modal
 (fn [db _ ]
   (dissoc db :subscriber-modal)))

(reg-event-db :email-subscribers
 (fn [db [_ item subscribers]]
   ))
