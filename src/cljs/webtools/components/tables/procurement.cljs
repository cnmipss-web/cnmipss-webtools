(ns webtools.components.tables.procurement
  (:require [cljs-time.core :as time]
            [re-frame.core :as rf]
            [webtools.components.buttons :as btn]
            [webtools.handlers.events :as events]
            [webtools.models.procurement.core :as pcore]
            [webtools.util :as util]
            [webtools.util.dates :as util-dates]))

(def ^:private key->name {:rfps "Requests for Proposal" :ifbs "Invitations for Bid"})

(defn- force-close?
  [{:keys [status close_date]}]
  (try
    (or (not status)
        (and close_date
             (time/after? (time/now) (util-dates/parse-date close_date))))
    (catch js/Error e
      (println "Exception in webtools.components.tables.force-close?" e)
      false)))

(defn- procurement-row [{:keys [file_link spec_link open_date close_date title number description]
                         :as   item}]
  [:tr.record-table__row
   {:class (if (force-close? item) "record-table__row--closed")}
   [:td.custom-col-1.text.text--left
    number]

   [:td.custom-col-2.text.text--center.p-1
    (util-dates/print-date open_date)]

   [:td.custom-col-2.text.text--center.p-1
    (util-dates/print-date-at-time close_date)]

   [:td.custom-col-4.text.text--center.p-1
    title]

   [:td.custom-col-8.text.text--left.p-1
    (str (subs description 0 140) "...")]

   [:td.custom-col-3.text.text--left.pl-1.pr-1
    [btn/msg-button {:aria-controls "pns-subscriber-modal"
                     :data-target   "#pns-subscriber-modal"
                     :data-toggle   "modal"
                     :on-click      (fn set-subscriber-modal []
                                      (rf/dispatch [:set-subscriber-modal item]))
                     :title         "Subscribers"}]

    [btn/download-button {:href  file_link
                          :title "Download"}]

    [btn/info-button {:href  spec_link
                      :title "Specifications"}]

    [btn/edit-button {:aria-controls "procurement-modal"
                      :data-target   "#procurement-modal"
                      :data-toggle   "modal"
                      :on-click      (fn set-procurement-modal []
                                       (rf/dispatch [:set-procurement-modal item]))
                      :title         "Edit"}]

    [btn/delete-button {:title    "Delete"
                        :on-click (events/delete-procurement item)}]]])

(defn- procurement-table [k m]
  [:div.pns-table-box.col-xs-12
   [:h2.pns-table-box__title (key->name k)]
   [:table.record-table
    [:caption.sr-only "List of current requests for proposals and invitations for bids"]
    [:thead
     [:tr.record-table__row
      [:th.custom-col-1.text.text--center {:scope "col"} "Number"]
      [:th.custom-col-2.text.text--center {:scope "col"} "Opening Date"]
      [:th.custom-col-2.text.text--center {:scope "col"} "Closing Date"]
      [:th.custom-col-4.text.text--center {:scope "col"} "Title"]
      [:th.custom-col-8.text.text--center {:scope "col"} "Description"]
      [:th.custom-col-3.text.text--center {:scope "col"} "Links"]]]
    [:tbody
     (for [item (get m k)]
       ^{:key (random-uuid)} [procurement-row (assoc item :status true)])]]])

(defn- rfp-ifb-list []
  (let [procurement-list  @(rf/subscribe [:procurement-list])]
    [:div
     [procurement-table :rfps procurement-list]
     [procurement-table :ifbs procurement-list]]))

(defn existing-addenda [pns-item]
  (let [{:keys [id]} pns-item
        addenda      (->> @(rf/subscribe [:procurement-list])
                          :addenda
                          (filter #(= id (-> % :proc_id pcore/make-uuid))))]
    [:table#existing-addenda.text.text--center
     [:caption "Existing Addendums"]
     [:thead
      [:tr
       [:th.text.text--center "Number"]
       [:th.text.text--center "Link"]]]
     [:tbody
      (for [{:keys [addendum_number file_link]} addenda]
        (let [filename (last (re-find #"/([\w\.\-]+)$" file_link))]
          ^{:key (str (* addendum_number (.random js/Math)))}
          [:tr
           [:td (inc addendum_number)]
           [:td
            [:a {:href file_link :target "_blank"} filename]]]))]]))

(defn subscriber-row [subscriber]
  [:tr
   [:td (:company_name subscriber)]
   [:td (:contact_person subscriber)]
   [:td (-> subscriber :telephone util/format-tel-num)]
   [:td (:email subscriber)]])

(defn pns-subscribers [subscribers]
  [:table#pns-subscriber-table
   [:caption "List of Subscribers"]
   [:thead
    [:tr
     [:th "Company Name"]
     [:th "Contact Person"]
     [:th "Phone Number"]
     [:th "Email"]]]
   [:tbody
    (for [subscriber subscribers]
      ^{:key (:id subscriber)}
      [subscriber-row subscriber])]])
