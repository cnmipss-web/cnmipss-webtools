(ns certification-db.handlers
  (:require [certification-db.db :as db]
            [re-frame.core :refer [dispatch reg-event-db]]))

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
