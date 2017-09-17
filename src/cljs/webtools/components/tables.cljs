(ns webtools.components.tables
  (:require [re-frame.core :as rf]
            [cljs.spec.alpha :as s]
            [cljs-time.core :as time]
            [cljs-time.format :as f]
            [webtools.components.forms :as forms]
            [webtools.handlers.events :as events]
            [webtools.procurement.core :as p]
            [webtools.constants :as const]
            [webtools.util :as util]
            [webtools.util.dates :as util-dates]
            [webtools.spec.procurement]
            [webtools.spec.subscription]))

(defn user-row [user]
  [:tr.row.user-list-row
   [:td.col-xs-3.text-left (user :email)]
   [:td.col-xs-9.text-left (forms/edit-user-roles user)]])

(defn user-table [users]
  [:table.user-list
   [:caption.sr-only "Registered Users"]
   [:thead
    [:tr.row.user-list-row
     [:th.col-xs-3.text-center {:scope "col"} "Email"]
     [:th.col-xs-9.text-center {:scope "col"} "Roles"]]]
   [:tbody
    (for [user (sort-by :email users)]
      ^{:key (str "user-" (user :email))} [user-row user])]])

(defn force-close?
  [{:keys [status close_date]}]
  (or (not status)
      (and close_date
           (time/after? (time/now) close_date))))

(defn jva-row [jva]
  (let [{:keys [status close_date]} jva]
    [:tr.row.jva-list-row {:class (if (force-close? jva) "closed")}
     [:td.custom-col-1 (jva :announce_no)]
     [:td.custom-col-4 (jva :position)]
     [:td.custom-col-1
      (if (force-close? jva)
        [:em "Closed"]
        [:strong "Open"])]
     [:td.custom-col-2 (jva :open_date)]
     [:td.custom-col-2
      (if close_date
        close_date
        "Until Filled")]
     [:td.custom-col-5 (jva :salary)]
     [:td.custom-col-2 (jva :location)]
     [:td.custom-col-3 
      [:a {:href (jva :file_link)}
       [:button.btn.btn-info.file-link {:title "Download"} [:i.fa.fa-download]]]
      [:a {:on-click (fn [] (rf/dispatch [:set-jva-modal jva]))}
       [:button.btn.btn-warning.file-link {:title "Edit"
                                               :data-toggle "modal"
                                               :data-target "#jva-modal"
                                               :aria-controls "jva-modal"} [:i.fa.fa-pencil]]]
      [:a {:on-click (events/delete-jva jva)}
       [:button.btn.btn-danger.file-link {:title "Delete"} [:i.fa.fa-trash]]]]]))

(defn filter-jvas
  [jvas]
  (filter
   (fn [jva] (let [{:keys [position location announce_no salary]} jva
                   searches @(rf/subscribe [:jva-searches])]
               (every? #(re-seq (re-pattern (str "(?i)" %))
                                (str position " " location " " announce_no " " salary)) searches)))
   jvas))

(defn sort-jvas [jvas]
  (concat (->> jvas (filter (comp not force-close?)) (sort-by :announce_no) reverse)
          (->> jvas (filter force-close?) (sort-by :announce_no) reverse)))

(defn jva-list [jvas]
  [:table.jva-list.col-xs-12
   [:caption.sr-only "List of current and past JVAs"]
   [:thead
    [:tr.row.jva-list-row
     [:th.w-1.text-center {:scope "col"} "Number"]
     [:th.w-4.text-center {:scope "col"} "Position/Title"]
     [:th.w-1.text-center {:scope "col"} "Status"]
     [:th.w-2.text-center {:scope "col"} "Opening Date"]
     [:th.w-2.text-center {:scope "col"} "Closing Date"]
     [:th.w-5.text-center {:scope "col"} "Salary"]
     [:th.w-2.text-center {:scope "col"} "Location"]
     [:th.w-3.text-center {:scope "col"} "Links"]]]
   [:tbody
    (for [jva (-> jvas filter-jvas sort-jvas)]
      ^{:key (str "jva-" (jva :announce_no))} [jva-row jva])]])

(def key->name {:rfps "Requests for Proposal" :ifbs "Invitations for Bid"})

(s/fdef procurement-row
        :args :webtools.spec.procurement/record
        :ret vector?)

(defn procurement-row [item]
  [:tr.row.jva-list-row {:class (if (force-close? item) "closed")}
   [:td.custom-col-1 (:number item)]
   [:td.custom-col-2 (util-dates/print-date (:open_date item))]
   [:td.custom-col-2 (util-dates/print-date-at-time (:close_date item))]
   [:td.custom-col-4 (:title item)]
   [:td.custom-col-8.text-left (-> item :description (subs 0 140) (str "..."))]
   [:td.custom-col-3
    [:a {:on-click #(rf/dispatch [:set-subscriber-modal item])}
     [:button.btn.btn-success.file-link {:title "Subscribers"
                                         :data-toggle "modal"
                                         :data-target "#pns-subscriber-modal"
                                         :aria-controls "pns-subscriber-modal"} [:i.fa.fa-envelope]]]
    [:a {:href (:file_link item)}
     [:button.btn.btn-info.file-link {:title "Download"} [:i.fa.fa-download]]]
    [:a {:on-click (fn [] (rf/dispatch [:set-procurement-modal item]))}
     [:button.btn.btn-warning.file-link {:title "Edit"
                                         :data-toggle "modal"
                                         :data-target "#procurement-modal"
                                         :aria-controls "procurement-modal"} [:i.fa.fa-pencil]]]
    [:a {:on-click (events/delete-procurement item)}
     [:button.btn.btn-danger.file-link {:title "Delete"} [:i.fa.fa-trash]]]]])

(defn procurement-table [k m] 
  [:div.procurement-table-box.col-xs-12
   [:h2.procurement-title.text-center (key->name k)]
   [:table.procurement-list
    [:caption.sr-only "List of current"]
    [:thead
     [:tr.row.jva-list-row
      [:th.custom-col-1.text-center {:scope "col"} "Number"]
      [:th.custom-col-2.text-center {:scope "col"} "Opening Date"]
      [:th.custom-col-2.text-center {:scope "col"} "Closing Date"]
      [:th.custom-col-4.text-center {:scope "col"} "Title"]
      [:th.custom-col-8.text-center {:scope "col"} "Description"]
      [:th.custom-col-3.text-center {:scope "col"} "Link"]]]
    [:tbody
     (for [item (-> m k)]
       ^{:key (str (name k) (:title item))} [procurement-row (assoc item :status true)])]]])

(defn rfp-ifb-list [procurement-list]
  [:div
   [procurement-table :rfps procurement-list]
   [procurement-table :ifbs procurement-list]])

(defn existing-addenda [pns-item]
  (let [{:keys [id]} pns-item
        addenda (->> @(rf/subscribe [:procurement-list])
                    :addenda
                    (filter #(= id (-> % :proc_id p/make-uuid))))]
    [:table#existing-addenda.text-center
     [:caption "Existing Addendums"]
     [:thead
      [:tr
       [:th.text-center "Number"]
       [:th.text-center "Link"]]]
     [:tbody
      (for [{:keys [addendum_number file_link]} addenda]
        (let [filename (last (re-find #"/([\w\.\-]+)$" file_link))]
          ^{:key (str (* addendum_number (.random js/Math)))}
          [:tr
           [:td (inc addendum_number)]
           [:td
            [:a {:href file_link :target "_blank"} filename]]]))]]))

(defn cert-row [row]
  (let [{:keys [last_name first_name cert_type cert_no start_date expiry_date mi]} row]
    [:tr.row.lookup-row.col-xs-12
     [:th.col-xs-2 {:scope "row"}
      [:p.text-center (second (re-find #"(.*?)(\-renewal\-\d+)?$" cert_no))]]
     [:td.col-xs-2
      [:p.text-center last_name]]
     [:td.col-xs-2
      [:p.text-center (str first_name " " mi)]]
     [:td.col-xs-2
      [:p.text-center cert_type]]
     [:td.col-xs-2
      [:p.text-center start_date]]
     [:td.col-xs-2
      [:p.text-center expiry_date]]]))


(defn error-table [error-list]
  [:table
   [:caption "Duplicate Certs: "]
   [:tbody
    (for [error error-list]
      (let [certs (->> (clojure.string/split error #"\n")
                       (mapv cljs.reader/read-string))]
        ^{:key (str (* 100000000 (.random js/Math)))} [:div.container-fluid
                                                       [cert-row (first certs)]
                                                       [cert-row (second certs)]]))]]) 
(s/fdef subscriber-row
        :args :webtools.spec.subscription/record
        :ret vector?)
 
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
