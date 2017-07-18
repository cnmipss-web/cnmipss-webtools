(ns certification-db.components.tables
  (:require [re-frame.core :as rf]
            [certification-db.components.forms :as forms]))

(defn user-row [user]
  [:tr.row.user-list-row
   [:td.col-xs-3.text-left (user :email)]
   [:td.col-xs-9.text-left (forms/edit-user-roles user)]])

(defn user-table [users]
  [:table.user-list
   [:caption.sr-only "Registered Users"]
   [:thead
    [:tr.row.user-list-row
     [:th.col-xs-3.text-center {:scope "col"} "Email"]
     [:th.col-xs-9.text-center {:scope "col"} "Roles"]]]
   [:tbody
    (for [user (sort-by :email users)]
      ^{:key (str "user-" (user :email))} [user-row user])]])

(defn jva-list []
  [:table.jva-list
   [:caption.sr-only "List of current and past JVAs"]
   [:thead
    [:tr.row.jva-list-row
     [:th.col-xs-2.text-center {:scope "col"} "Announcement No"]
     [:th.col-xs-3.text-center {:scope "col"} "Position/Title"]
     [:th.col-xs-1.text-center {:scope "col"} "Status"]
     [:th.col-xs-1.text-center {:scope "col"} "Opening Date"]
     [:th.col-xs-1.text-center {:scope "col"} "Closing Date"]
     [:th.col-xs-1.text-center {:scope "col"} "Salary"]
     [:th.col-xs-2.text-center {:scope "col"} "Location"]
     [:th.col-xs-1.text-center {:scope "col"} "Full JVA Posting"]]]])
