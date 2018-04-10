(ns webtools.wordpress-api
  (:require [clj-http.client :as http]
            [clojure.tools.logging :as log]
            [webtools.config :refer [env]]
            [webtools.constants :refer [wp-media-route wp-token-route]]
            [webtools.json :as json]))



(defn wp-auth-token
  []
  (let [{:keys [wp-host]} env
        {:keys [status body error] :as response}
        (http/post (str wp-host wp-token-route)
                   {:headers {"Content-Type" "application/json"}
                    :body (json/edn->json {:username (env :wp-un)
                                           :password (env :wp-pw)})})]
    (if error
      (throw error)
      (str "Bearer " (get (json/json->edn body) "token")))))

(defn reduce-media-opts
  [acc [key val]]
  (str acc (name key) "=" val "&"))

(defn create-media
  [filename file & {:keys [date date_gmt slug status title author comment_status
                           ping_status meta alt_text caption description post] :as opts}]
  (let [query-string (reduce reduce-media-opts "?" opts)]
    (try
      (let [{:keys [wp-host]} env
            {:keys [body status error headers] :as response}
            (http/post (str wp-host wp-media-route query-string)
                       {:headers {"Content-Type" "multipart/form-data"
                                  "Content-Disposition" (str "attachment; filename=\"" filename "\"")
                                  "Authorization" (wp-auth-token)}
                        :multipart [{:name "file" :content (clojure.java.io/file file)}]})
            file-info (:body (http/get (get headers "Location")
                                       {:headers {"Authorization" (wp-auth-token)}}))]
        (if error (throw error))
        (-> file-info json/json->edn :source_url))
      (catch Exception e
        (log/error e)))))

(defn delete-media
  [slug]
  (let [{:keys [wp-host]} env
        media-url (str wp-host wp-media-route "?slug=" slug)
        {:keys [body status error]} (http/get media-url)
        {:keys [id]} (-> body json/json->edn clojure.walk/keywordize-keys first)]
    (try
      (http/delete (str wp-host wp-media-route "/" id)
                   {:headers {"Content-Type" "application/json"
                              "Authorization" (wp-auth-token)}
                    :body (json/edn->json {:force true})
                    :error-handler (fn [e] (log/error e))})
      (catch Exception e
        (log/error e)))))
