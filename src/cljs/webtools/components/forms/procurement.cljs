(ns webtools.components.forms.procurement
  (:require [re-frame.core :as rf]
            [webtools.components.buttons :as btn]
            [webtools.components.forms.generic :as gforms]
            [webtools.handlers.events :as events]
            [webtools.util.dates :as util-dates]))

(def ^:private jq js/jQuery)

(def ^:private procurement-fields
  {:number      "Number"
   :status      "Status"
   :open_date   "Opening Date"
   :close_date  "Closing Date"
   :description "Description"
   :title       "Title"})

(defn- update-pns-val
  [key]
  (fn []
    (->> (-> (str "#" (name key)) jq .val)
         (conj [:edit-procurement key])
         (rf/dispatch))))

(defn procurement-upload []
  (let [accept ".pdf"
        cls "col-xs-4"
        action "/webtools/upload/procurement-pdf"
        path (.-hash js/location)]
    [:form.upload-form {:action action :method "post" :enc-type "multipart/form-data"}
     [:div.form-inline.row
      [gforms/upload-group {:id "ann-file"
                            :label "Upload New RFP/IFB Announcement"
                            :accept accept
                            :class cls}] 
      [gforms/upload-group {:id "spec-file"
                            :label "Upload Specs for this Announcement"
                            :accept accept
                            :class cls}]
      [:div.form-group.col-xs-4
       [gforms/hidden-input {:name "path"
                             :value path}]
       [btn/submit-btn {:text "Upload"} ]]]]))

(defn edit-procurement [item] 
  [:form#edit-procurement.edit-modal {:on-submit (events/edit-procurement item)
                                      :key (random-uuid)} ;;HACK: key forces :default-values to update
   (for [[key val] (->> item
                        (filter (fn [[key val]] (not= key :file_link)))
                        (filter (fn [[key val]] (not= key :spec_link)))
                        (filter (fn [[key val]] (not= key :type)))
                        (sort-by (fn [[key val]] (case key
                                                   :number 0
                                                   :title 1
                                                   :open_date 2
                                                   :close_date 3
                                                   :description 4
                                                   5))))]
     (let [field-name (key procurement-fields)
           opts-map {:type "text"
                     :id (name key)
                     :name field-name
                     :on-blur (update-pns-val key)}]
       (case key
         :status [:div {:key (str key (.random js/Math))}]

         :id [:div {:key (str key (.random js/Math))}]

         :description
         [:div.form-group {:key (str key)}
          [:label.bold {:for field-name} field-name]
          [:textarea.form-control (-> opts-map
                                      (assoc :default-value val)
                                      (dissoc :type))]]
         :open_date
         [:div.form-group {:key (str key)}
          [:label.bold {:for field-name} field-name]
          [:input.form-control (assoc opts-map :default-value (util-dates/print-date val))]]
         
         :close_date
         [:div.form-group {:key (str key)}
          [:label.bold {:for field-name} field-name]
          [:input.form-control (assoc opts-map :default-value (util-dates/print-date-at-time val))]]
         
         [:div.form-group {:key (str key)}
          [:label.bold {:for field-name} field-name]
          [:input.form-control (assoc opts-map :default-value val)]])))])

(defn procurement-addendum
  [item]
  [:form#procurement-addendum.edit-modal
   {:action "/webtools/upload/procurement-addendum" :method "post" :enc-type "multipart/form-data"}
   [:div.form-group
    [:label {:for "file"} "Upload Addendum"]
    [:input#upload-jva.form-control {:type "file" :id "file" :name "file" :accept ".pdf" :multiple false}]]
   [:div.form-group.sr-only
    [:input.form-control {:aria-hidden true :type "text" :name "id" :value (:id item)}]
    [:input.form-control {:aria-hidden true :type "text" :name "number" :value (:number item)}]
    [:input.form-control {:aria-hidden true :type "text" :name "type" :value (:type item)}]]
   [:div.form-group 
    [:input {:style {:display "none"} :on-change nil :type "text" :name "path" :value ""}  ]
    [:button#upload-btn.btn.btn-primary.form-control {:type "submit"} "Upload"]]])
