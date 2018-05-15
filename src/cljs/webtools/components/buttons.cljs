(ns webtools.components.buttons
  "Common, reusable button components")


(defn download-button [{:keys [text] :as opts-map}]
  (let [default-opts {:role "button"}
        opts (-> (merge default-opts opts-map)
                 (dissoc :text))]
    [:a.btn.btn-info.file-link opts
     [:i.fa.fa-download]
     " " text]))

(defn edit-button [{:keys [title text] :as opts-map}]
  (let [default-opts {:aria-label title}
        opts (-> (merge default-opts opts-map)
                 (dissoc :text)
                 (dissoc :title))]
    [:button.btn.btn-warning.file-link opts
     [:i.fa.fa-pencil]
     " " text]))

(defn delete-button [{:keys [title text] :as opts-map}]
  (let [default-opts {:aria-label title}
        opts (-> (merge default-opts opts-map)
                 (dissoc :text)
                 (dissoc :title))]
    [:button.btn.btn-danger.file-link opts
     [:i.fa.fa-trash]
     " " text]))

(defn submit-btn [{:keys [text]
                   :or {text "Submit"}
                   :as opts-map}]
  (let [default-opts {:type "submit"
                      :style {:width "100%"
                              :height "100%"}}
        opts (-> (merge default-opts opts-map)
                 (dissoc :text))]
    [:button#upload-btn.btn.btn-primary.form-control
     opts
     text]))

(defn info-button  [{:keys [title text] :as opts-map}]
  (let [default-opts {:role "button"}
        opts (-> (merge default-opts opts-map)
                 (dissoc :text)
                 (dissoc :title))]
    [:a.btn.btn-info.file-link opts
     [:i.fa.fa-info-circle]
     " " text]))

(defn msg-button  [{:keys [title text] :as opts-map}]
  (let [default-opts {:aria-label title}
        opts (-> (merge default-opts opts-map)
                 (dissoc :text)
                 (dissoc :title))]
    [:button.btn.btn-success.file-link opts
     [:i.fa.fa-envelope]
     " " text]))
