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
  [:tr.row.jva-list-row {:class (if (not (jva :status)) "closed")}
   [:td.w-1 (jva :announce_no)]
   [:td.w-6 (jva :position)]
   [:td.w-1 (if (jva :status)
              [:strong "Open"]
              [:em "Closed"])]
   [:td.w-2 (jva :open_date)]
   [:td.w-2 (if-let [date (jva :close_date)]
              date
              "Until Filled")]
   [:td.w-5 (jva :salary)]
   [:td.w-2 (jva :location)]
   [:td.w-1 
    [:a {:href (jva :file_link)}
     [:button.btn.btn-info.jva-file-link {:title "Download"} [:i.fa.fa-download]]]
    [:a {:on-click (fn [] (rf/dispatch [:set-jva-modal jva]))}
     [:button.btn.btn-warning.jva-file-link {:title "Edit"
                                             :data-toggle "modal"
                                             :data-target "#jva-modal"
                                             :aria-controls "jva-modal"} [:i.fa.fa-pencil]]]]])

(defn jva-list [jvas]
  [:table.jva-list.col-xs-12
   [:caption.sr-only "List of current and past JVAs"]
   [:thead
    [:tr.row.jva-list-row
     [:th.w-1.text-center {:scope "col"} "Number"]
     [:th.w-6.text-center {:scope "col"} "Position/Title"]
     [:th.w-1.text-center {:scope "col"} "Status"]
     [:th.w-2.text-center {:scope "col"} "Opening Date"]
     [:th.w-2.text-center {:scope "col"} "Closing Date"]
     [:th.w-5.text-center {:scope "col"} "Salary"]
     [:th.w-2.text-center {:scope "col"} "Location"]
     [:th.w-1.text-center {:scope "col"} "Link"]]]
   [:tbody
    (for [jva (reverse (sort-by :announce_no jvas))]
      ^{:key (str "jva-" (jva :announce_no))} [jva-row jva])]])
