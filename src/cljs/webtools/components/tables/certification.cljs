(ns webtools.components.tables.certification
  (:require [cljs.reader :refer [read-string]]
            [clojure.string :as cstr]
            [re-frame.core :as rf]
            [webtools.components.buttons :refer [edit-button delete-button]]
            [webtools.handlers.events :as events]
            [webtools.handlers.search :refer [search-by]]))

(defn- cert-row [row]
  (let [{:keys [last_name
                first_name
                cert_type
                cert_no
                start_date
                expiry_date
                mi]} row
        delete-cert  (events/delete-cert row)]
    [:tr.record-table__row
     [:th.custom-col-3.text.text--center {:scope "row"}
      (second (re-find #"(.*?)(\-renewal\-\d+)?$" cert_no))]
     [:td.custom-col-3.text.text--center
      last_name]
     [:td.custom-col-3.text.text--center
      (str first_name " " mi)]
     [:td.custom-col-2.text.text--center
      cert_type]
     [:td.custom-col-3.text.text--center
      start_date]
     [:td.custom-col-3.text.text--center
      expiry_date]
     [:td.custom-col-3.text.text--center
      [edit-button {:title         "Edit"
                    :data-toggle   "modal"
                    :data-target   "#cert-modal"
                    :aria-controls "cert-modal"
                    :on-click      (fn [] (rf/dispatch [:set-cert-modal row]))}]
      [delete-button {:title    "Delete"
                      :on-click delete-cert} [:i.fa.fa-trash]]]]))

(defn- flatten-errors [list next-error]
  (let [certs (->> (cstr/split next-error #"\n")
                   (mapv read-string))]
    (concat list certs)))

(defn- sort-certs
  [certs]
  (sort-by :cert_no certs))

(defn error-table [error-list]
  (let [th-props {:scope "col"}]
    [:table.record-table
     [:caption "Duplicate Certs: These records caused an error and were not saved to the database."]
     [:thead
      [:tr.record-table__row
       [:th.custom-col-3.text.text--center th-props "Cert Number"]
       [:th.custom-col-3.text.text--center th-props "Last Name"]
       [:th.custom-col-3.text.text--center th-props "First Name"]
       [:th.custom-col-2.text.text--center th-props "Cert Type"]
       [:th.custom-col-3.text.text--center th-props "Effective Date"]
       [:th.custom-col-3.text.text--center th-props "Expiration Date"]]]
     [:tbody
      (for [cert (reduce flatten-errors [] error-list)]
        ^{:key (random-uuid)} (vec (drop-last (cert-row cert))))]]))

(defn cert-table [table]
  (let [th-props    {:scope "col"}
        n-results   (count table)
        results-str (str n-results (if (> n-results 1) " results." " result."))]
    [:div
     [:p.sr-only {:aria-live "polite"} results-str]
     [:table.record-table
      [:caption.sr-only (str "Certified Personnel Table ")]
      [:thead
       [:tr.record-table__row
        [:th.custom-col-3.text.text--center th-props "Cert Number"]
        [:th.custom-col-3.text.text--center th-props "Last Name"]
        [:th.custom-col-3.text.text--center th-props "First Name"]
        [:th.custom-col-2.text.text--center th-props "Cert Type"]
        [:th.custom-col-3.text.text--center th-props "Effective Date"]
        [:th.custom-col-3.text.text--center th-props "Expiration Date"]
        [:th.custom-col-3.text.text--center th-props "Tools"]]]
      [:tbody
       (if (< 0 (count @(rf/subscribe [:search-text])))
         (for [cert table]
           ^{:key (random-uuid)} [cert-row cert]))]]]))

(defn existing-certifications
  []
  (let [table @(rf/subscribe [:cert-list])]
    [cert-table (-> table
                    js->clj
                    clojure.walk/keywordize-keys
                    (search-by :cert_no :first_name :last_name)
                    sort-certs)]))
