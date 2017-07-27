(ns certification-db.handlers.reframe-subs
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
   (let [{:keys [roles active-role session]} db
         {:keys [admin]} session]
     (if admin
       active-role
       (if ((set roles) active-role)
         active-role
         "")))))

(reg-sub
 :user-list
 (fn [db _]
   (:user-list db)))

(reg-sub
 :admin-access
 (fn [db _]
   (get-in db [:session :admin])))

(reg-sub
 :show-roles?
 (fn [db _]
   (:show-roles db)))

(reg-sub
 :jva-list
 (fn [db _]
   (:jva-list db)))

(reg-sub
 :jva-modal
 (fn [db _]
   (:jva-modal db)))

(reg-sub
 :jva-searches
 (fn [db _]
   (:jva-searches db)))

(reg-sub
 :rfp-ifb-list
 (fn [db _]
   (:rfp-ifb-list db)))
