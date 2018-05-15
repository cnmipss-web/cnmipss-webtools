(ns webtools.components.tables.fns
  (:require [re-frame.core :as rf]
            [webtools.components.buttons :as btn]
            [webtools.handlers.events :as events]))

(defn fns-recent-results
  "Component to display recent FNS-NAP registration match results"
  []
  (let [fns-list @(rf/subscribe [:fns-nap])
        th-props {:scope "col"}] 
    [:table.record-table
     [:thead
      [:tr.record-table__row
       [:th th-props "Date Created"]
       [:th th-props "Matched FNS and NAP registrations"]
       [:th th-props "Original FNS Registration records"]
       [:th th-props "Original NAP Registration records"]
       [:th th-props]]]
     [:tbody
      (for [{:keys [date_created fns_file_link nap_file_link matched_file_link]
             :as fns-record} fns-list]
        [:tr.record-table__row
         {:key (random-uuid)}
         [:td date_created]
         [:td.record-table__td--center
          [btn/download-button {:href matched_file_link
                                :title (str "Matched FNS and NAP Registrations from " date_created)
                                :text "Matched Registration"}]]
         [:td.record-table__td--center
          [btn/download-button {:href fns_file_link
                                :title (str "FNS Registrations from " date_created)
                                :text "Original FNS"}]]
         [:td.record-table__td--center
          [btn/download-button {:href nap_file_link
                                :title (str "NAP Registrations from " date_created)
                                :text "Original NAP"}]]
         [:td.record-table__td--center
          [btn/delete-button {:title "Delete"
                              :on-click (events/delete-fns-nap fns-record)}]]])]]))
