(ns webtools.components.nav
  (:require [re-frame.core :as rf]
            [webtools.handlers.events :as e]
            [webtools.constants :refer [role-list]]))

(defn header []
  (let [{:keys [email]} @(rf/subscribe [:session])]
    [:header.navbar
     [:div.navbar-header
      [:a.navbar-brand {:href "#/app"} "CNMI PSS Webtools"]
      [:ul.navbar-nav
       (if (some? email)
         [:li.nav-item>button.nav-link {:on-click #(rf/dispatch [:toggle-roles])
                                        :aria-controls "nav-sidebar"
                                        :aria-expanded @(rf/subscribe [:show-roles?])}
          (if @(rf/subscribe [:show-roles?])
            "Hide Roles"
            "Show Roles")])
       [:li.nav-item>p.nav-link
        (str " " email)]]]
     (if (some? email)
       [:div.navbar-footer
        [:ul.navbar-nav.ml-auto
         [:li.nav-item>a.nav-link {:href "/webtools/logout"} "Logout"]]])]))

(defn side-bar-btn [role active]
  [:button.btn.sidebar-btn {:on-click (e/set-active-role role)
                            :class (if active "active" "")
                            :aria-controls "main-container"
                            :aria-label (str role " role")
                            :aria-expanded (= role @(rf/subscribe [:active-role]))} role])

(defn sidebar [roles]
   (let [active @(rf/subscribe [:active-role])
         admin @(rf/subscribe [:admin-access])]
     [:nav#nav-sidebar.sidebar.col-sm-3 {:style (if @(rf/subscribe [:show-roles?])
                                      {}
                                      {:left "-300px"})}
      (doall (for [role role-list]
               [:div.row>div.col-xs-12 {:key (str "role-" role)}
                (if (or (some #{role} roles)
                        admin)
                  (side-bar-btn role (= active role)))]))]))
