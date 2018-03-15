(ns webtools.components.tables.fns
  (:require [re-frame.core :as rf]
            [webtools.components.buttons :refer [download-button]]))

(defn fns-recent-results
  "Component to display recent FNS-NAP registration match results"
  []
  (let [fns-list @(rf/subscribe [:fns-nap])] 
    [:table.record-table
     [:thead
      [:tr.record-table__row
       [:th {:scope "column"} "Date Created"]
       [:th {:scope "column"} "Matched FNS and NAP registrations"]
       [:th {:scope "column"} "Original FNS Registration records"]
       [:th {:scope "column"} "Original NAP Registration records"]]]
     [:tbody
      (for [{:keys [date_created fns_file_link nap_file_link matched_file_link]} fns-list]
        [:tr.record-table__row
         {:key (random-uuid)}
         [:td date_created]
         [:td.record-table__td--center
          [download-button {:url matched_file_link
                            :title (str "Matched FNS and NAP Registrations from " date_created)
                            :text "Matched Registration"}]]
         [:td.record-table__td--center
          [download-button {:url fns_file_link
                            :title (str "FNS Registrations from " date_created)
                            :text "Original FNS"}]]
         [:td.record-table__td--center
          [download-button {:url nap_file_link
                            :title (str "NAP Registrations from " date_created)
                            :text "Original NAP"}]]])]]))
