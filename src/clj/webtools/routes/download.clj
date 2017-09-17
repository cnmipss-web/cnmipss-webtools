(ns webtools.routes.download
  (:require [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.spec.alpha :as s]
            [webtools.db.core :as db]
            [webtools.util :as util]
            [webtools.procurement.core :as p]))

(s/fdef subscriber-list-csv
        :args (s/cat :id :webtools.spec/uuid)
        :ret  :ring/response
        :fn   (s/or :success (s/and #(= (-> % :ret :status) 200)
                                    #(instance? java.io.File (-> % :ret :body)))
                    :error #(= (-> % :ret :status) 500)))

(defn subscriber-list-csv [id]
  (let [uuid (p/make-uuid id)
        subscriptions (->> (db/get-subscriptions {:proc_id uuid})
                           (mapv #(assoc % :telephone (util/format-tel-num (:telephone %)))))
        columns (->> subscriptions
                     first
                     keys
                     (mapv name)
                     (mapv #(util/capitalize-words (clojure.string/replace % #"_" " "))))
        rows (mapv vals subscriptions)
        data (->> (cons columns rows)
                  (mapv #(drop 2 %))
                  (mapv #(take 4 %)))]
    (with-open [temp (io/writer "temp.csv")]
      (csv/write-csv temp data))
    (-> (response/file-response "temp.csv")
        (response/header "Content-Type" "text/csv")
        (response/header "Content-Disposition"
                         (str "attachment;filename=" "subscribers.csv")))))

(defroutes download-routes
  (GET "/download/subscribers/:id" [id]
       (subscriber-list-csv id)))
