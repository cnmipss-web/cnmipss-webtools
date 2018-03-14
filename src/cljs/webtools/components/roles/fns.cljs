(ns webtools.components.roles.fns
  (:require [clojure.walk :refer [keywordize-keys]]
            [re-frame.core :as rf]
            [webtools.components.forms :as forms]
            [webtools.util.dates :as util-dates]
            [webtools.components.buttons :refer [download-button]]))

(defn- -fns-recent-results []
  (let [fns-list @(rf/subscribe [:fns-nap])] 
    [:table
     [:thead
      [:tr
       [:th {:scope "column"} "Date Created"]
       [:th {:scope "column"} "Matched FNS and NAP registrations"]
       [:th {:scope "column"} "Original FNS Registration records"]
       [:th {:scope "column"} "Original NAP Registration records"]]]
     [:tbody
      (for [{:keys [date_created fns_file_link nap_file_link matched_file_link]} fns-list]
        [:tr {:key (random-uuid)}
         [:td date_created]
         [:td
          (download-button {:url matched_file_link
                            :title (str "Matched FNS and NAP Registrations from " date_created)})]
         [:td
          (download-button {:url fns_file_link
                            :title (str "FNS Registrations from " date_created)})]
         [:td
          (download-button {:url nap_file_link
                            :title (str "NAP Registrations from " date_created)})]])]]))

(defn fns-view []
  [:div.row
   [:div.col-xs-12
    [forms/fns-upload-form]]
   [:div.col-xs-12
    [-fns-recent-results]]])
