(ns webtools.components.buttons
  "Common, reusable button components"
  )

(defn download-button [{:keys [text] :as opts-map}]
  (let [default-opts {:role "button"}]
    [:a.btn.btn-info.file-link (merge default-opts opts-map)
     [:i.fa.fa-download]
     " " text]))

(defn edit-button [{:keys [title text] :as opts-map}]
  (let [default-opts {:aria-label title}]
    [:button.btn.btn-warning.file-link (merge default-opts opts-map)
     [:i.fa.fa-pencil]
     " " text]))

(defn delete-button [{:keys [title text] :as opts-map}]
  (let [default-opts {:aria-label title}]
    [:button.btn.btn-danger.file-link (merge default-opts opts-map)
     [:i.fa.fa-trash]
     " " text]))

(defn submit-btn [{:keys [text]
                   :or {text "Submit"}
                   :as opts-map}]
  (let [default-opts {:type "submit"
                      :style {:width "100%"
                              :height "100%"}}]
    [:button#upload-btn.btn.btn-primary.form-control
     (merge default-opts opts-map)
     text]))

(defn info-button  [{:keys [title text] :as opts-map}]
  (let [default-opts {:role "button"}]
    [:a.btn.btn-info.file-link (merge default-opts opts-map)
     [:i.fa.fa-info-circle]
     " " text]))

(defn msg-button  [{:keys [title text] :as opts-map}]
  (let [default-opts {:aria-label title}]
    [:button.btn.btn-success.file-link (merge default-opts opts-map)
     [:i.fa.fa-envelope]
     " " text]))
