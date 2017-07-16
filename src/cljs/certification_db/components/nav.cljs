(ns certification-db.components.nav
  (:require [re-frame.core :as rf]))

(defn navbar []
  [:header.navbar.navbar-light.bg-faded
   [:div.navbar-header
    [:a.navbar-brand {:href "#/"} "CNMI PSS Webtools"]
    [:ul.navbar-nav.mr-auto>li.nav-item>p.nav-link
     (str " " (:email @(rf/subscribe [:session])))]]])

(def ^:private role-list ["Certification" "HRO" "Procurement"])

(defn user-sidebar [roles]
  [:nav.sidebar.bg-info
   (for [role role-list]
     (if (some #{role} roles)
       [:button.btn.sidebar-btn {:key (str "role-" role)} role]))])

(defn admin-sidebar []
  [:nav.sidebar.bg-info
   [:button.btn.sidebar-btn "Certification"]
   [:button.btn.sidebar-btn "HRO"]
   [:button.btn.sidebar-btn "Procurement"]
   [:button.btn.sidebar-btn "Manage Users"]
   [:button.btn.sidebar-btn "Manage DB"]])
