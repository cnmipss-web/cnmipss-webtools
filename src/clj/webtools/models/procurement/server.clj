(ns webtools.models.procurement.server
  (:require [clojure.string :as cstr]
            [clojure.tools.logging :as log]
            [webtools.constants :as const]
            [webtools.db :as db]
            [webtools.models.procurement.core :as p :refer :all]
            [webtools.util :as util]
            [webtools.util.dates :as util-dates]
            [webtools.wordpress-api :as wp])) 

(extend-type webtools.models.procurement.core.PSAnnouncement
  procurement-to-db
  (proc-type [pnsa]
    (keyword (:type pnsa)))
  
  (save-to-db! [pnsa]
    (db/create-pnsa! pnsa))

  (change-in-db! [pnsa]
    (db/update-pnsa! pnsa))
  
  (delete-from-db! [pnsa]
    (db/delete-pnsa! pnsa))

  communicate-procurement
  (uppercase-type [pns]
    (-> pns :type name cstr/upper-case))

  (title-string [{:keys [number title] :as pns}]
    (str (uppercase-type pns) "# " number " " title)))

(defn- -get-pnsa [map]
  (if-let [pnsa (db/get-single-pnsa map)]
    (map->PSAnnouncement (update pnsa :type keyword))))

(extend-protocol procurement-from-db
  java.lang.String
  (get-pns-from-db [id]
    (-get-pnsa {:id (make-uuid id)}))

  (get-subs-from-db [proc_id]
    (map p/convert-sub-from-map (db/get-subscriptions {:proc_id (p/make-uuid proc_id)})))

  (make-uuid [id] (java.util.UUID/fromString id))

  java.util.UUID
  (get-pns-from-db [uuid]
    (-get-pnsa {:id uuid}))

  (get-subs-from-db [proc_id]
    (map p/convert-sub-from-map (db/get-subscriptions {:proc_id proc_id})))
  
  (make-uuid [id] id)
  
  nil
  (get-pns-from-db [id] nil)
  (get-subs-from-db [proc_id] nil)
  (make-uuid [id] nil))


(defn- -convert-pns
  [{:keys [number type id] :as pns}]
  (try
    (if (every? some? [number type id])
      (-> (update pns :id make-uuid)
          (update :type keyword)
          (update :open_date util-dates/parse-date)
          (update :close_date util-dates/parse-date-at-time)
          map->PSAnnouncement))
    (catch Exception e
      (log/error e)
      (throw e))))

(defn- -convert-sub
  [{:keys [id proc_id] :as sub}]
  (try
    (if (every? some? [id proc_id])
      (-> (update sub :id make-uuid)
          (update :proc_id make-uuid)
          (update :telephone util/format-tel-num)
          map->Subscription))
    (catch Exception e
      (log/error e)
      (throw e))))

(extend-protocol create-procurement
  clojure.lang.PersistentArrayMap
  (convert-pns-from-map [pns] (-convert-pns pns))
  (convert-sub-from-map [sub] (-convert-sub sub))

  clojure.lang.PersistentHashMap
  (convert-pns-from-map [pns] (-convert-pns pns))
  (convert-sub-from-map [sub] (-convert-sub sub)))
