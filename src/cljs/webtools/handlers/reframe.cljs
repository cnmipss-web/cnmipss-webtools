(ns webtools.handlers.reframe
  (:require [webtools.db :as db]
            [webtools.constants :as const]
            [webtools.util :as util]
            [webtools.util.dates :as util-dates]
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
   (let [roles-list (clojure.string/split roles #",")]
     (when (and
            (not admin)
            (= 1 (count roles-list)))
       (dispatch [:set-active-role (first roles-list)])
       (dispatch [:hide-roles]))
     (as-> (assoc db :bad-login false) db
       (assoc db :session (util/keyed [email admin]))
       (if admin
         (assoc db :roles const/role-list)
         (assoc db :roles roles-list))))))

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
   (assoc db :cert-modal (assoc (:cert-modal db) key val))))

(reg-event-db
 :store-users
 (fn [db [_ users]]
   (let [{:keys [email]} (db :session)
         {:keys [admin roles]} (first (filter #(= email (:email %)) users))]
     (-> db
         (assoc :user-list users)
         (assoc-in [:session :admin] admin)
         (assoc :roles (clojure.string/split roles #","))))))

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
   (let [subscriptions (filter #(= id (-> % :proc_id p/make-uuid)) (get-in db [:procurement-list :subscriptions]))]
     (assoc db :subscriber-modal [item subscriptions]))))

(reg-event-db :clear-subscriber-modal
 (fn [db _ ]
   (dissoc db :subscriber-modal)))

(reg-event-db :email-subscribers
 (fn [db [_ item subscribers]]
   ))
