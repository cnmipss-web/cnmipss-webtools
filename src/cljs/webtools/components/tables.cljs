(ns webtools.components.tables
  (:require
   [cljs-time.core :as time]
   [cljs-time.format :as f]
   [clojure.string :as cstr]
   [re-frame.core :as rf]
   [webtools.components.forms :as forms]
   [webtools.components.tables.fns :as tfns]
   [webtools.components.tables.certification :as tcert]
   [webtools.components.tables.hro :as thro]
   [webtools.constants :as const]
   [webtools.handlers.events :as events]
   [webtools.procurement.core :as p]
   [webtools.spec.procurement]
   [webtools.spec.subscription]
   [webtools.timeout :refer [throttle]]

   [webtools.util.dates :as util-dates]))

(def fns-recent-results tfns/fns-recent-results)

(def error-table tcert/error-table)
(def existing-certifications tcert/existing-certifications)

(defn user-row [user]
  [:tr.record-table__row
   [:td.custom-col-5.text.text-left {:style {:padding-left "10px"}} (user :email)]
   [:td.custom-col-15.text.text-left
    [forms/edit-user-roles user]]])

(defn user-table [users]
  [:table.record-table
   [:caption.sr-only "Registered Users"]
   [:thead
    [:tr.record-table__row
     [:th.custom-col-5.text.text-center {:scope "col"} "Email"]
     [:th.custom-col-15.text.text-center {:scope "col"} "Roles"]]]
   [:tbody
    (for [user (sort-by :email users)]
      ^{:key (str "user-" (user :email))} [user-row user])]])



