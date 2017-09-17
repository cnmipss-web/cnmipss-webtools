(ns webtools.procurement.front-end
  (:require [webtools.procurement.core :as p]
            [webtools.constants :as const]
            [webtools.util :as util]
            [webtools.util.dates :as util-dates]
            [re-frame.core :as rf]
            [cljs-time.format :as f]))

(defn- -fix-types [{:keys [id type open_date close_date] :as pns}]
  (-> (clojure.walk/keywordize-keys pns)
      (assoc :id (p/make-uuid id))
      (assoc :type (keyword type))
      (assoc :open_date (util-dates/parse-date open_date))
      (assoc :close_date (util-dates/parse-date-at-time close_date))))

(extend-type webtools.procurement.core/PSAnnouncement
  p/process-procurement
  (proc-type [pnsa]
      (-> pnsa :type keyword))
  
  (for-json [{:keys [id open_date close_date] :as pns}]
    (-> (assoc pns :id (str id))
        (assoc :open_date (util-dates/print-date open_date))
        (assoc :close_date (util-dates/print-date-at-time close_date)))))

(extend-protocol p/create-procurement
  cljs.core/PersistentHashMap
  (pns-from-map [pns]
    (p/map->PSAnnouncement (-fix-types pns)))
  cljs.core/PersistentArrayMap
  (pns-from-map [pns]
    (p/map->PSAnnouncement (-fix-types pns))))

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
        (throw (js/Error. (str "Duplicate matches " matches)))))))

(extend-type cljs.core/UUID
  p/retrieve-procurement
  (make-uuid [id] id)
  (get-pns-from-db [id]
    (let [{:keys [rfps ifbs]} @(rf/subscribe [:procurement-list])
          matches  (apply conj
                          (filter #(= id (:id %)) rfps)
                          (filter #(= id (:id %)) ifbs))]
      (if (>= 1 (count matches))
        (first matches)
        (throw (js/Error. (str "Duplicate matches " matches)))))))

(extend-type nil
  p/retrieve-procurement
  (make-uuid [id] nil)
  (get-pns-from-db [id] nil))
