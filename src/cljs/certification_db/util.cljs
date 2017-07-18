(ns certification-db.util
  (:require [ajax.core :as ajax]))

(defn full-response-format [body-format]
  (-> (body-format)
      (update :read (fn [original-handler]
                      (fn [response-obj]
                        {:headers  (js->clj (.getResponseHeaders response-obj))
                         :body    (original-handler response-obj)
                         :status  (.getStatus response-obj)})))))

(defn getElementById [id]
  (.getElementById js/document id))

(def jq js/jQuery)
