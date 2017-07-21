(ns certification-db.handlers.reframe
  (:require [certification-db.db :as db]
            [certification-db.constants :as const]
            [certification-db.util :as util]
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
 :set-jva-modal
 (fn [db [_ jva]]
   (assoc db :jva-modal jva)))

(reg-event-db
 :toggle-jva-status
 (fn [db [_ jva]]
   (assoc db :jva-modal (assoc jva :status (not (:status jva))))))

(reg-event-db
 :edit-jva
 (fn [db [_ key val]]
   (assoc db :jva-modal (assoc (:jva-modal db) key val))))
