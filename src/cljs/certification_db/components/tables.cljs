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

(defn jva-row [jva]
  [:tr.row.jva-list-row
   [:td.col-xs-2 (jva :announce-no)]
   [:td.col-xs-3 (jva :position)]
   [:td.col-xs-1 (jva :status)]
   [:td.col-xs-1 (jva :open-date)]
   [:td.col-xs-1 (jva :close-date)]
   [:td.col-xs-1 (jva :salary)]
   [:td.col-xs-2 (jva :location)]
   [:td.col-xs-1 (jva :file-link)]])

(defn jva-list [jvas]
  [:table.jva-list.col-xs-12
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
     [:th.col-xs-1.text-center {:scope "col"} "Full JVA Posting"]]]
   [:tbody
    (for [jva (sort-by :close-date jvas)]
      ^{:key (str "jva-" (jva :announce-no))} [jva-row jva])]])
