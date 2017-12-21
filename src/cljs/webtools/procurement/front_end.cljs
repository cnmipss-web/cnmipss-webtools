(ns webtools.procurement.front-end
  (:require [webtools.procurement.core :as p]
            [webtools.constants :as const]
            [webtools.util :as util]
            [webtools.util.dates :as util-dates]
            [webtools.spec.dates]
            [re-frame.core :as rf]
            [cljs-time.format :as f]))

(defn- -fix-types [{:keys [id type open_date close_date] :as pns}]
  (-> (clojure.walk/keywordize-keys pns)
      (assoc :id (p/make-uuid id))
      (assoc :type (keyword type))
      (assoc :open_date (util-dates/parse-date open_date))
      (assoc :close_date (util-dates/parse-date-at-time close_date))))

(extend-protocol p/create-procurement
  cljs.core/PersistentHashMap
  (convert-pns-from-map [pns]
    (p/map->PSAnnouncement (-fix-types pns)))
  cljs.core/PersistentArrayMap
  (convert-pns-from-map [pns]
    (p/map->PSAnnouncement (-fix-types pns))))

(extend-type string
  p/procurement-from-db
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
  p/procurement-from-db
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
  p/procurement-from-db
  (make-uuid [id] nil)
  (get-pns-from-db [id] nil))
