(ns webtools.subscriptions.reframe-subs
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

(reg-sub :user-list
 (fn [db _]
   (:user-list db)))

(reg-sub :admin-access
 (fn [db _]
   (get-in db [:session :admin])))

(reg-sub :show-roles?
 (fn [db _]
   (:show-roles db)))

(reg-sub :search-text
 (fn [db _]
   (:search-text db)))

(reg-sub :cert-list
 (fn [db _]
   (:cert-list db)))

(reg-sub :cert-modal
 (fn [db _ ]
   (:cert-modal db)))

(reg-sub :jva-list
 (fn [db _]
   (:jva-list db)))

(reg-sub :jva-modal
 (fn [db _] 
   (:jva-modal db)))

(reg-sub :edit-jva
 (fn [db _]
   (:edit-jva db)))

(reg-sub :jva-searches
 (fn [db _]
   (:jva-searches db)))

(reg-sub :procurement-list
 (fn [db _]
   (:procurement-list db)))

(reg-sub :procurement-modal
 (fn [db _]
  (:procurement-modal db)))

(reg-sub :error-list
 (fn [db _]
   (:error-list db)))

(reg-sub :error
 (fn [db _]
   (:error db)))

(reg-sub :add-addendum
 (fn [db _]
   (:add-addendum db)))

(reg-sub :change-specs
 (fn [db _]
   (:change-specs db)))

(reg-sub :addenda
 (fn [db _]
   (:addenda db)))

(reg-sub :subscriber-modal
 (fn [db _]
   (:subscriber-modal db)))
