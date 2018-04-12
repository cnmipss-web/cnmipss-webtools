(ns webtools.models.procurement.server
  (:require [cemerick.url :as curl]
            [clojure.string :as cstr]
            [clojure.tools.logging :as log]
            [webtools.constants :as const]
            [webtools.db.core :as db]
            [webtools.models.procurement.core :as p :refer :all]
            [webtools.util :as util]
            [webtools.util.dates :as util-dates]
            [webtools.wordpress-api :as wp])
  (:import (org.apache.pdfbox.pdmodel PDDocument)
           (org.apache.pdfbox.text PDFTextStripper))) 

(extend-type webtools.models.procurement.core.PSAnnouncement
  procurement-to-db
  (proc-type [pnsa]
    (keyword (:type pnsa)))
  
  (save-to-db [pnsa]
    (db/create-pnsa! pnsa))

  (change-in-db [pnsa]
    (db/update-pnsa! pnsa))
  
  (delete-from-db [pnsa]
    (db/delete-pnsa! pnsa))

  communicate-procurement
  (uppercase-type [pns]
    (-> pns :type name cstr/upper-case))

  (title-string [{:keys [number title] :as pns}]
    (str (uppercase-type pns) "# " number " " title)))

(def ^:private procurement-regexes
  {:type #"(RFP|IFB)"
   :number #"(?i)PSS\s*(RFP|IFB)\s*\#\s*\:?\s*(\d+\-\d+)"
   :open_date #"(?i)^(\.*OPEN\:\s*)(\w+\s\d{2},\s\d{4})"
   :close_date #"(?i)^(\.*CLOSE\:\s*)(\w+\s\d{2},\s\d{4}\s+at\s+\d{1,2}\:\d{2}\s+am|pm)"
   :title #"(?i)Title\:\s*([\p{L}\p{Z}\p{M}\p{P}\p{N}]+)"})

(defn- procurement-reducer [rfp next-line]
  (let [this-line (reduce merge (map (fn [[k re]] {k (util/line-parser re next-line)}) procurement-regexes))]
    (merge-with util/select-non-nil this-line rfp)))

(defn create-pns-from-file 
  "Takes an announcement file and a specification file and uploads both to the WP site.  
  Parses information from the files to create a PSAnnouncement type record."
  [ann-file spec-file]
  (let [{:keys [tempfile
                size
                filename]} ann-file
        pdf-doc            (PDDocument/load tempfile)
        announcement       (.getText (PDFTextStripper.) pdf-doc)
        desc               (-> (re-find const/procurement-description-re announcement)
                               (last)
                               (cstr/trim))
        lines              (cstr/split announcement #"\n")]
    (.close pdf-doc)
    (as->
        (reduce procurement-reducer {} lines) rec
      (filter (comp some? val) rec)
      (map (fn [[k v]] [k (cstr/replace v #"\s+" " ")]) rec)
      (into {} rec)
      (assoc rec :type (-> rec :type cstr/lower-case keyword))
      (assoc rec :description desc)
      (assoc rec :open_date (-> rec :open_date util-dates/parse-date))
      (assoc rec :close_date (-> rec :close_date util-dates/parse-date-at-time))
      (util/make-status rec)
      (assoc rec :id (java.util.UUID/randomUUID))
      (assoc rec :file_link
             (wp/create-media filename tempfile
                              :title (:title rec)
                              :alt_text (str "Announcement for "
                                             (-> rec :type name cstr/upper-case)
                                             "# " (:number rec) " " (:title rec))
                              :description (-> (:description rec)
                                               (#(re-find #"([\p{L}\p{Z}\p{P}\p{M}\n]*?)\n\p{Z}\n" %))
                                               (last)
                                               (curl/url-encode))
                              :slug (:id rec)))
      (assoc rec :spec_link
             (wp/create-media (:filename spec-file) (:tempfile spec-file)
                              :title (:title rec)
                              :alt_text (str "Specifications for "
                                             (-> rec :type name cstr/upper-case)
                                             "# " (:number rec) " " (:title rec))
                              :description (-> (:description rec)
                                               (#(re-find #"([\p{L}\p{Z}\p{P}\p{M}\n]*?)\n\p{Z}\n" %))
                                               (last)
                                               (curl/url-encode))
                              :slug (str (:id rec) "-spec")))
      (map->PSAnnouncement rec))))

(defn- -get-pnsa [map]
  (if-let [pnsa (db/get-single-pnsa map)]
    (-> (update pnsa :type keyword)
        (map->PSAnnouncement))))

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
