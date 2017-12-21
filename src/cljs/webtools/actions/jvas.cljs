(ns webtools.actions.jvas
  (:require [re-frame.core :refer [reg-event-db]]))

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
   (assoc db :jva-modal (update jva :status not))))

(reg-event-db
 :edit-jva
 (fn [db [_ key val]]
   (update db :jva-modal #(assoc % key val))))

