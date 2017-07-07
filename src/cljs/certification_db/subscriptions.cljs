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
