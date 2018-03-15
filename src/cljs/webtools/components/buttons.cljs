(ns webtools.components.buttons)

(defn download-button [{:keys [url title text]}]
  [:a.btn.btn-info.file-link {:title title
                              :href url
                              :role "button"}
   [:i.fa.fa-download
    " " text]])
