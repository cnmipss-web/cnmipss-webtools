(ns webtools.re-frame.events.core
  (:require
   [cemerick.url :as curl]
   [clojure.string :as cstr]
   [re-frame.core :refer [dispatch reg-event-db]]
   [webtools.re-frame.events.errors]
   [webtools.re-frame.events.fns]
   [webtools.re-frame.events.jvas]
   [webtools.re-frame.events.login]
   [webtools.re-frame.events.role]
   [webtools.constants :as const]
   [webtools.db :as db]
   [webtools.handlers.api :as ajax-handlers]
   [webtools.models.procurement.core :as p]
   [webtools.util :as util]
   [webtools.util.dates :as util-dates]))

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
 :action-success
 (fn [db [_ _]]
   (assoc db :success true)))

(reg-event-db
 :action-failed
 (fn [db [_ _]]
   (assoc db :success false)))

(reg-event-db
 :set-search-text
 (fn [db [_ searches]]
   (assoc db :search-text searches)))

(reg-event-db
 :store-cert-list
 (fn [db [_ certs]]   
   (assoc db :cert-list certs)))

(reg-event-db
 :set-cert-modal
 (fn [db [_ cert]]
   (assoc db :cert-modal cert)))

(reg-event-db
 :edit-cert
 (fn [db [_ key val]]
   (assoc-in db [:cert-modal key] val)))

(reg-event-db
 :store-users
 (fn [db [_ users]]
   (let [{:keys [email]} (db :session)
         {:keys [admin roles]} (first (filter #(= email (:email %)) users))]
     (-> db
         (assoc :user-list users)
         (assoc-in [:session :admin] admin)
         (assoc :roles (cstr/split roles #","))))))

(reg-event-db
 :store-procurement-list
 (fn [db [_ list]]
  (assoc db :procurement-list {:rfps (->> (:pnsa list)
                                          (filter #(= "rfp" (:type %)))
                                          (map p/convert-pns-from-map))
                               :ifbs (->> (:pnsa list)
                                          (filter #(= "ifb" (:type %)))
                                          (map p/convert-pns-from-map))
                               :addenda (:addenda list)
                               :subscriptions (:subscriptions list)})))

(reg-event-db
 :set-procurement-modal
 (fn [db [_ item]]
   (assoc db :procurement-modal item)))

(reg-event-db
 :edit-procurement
 (fn [{:keys [procurement-modal] :as db} [_ key val]]
   (case key
     :open_date
     (try
       (let [date (util-dates/parse-date val)]
         (assoc db :procurement-modal (assoc procurement-modal key date)))
       (catch :default e
         (.error js/console e)
         db))
     :close_date
     (try
       (let [date (util-dates/parse-date-at-time val)]
         (assoc db :procurement-modal (assoc procurement-modal key date)))
       (catch :default e
         (.error js/console e)
         db))
     (assoc db :procurement-modal (assoc procurement-modal key val)))))

(reg-event-db
 :error-list
 (fn [db [_ errors]]
   (assoc db :error-list errors)))

(reg-event-db :add-addendum
 (fn [db [_ setting]]
   (assoc db :add-addendum setting)))

(reg-event-db :change-specs
 (fn [db [_ setting]]
   (assoc db :change-specs setting)))

(reg-event-db :set-subscriber-modal
 (fn [db [_ {:keys [id] :as item}]]
   (let [subscriptions (filter #(= id (-> % :proc_id p/make-uuid)) (get-in db [:procurement-list :subscriptions]))]
     (assoc db :subscriber-modal [item subscriptions]))))

(reg-event-db :clear-subscriber-modal
 (fn [db _ ]
   (dissoc db :subscriber-modal)))

(reg-event-db :email-subscribers
 (fn [db [_ item subscribers]]
   db))
