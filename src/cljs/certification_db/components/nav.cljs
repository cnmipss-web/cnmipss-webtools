(ns certification-db.components.nav
  (:require [re-frame.core :as rf]
            [certification-db.event-controllers :as e]))

(defn active? [role]
  )

(defn navbar []
  [:header.navbar
   [:div.navbar-header
    [:a.navbar-brand {:href "#/"} "CNMI PSS Webtools"]
    [:ul.navbar-nav.mr-auto>li.nav-item>p.nav-link
     (str " " (:email @(rf/subscribe [:session])))]]])

(def ^:private role-list ["Certification" "HRO" "Procurement" "Manage Users" "Manage DB"])

(defn side-bar-btn [role active]
  [:button.btn.sidebar-btn {:on-click (e/set-active-role role)
                            :class (if active "active" "")} role])

(defn user-sidebar [roles]
   (let [active @(rf/subscribe [:active-role])]
     [:nav.sidebar.col-sm-3
     (for [role role-list]
       [:div.row>div.col-xs-12 {:key (str "role-" role)}
        (if (some #{role} roles)
          (side-bar-btn role (= active role)))])]))

(defn admin-sidebar []
  (user-sidebar role-list))
  

