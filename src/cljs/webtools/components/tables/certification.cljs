(ns webtools.components.tables.certification
  (:require [cljs.reader :refer [read-string]]
            [clojure.string :as cstr]
            [re-frame.core :as rf]
            [webtools.components.buttons :as btn]
            [webtools.handlers.events :as events]
            [webtools.handlers.search :refer [search-by]]))

(defn- cert-headers []
  (let [th-props {:scope "col" :col-span "1"}]
    [:thead
     [:tr.record-table__row
      [:th.text.text--center th-props "Cert Number"]
      [:th.text.text--center th-props "Last Name"]
      [:th.text.text--center th-props "First Name"]
      [:th.text.text--center th-props "Cert Type"]
      [:th.text.text--center th-props "Effective Date"]
      [:th.text.text--center th-props "Expiration Date"]
      [:th.text.text--center th-props "Links"]]]))

(defn- collision-headers []
  (let [th-props {:scope "col"}]
    [:thead
     [:tr.record-table__row
      [:th.text.text--center th-props] 
      [:th.text.text--center.record-table__th--border {:scope "col" :col-span "4"} "Original"]
      [:th.text.text--center.record-table__th--border {:scope "col" :col-span "4"} "Uploaded"]]
     [:tr.record-table__row
      [:th.text.text--center th-props "Cert Number"]
      [:th.text.text--center.record-table__th--border th-props "Name"]
      [:th.text.text--center th-props "Effective Date"]
      [:th.text.text--center th-props "Expiration Date"]
      [:th.text.text--center th-props "Cert Type"]
      [:th.text.text--center.record-table__th--border th-props "Name"]
      [:th.text.text--center th-props "Effective Date"]
      [:th.text.text--center th-props "Expiration Date"]
      [:th.text.text--center th-props "Cert Type"]
      ]]))

(defn- cert-row [row]
  (let [{:keys [last_name
                first_name
                cert_type
                cert_no
                start_date
                expiry_date
                mi]}   row
        delete-cert-fn (events/delete-cert row)]
    [:tr.record-table__row
     [:th.text.text--center {:scope "row"}
      (second (re-find #"(.*?)(\-renewal\-\d+)?$" cert_no))]

     [:td.text.text--center
      last_name]

     [:td.text.text--center
      (str first_name " " mi)]

     [:td.text.text--center
      cert_type]

     [:td.text.text--center
      start_date]

     [:td.text.text--center
      expiry_date]

     [:td.text.text--center
      [btn/edit-button {:title         "Edit"
                        :data-toggle   "modal"
                        :data-target   "#cert-modal"
                        :aria-controls "cert-modal"
                        :on-click      (fn set-cert-modal []
                                         (rf/dispatch [:set-cert-modal row]))}]
      [btn/delete-button {:title    "Delete"
                          :on-click delete-cert-fn}]]]))

(defn- collision-row [{:keys [cert_no] :as collision}] 
  [:tr.record-table__row
   [:th.text.text--center {:scope "row"}
    (second (re-find #"(.*?)(\-renewal\-\d+)?$" cert_no))]

   [:td.text.text--center
    (:name1 collision)]

   [:td.text.text--center
    (:start_date1 collision)]

   [:td.text.text--center
    (:expiry_date1 collision)]

   [:td.text.text--center
    (:cert_type1 collision)]

   [:td.text.text--center
    (:name2 collision)]

   [:td.text.text--center
    (:start_date2 collision)]

   [:td.text.text--center
    (:expiry_date2 collision)]

   [:td.text.text--center
    (:cert_type2 collision)]   ])

(defn- flatten-errors [list next-error]
  (let [certs (mapv read-string (cstr/split next-error #"\n"))]
    (concat list certs)))

(defn- sort-certs
  [certs]
  (sort-by :cert_no certs))

(defn error-table []
  (when-let [collision-list @(rf/subscribe [:error-list])]
    [:table.record-table
     [:caption
      "Duplicate Certs: These records caused an error and were not saved to the database."
      ]
     [collision-headers]
     [:tbody
      (for [collision collision-list]
        ^{:key (random-uuid)} (collision-row collision))]]))

(defn existing-certifications []
  (let [table            (-> @(rf/subscribe [:cert-list])
                             js->clj
                             clojure.walk/keywordize-keys
                             (search-by :cert_no :first_name :last_name)
                             sort-certs)
        n-results        (count table)
        results-str      (str n-results (if (> n-results 1) " results." " result."))]
    [:div
     [:p.sr-only {:aria-live "polite"} results-str]
     
     [:table.record-table
      [:caption.sr-only
       "Certified Personnel Table"]
      [cert-headers]
      [:tbody
       (for [cert table]
         ^{:key (random-uuid)} [cert-row cert])]]]))
