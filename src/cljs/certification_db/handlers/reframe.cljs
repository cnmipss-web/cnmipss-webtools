(ns certification-db.handlers.reframe
  (:require [certification-db.db :as db]
            [re-frame.core :refer [dispatch reg-event-db]]
            [ajax.core :as ajax]))

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
