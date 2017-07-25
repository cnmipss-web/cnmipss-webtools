(ns certification-db.wordpress-api
  (:require [certification-db.constants :as const :refer [wp-host wp-media-route wp-token-route]]
            [certification-db.util :as util]
            [clj-http.client :as http]
            [certification-db.config :refer [env]]
            [certification-db.json :refer [json->edn]]))

(defn wp-auth-token
  []
  (let [{:keys [status body error]}
        (http/post (str wp-host wp-token-route)
                    {:headers {"Content-Type" "application/json"}
                     :body (clojure.data.json/write-str {:username (env :wp-un)
                                                         :password (env :wp-pw)})})]
    (if error
      (throw error)
      (str "Bearer " (get (clojure.data.json/read-str body) "token")))))

(defn reduce-media-opts
  [acc [key val]]
  (str acc (name key) "=" val "&"))

(defn create-media
  [filename file & {:keys [date date_gmt slug status title author comment_status
                    ping_status meta alt_text caption description post] :as opts}]
  (let [query-string (reduce reduce-media-opts "?" opts)]
    (try
      (let [{:keys [body status error]}
            (http/post (str wp-host wp-media-route query-string)
                       {:headers {"Content-Type" "mulitpart/form-data"
                                  "Content-Disposition" (str "attachment; filename=\"" filename "\"")
                                  "Authorization" (wp-auth-token)}
                        :multipart [{:name "file" :content (clojure.java.io/file file)}]})]
        (if error (throw error))
        (-> body json->edn :source_url))
      (catch Exception e
        (println e)))))

(defn delete-media
  [slug]
  (println "Slug: " slug)
  (let [{:keys [body status error]} (http/get (str wp-host wp-media-route "?slug=" slug))
        {:keys [id]} (-> body clojure.data.json/read-str clojure.walk/keywordize-keys first)]
    (println "Id: " id body (type body))
    (println (str wp-host wp-media-route "/" id))
    (http/delete (str wp-host wp-media-route "/" id)
                 {:headers {"Content-Type" "application/json"
                            "Authorization" (wp-auth-token)}
                  :body (-> {:force true} clojure.data.json/write-str)})))
