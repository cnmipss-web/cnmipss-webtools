(ns certification-db.subscriptions
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :page
  (fn [db _]
    (:page db)))

(reg-sub
 :bad-login
 (fn [db _]
   (:bad-login db)))

(reg-sub
 :session
 (fn [db _]
   (:session db)))

(reg-sub
 :success
 (fn [db _]
   (:success db)))

(reg-sub
 :roles
 (fn [db _]
   (:roles db)))

(reg-sub
 :active-role
 (fn [db _]
   (:active-role db)))

(reg-sub
 :user-list
 (fn [db _]
   (:user-list db)))
