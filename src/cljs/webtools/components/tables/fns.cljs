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
       [:th {:scope "col"} "Date Created"]
       [:th {:scope "col"} "Matched FNS and NAP registrations"]
       [:th {:scope "col"} "Original FNS Registration records"]
       [:th {:scope "col"} "Original NAP Registration records"]]]
     [:tbody
      (for [{:keys [date_created fns_file_link nap_file_link matched_file_link]} fns-list]
        [:tr.record-table__row
         {:key (random-uuid)}
         [:td date_created]
         [:td.record-table__td--center
          [download-button {:href matched_file_link
                            :title (str "Matched FNS and NAP Registrations from " date_created)
                            :text "Matched Registration"}]]
         [:td.record-table__td--center
          [download-button {:href fns_file_link
                            :title (str "FNS Registrations from " date_created)
                            :text "Original FNS"}]]
         [:td.record-table__td--center
          [download-button {:href nap_file_link
                            :title (str "NAP Registrations from " date_created)
                            :text "Original NAP"}]]])]]))
