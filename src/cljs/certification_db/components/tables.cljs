(ns certification-db.components.tables
  (:require [re-frame.core :as rf]
            [cljs-time.core :as time]
            [cljs-time.format :as format]
            [certification-db.components.forms :as forms]
            [certification-db.handlers.events :as events]))

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

(defn parse-date
  [date]
  (format/parse (format/formatter "MMMM dd, YYYY") date))

(defn force-close?
  [{:keys [status close_date]}]
  (or (not status)
      (and close_date
           (time/after? (time/now) (parse-date close_date)))))

(defn jva-row [jva]
  (let [{:keys [status close_date]} jva]
    [:tr.row.jva-list-row {:class (if (force-close? jva) "closed")}
     [:td.w-1 (jva :announce_no)]
     [:td.w-4 (jva :position)]
     [:td.w-1 (if (force-close? jva)
                [:em "Closed"]
                [:strong "Open"])]
     [:td.w-2 (jva :open_date)]
     [:td.w-2 (if close_date
                close_date
                "Until Filled")]
     [:td.w-5 (jva :salary)]
     [:td.w-2 (jva :location)]
     [:td.w-3 
      [:a {:href (jva :file_link)}
       [:button.btn.btn-info.jva-file-link {:title "Download"} [:i.fa.fa-download]]]
      [:a {:on-click (fn [] (rf/dispatch [:set-jva-modal jva]))}
       [:button.btn.btn-warning.jva-file-link {:title "Edit"
                                               :data-toggle "modal"
                                               :data-target "#jva-modal"
                                               :aria-controls "jva-modal"} [:i.fa.fa-pencil]]]
      [:a {:on-click (events/delete-jva jva)}
       [:button.btn.btn-danger.jva-file-link {:title "Delete"} [:i.fa.fa-trash]]]]]))

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
     [:th.w-3.text-center {:scope "col"} "Link"]]]
   [:tbody
    (for [jva (-> jvas filter-jvas sort-jvas)]
      ^{:key (str "jva-" (jva :announce_no))} [jva-row jva])]])

(def key->name {:rfps "Requests for Proposal" :ifbs "Invitations for Bid"})

(defn procurement-row [item]
  [:tr.row.jva-list-row {:class (if (force-close? item) "closed")}
   [:td.w-2 (or (:rfp_no item) (:ifb_no item))]
   [:td.w-2 (:open_date item)]
   [:td.w-2 (:close_date item)]
   [:td.w-4 (:title item)]
   [:td.w-8.text-left (-> item :description (subs 0 140) (str "..."))]
   [:td.w-2 (:file_link item)]])

(defn procurement-table [k m]
  [:div.procurement-table-box
   [:h2.procurement-title.text-center (key->name k)]
   [:table.procurement-list.col-xs-12
    [:caption.sr-only "List of current"]
    [:thead
     [:tr.row.jva-list-row
      [:th.w-2.text-center {:scope "col"} "Number"]
      [:th.w-2.text-center {:scope "col"} "Opening Date"]
      [:th.w-2.text-center {:scope "col"} "Closing Date"]
      [:th.w-4.text-center {:scope "col"} "Title"]
      [:th.w-8.text-center {:scope "col"} "Description"]
      [:th.w-2.text-center {:scope "col"} "Link"]]]
    [:tbody
     (for [item (-> m k)]
       ^{:key (str (name k) (:title item))} [procurement-row (assoc item :status true)])]]])

(defn rfp-ifb-list [rfp-ifb-list]
  [:div
   [procurement-table :rfps rfp-ifb-list]
   [procurement-table :ifbs rfp-ifb-list]])
