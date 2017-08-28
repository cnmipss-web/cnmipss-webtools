(ns webtools.procurement.front-end
  (:require [webtools.procurement.core :as p]
            [webtools.constants :as const]
            [webtools.util :as util]
            [re-frame.core :as rf]
            [cljs-time.format :as f]))

(extend-type webtools.procurement.core/PSAnnouncement
  p/process-procurement
  (for-json [pns]
    (-> (assoc pns :id (-> pns :id str))
        (assoc :open_date (-> pns :open_date util/print-date))
        (assoc :close_date (-> pns :close_date util/print-datetime)))))

(defn- parse-date
  [date]
  (if (string? date)
    (if (some? (re-find #"at" date))
      (f/parse (f/formatter "MMMM dd, YYYY h:mm A") (-> date
                                                        (clojure.string/replace #"at" "")
                                                        (clojure.string/replace #"\s+" " ")))
      (f/parse (f/formatter "MMMM dd, YYYY") date))
    date))

(extend-protocol p/create-procurement
  cljs.core/PersistentHashMap
  (pns-from-map [item]
    (let [pns (clojure.walk/keywordize-keys item)]
      (p/map->PSAnnouncement (-> pns
                                 (assoc :id (-> pns :id p/make-uuid))
                                 (assoc :type (-> pns :type keyword))
                                 (assoc :open_date (parse-date (:open_date pns)))
                                 (assoc :close_date (parse-date (:close_date pns)))))))
  cljs.core/PersistentArrayMap
  (pns-from-map [item]
    (let [pns (clojure.walk/keywordize-keys item)]
      (p/map->PSAnnouncement (-> pns
                                 (assoc :id (-> pns :id p/make-uuid))
                                 (assoc :type (-> pns :type keyword))
                                 (assoc :open_date (parse-date (:open_date pns)))
                                 (assoc :close_date (parse-date (:close_date pns))))))))

(extend-type string
  p/retrieve-procurement
  (make-uuid [id] (uuid id))
  (get-pns-from-db [id]
    (let [{:keys [rfps ifbs]} @(rf/subscribe [:procurement-list])
          matches  (apply conj
                          (filter #(= (p/make-uuid id) (:id %)) rfps)
                          (filter #(= (p/make-uuid id) (:id %)) ifbs))]
      (if (>= 1 (count matches))
        (first matches)
        (throw (js/Error. (str "Duplicate matches " (mapv (fn [x]
                                                            (into {} (filter #(= "test" (second %)) x)))
                                                          matches))))))))

(extend-type cljs.core/UUID
  p/retrieve-procurement
  (get-pns-from-db [id]
    (let [{:keys [rfps ifbs]} @(rf/subscribe [:procurement-list])
          matches  (apply conj
                          (filter #(= id (:id %)) rfps)
                          (filter #(= id (:id %)) ifbs))]
      (if (>= 1 (count matches))
        (first matches)
        (throw (js/Error. (str "Duplicate matches " (mapv (fn [x]
                                                            (into {} (filter #(= "test" (second %)) x)))
                                                          matches))))))))

(extend-type nil
  p/retrieve-procurement
  (make-uuid [id] nil)
  (get-pns-from-db [id] nil))
