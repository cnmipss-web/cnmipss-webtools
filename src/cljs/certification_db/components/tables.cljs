(ns certification-db.components.tables
  (:require [re-frame.core :as rf]
            [certification-db.components.forms :as forms]))

(defn user-row [user]
  [:tr.row.user-list-row
   [:td.col-xs-3.text-left.d-flex.align-items-center (get user "email")]
   [:td.col-xs-9.text-left (forms/edit-user-roles user)]])

(defn user-table [users]
  [:table.user-list
   [:caption.sr-only "Registered Users"]
   [:thead
    [:tr.row.user-list-row
     [:th.col-xs-3.text-center {:scope "col"} "Email"]
     [:th.col-xs-9.text-center {:scope "col"} "Roles"]]]
   [:tbody
    (for [user users]
      ^{:key (str "user-" (get user "email"))} [user-row user])]])