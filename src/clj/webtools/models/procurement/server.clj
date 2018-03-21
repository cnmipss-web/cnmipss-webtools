(ns webtools.models.procurement.server
  (:require [cemerick.url :as curl]
            [clojure.string :as cstr]
            [clojure.tools.logging :as log]
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
    (-> pnsa :type keyword))
  
  (save-to-db [pnsa]
    (-> pnsa
        (db/make-sql-date :open_date)
        (db/make-sql-datetime :close_date)
        (db/create-pnsa!)))

  (change-in-db [pnsa]
    (db/update-pnsa! pnsa))
  
  (delete-from-db [pnsa]
    (db/delete-pnsa! pnsa))

  communicate-procurement
  (uppercase-type [pns]
    (-> pns :type name cstr/upper-case))

  (title-string [{:keys [number title] :as pns}]
    (str (uppercase-type pns) "# " number " " title))
  
  (changes-email [orig new sub]
    (let [title-string (str (-> new :type name cstr/upper-case)
                            "# " (:number orig) " " (:title orig))
          referent-term (if (= :rfp (:type new)) "request" "invitation")]
      [:body
       [:p (str "Greetings " (:contact_person sub) ",")]
       [:p (str "We would like to notify you that details of " title-string " have been changed.")]

       (if (not= (:open_date orig) (:open_date new))
         [:p (str "The window for submissions will now begin on "
                  (util-dates/print-date (:open_date new)) ".  ")])

       (if (not= (:close_date orig) (:close_date new))
         [:p (str "The window for submissions will now close at "
                  (util-dates/print-date-at-time (:close_date new)) ".  ")])

       (if (not= (:number orig) (:number new))
         [:p (str "The " (-> new :type name cstr/upper-case)
                  "# of this request has been changed to "
                  (:number new) ".  ")])

       (if (not= (:title orig) (:title new))
         [:p (str "The title of this " referent-term " has been changed to: ")
          [:em (:title new)] ".  "])

       (if (not= (:description orig) (:description new))
         [:p
          (str "The description of this " referent-term " has been change to the following: ")
          [:br]
          [:br]
          (:description new)])

       [:br]
       [:p "If you have any questions, please contact Kimo Rosario at kimo.rosario@cnmipss.org"]
       [:br]
       [:p "Thank you,"]
       [:p "Kimo Rosario"]
       [:p "Procurement & Supply Officer"]
       [:p "CNMI PSS"]])))

(def procurement-regexes
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
        desc               (-> (re-find
                                #"(?i)Title\:\s*[\p{L}\p{M}\p{P}\n\s\d]*?\n([\p{L}\p{M}\p{P}\n\s\d]+?)\/s\/"
                                announcement)
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

(extend-protocol procurement-from-db
  java.lang.String
  (get-pns-from-db [id]
    (-> {:id (make-uuid id)}         
        (db/get-single-pnsa)
        (map->PSAnnouncement)))

  (get-subs-from-db [proc_id]
    (map p/convert-sub-from-map (db/get-subscriptions {:proc_id (p/make-uuid proc_id)})))

  (make-uuid [id] (java.util.UUID/fromString id))

  java.util.UUID
  (get-pns-from-db [uuid]
    (-> {:id uuid}
        (db/get-single-pnsa)
        (map->PSAnnouncement)))

  (get-subs-from-db [proc_id]
    (map p/convert-sub-from-map (db/get-subscriptions {:proc_id proc_id})))
  
  (make-uuid [id] id)
  
  nil
  (get-pns-from-db [id] nil)
  (get-subs-from-db [proc_id] nil)
  (make-uuid [id] nil))


(let [f (fn [pns]
          (try
            (if (every? some? [(:number pns) (:type pns) (:id pns)])
              (-> (assoc pns :id (-> pns :id make-uuid))
                  (assoc :open_date (-> pns :open_date util-dates/parse-date))
                  (assoc :close_date (-> pns :close_date util-dates/parse-date-at-time))
                  map->PSAnnouncement))
            (catch Exception e
              (log/error e)
              (throw e))))
      g (fn [sub]
          (try
            (if (every? some? [(:id sub) (:proc_id sub)])
              (-> (assoc sub :id (-> sub :id make-uuid))
                  (assoc :proc_id (-> sub :proc_id make-uuid))
                  (assoc :telephone (-> sub :telephone util/format-tel-num))
                  map->Subscription))
            (catch Exception e
              (log/error e)
              (throw e))))]
  (extend-protocol create-procurement
    clojure.lang.PersistentArrayMap
    (convert-pns-from-map [pns] (f pns))
    (convert-sub-from-map [sub] (g sub))

    clojure.lang.PersistentHashMap
    (convert-pns-from-map [pns] (f pns))
    (convert-sub-from-map [sub] (g sub))))
