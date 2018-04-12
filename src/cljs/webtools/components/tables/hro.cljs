(ns webtools.components.tables.hro
  (:require
   [cljs-time.core :as time]
   [re-frame.core :as rf]
   [webtools.components.buttons :as btn]
   [webtools.handlers.events :as events]
   [webtools.handlers.search :refer [search-by]]
   [webtools.util.dates :as util-dates]
   ))

(defn- filter-jvas
  [jvas]
  (search-by jvas :position :location :announce_no :salary))

(defn- force-close?
  [{:keys [status close_date]}]
  (try
    (or (not status)
        (and close_date
             (time/after? (time/now) (util-dates/parse-date close_date))))
    (catch js/Error e
      (println "Exception in webtools.components.tables.force-close?" e)
      false)))

(defn sort-jvas [jvas]
  (concat (->> jvas (filter (comp not force-close?)) (sort-by :announce_no) reverse)
          (->> jvas (filter force-close?) (sort-by :announce_no) reverse)))

(defn jva-row [jva]
  (let [{:keys [announce_no
                position
                salary
                location
                status
                open_date
                close_date]} jva]
    [:tr.record-table__row
     {:class (if (force-close? jva) "record-table__row--closed")}

     [:td.custom-col-1.text
      announce_no]

     [:th.custom-col-4.text {:scope "row"}
      position]

     [:td.custom-col-1.text.text--center
      [:strong (if (force-close? jva)
                 "Closed"
                 "Open")]]

     [:td.custom-col-2.text.text--center
      open_date]

     [:td.custom-col-2.text.text--center 
      (if (some? close_date)
        close_date
        "Until Filled")]

     [:td.custom-col-5.text
      salary]

     [:td.custom-col-2.text
      location]

     [:td.custom-col-3
      [btn/download-button {:title "Download"
                            :href  (jva :file_link)}]
      [btn/edit-button {:title         "Edit"
                        :data-toggle   "modal"
                        :data-target   "#jva-modal"
                        :aria-controls "jva-modal"
                        :on-click      (fn [] (rf/dispatch [:set-jva-modal jva]))}]
      [btn/delete-button {:title    "Delete"
                          :on-click (events/delete-jva jva)}]]]))

(defn jva-list []
  (let [jvas @(rf/subscribe [:jva-list])]
    [:table.record-table
     [:caption.sr-only "List of current and past JVAs"]
     [:thead
      [:tr.record-table__row
       [:th.custom-col-1.text.text--center {:scope "col"} "Number"]
       [:th.custom-col-4.text.text--center {:scope "col"} "Position/Title"]
       [:th.custom-col-1.text.text--center {:scope "col"} "Status"]
       [:th.custom-col-2.text.text--center {:scope "col"} "Opening Date"]
       [:th.custom-col-2.text.text--center {:scope "col"} "Closing Date"]
       [:th.custom-col-5.text.text--center {:scope "col"} "Salary"]
       [:th.custom-col-2.text.text--center {:scope "col"} "Location"]
       [:th.custom-col-3.text.text--center {:scope "col"} "Links"]]]
     [:tbody
      (for [jva (-> jvas filter-jvas sort-jvas)]
        ^{:key (str "jva-" (jva :announce_no))} [jva-row jva])]]))
