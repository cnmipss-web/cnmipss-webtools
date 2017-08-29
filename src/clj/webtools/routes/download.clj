(ns webtools.routes.download
  (:require [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [clojure.data.json :as json]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [webtools.db.core :as db]
            [webtools.email :as email]
            [webtools.config :refer [env]]
            [webtools.util :refer :all]
            [webtools.json :refer :all]
            [webtools.layout :refer [error-page]]
            [webtools.constants :refer [max-cookie-age]  :as const]
            [webtools.wordpress-api :as wp]
            [clojure.tools.logging :as log]))


(defn capitalize-words 
  "Capitalize every word in a string"
  [s]
  (->> (clojure.string/split (str s) #"\b") 
       (map clojure.string/capitalize)
       clojure.string/join))

(defroutes download-routes
  (GET "/download/subscribers/:id" [id]
       (let [uuid (java.util.UUID/fromString id)
             subscriptions (->> (db/get-subscriptions {:proc_id uuid})
                                (mapv #(assoc % :telephone (format-tel-num (:telephone %)))))
             x (println subscriptions)
             columns (->> subscriptions
                          first
                          keys
                          (mapv name)
                          (mapv #(capitalize-words (clojure.string/replace % #"_" " "))))
             rows (mapv vals subscriptions)
             data (->> (cons columns rows)
                       (mapv #(drop 2 %))
                       (mapv #(take 4 %)))]
         (with-open [temp (io/writer "temp.csv")]
           (csv/write-csv temp data))
         (-> (response/file-response "temp.csv")
             (response/header "Content-Type" "text/csv")
             (response/header "Content-Disposition"
                              (str "attachment;filename=" "subscribers.csv"))))))
