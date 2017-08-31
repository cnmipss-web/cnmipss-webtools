(ns webtools.procurement.server
  (:require [webtools.procurement.core :refer :all]
            [webtools.db.core :as db]
            [webtools.wordpress-api :as wp]
            [webtools.constants :as const]
            [webtools.util :as util]
            [webtools.util.dates :as util-dates]
            [clj-time.format :as f])
  (:import [org.apache.pdfbox.pdmodel PDDocument]
           [org.apache.pdfbox.text PDFTextStripper])) 

(extend-type webtools.procurement.core.PSAnnouncement
    process-procurement
    (proc-type [pnsa]
      (-> pnsa :type keyword))
    
    (save-to-db [pnsa]
      (db/create-pnsa! pnsa))

    (change-in-db [pnsa]
      (db/update-pnsa! pnsa))
  
    (delete-from-db [pnsa]
      (db/delete-pnsa! pnsa))

    communicate-procurement
    (changes-email [orig new sub]
      (let [title-string (str (-> new proc-type name clojure.string/upper-case)
                              "# " (:number new) " " (:title new))
            referent-term (if (= :rfp (proc-type new)) "request" "invitation")]
        [:html
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
            [:p (str "The " (-> new :type name clojure.string/upper-case)
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
          [:p "CNMI PSS"]]])))

(def procurement-regexes
  {:type #"(RFP|IFB)"
   :number #"(?i)PSS (RFP|IFB)\#\:\s*(\d{2}\-\d{3})"
   :open_date #"(?i)^(\.*OPEN\:\s*)(\w+\s\d{2},\s\d{4})"
   :close_date #"(?i)^(\.*CLOSE\:\s*)(\w+\s\d{2},\s\d{4}\s+at\s+\d{1,2}\:\d{2}\s+am|pm)"
   :title #"(?i)Title\:\s*([\p{L}\p{Z}\p{M}\p{P}\p{N}]+)"})

(defn- procurement-reducer [rfp next-line]
  (let [this-line (reduce merge (map (fn [[k re]] {k (util/line-parser re next-line)}) procurement-regexes))]
    (merge-with util/select-non-nil this-line rfp)))

(defn create-pns-from-file [file]
  (let [{:keys [tempfile size filename]} file
            announcement (->> tempfile PDDocument/load (.getText (PDFTextStripper.)))
            desc (-> (re-find #"(?i)Title\:\s*[\p{L}\p{M}\p{P}\n\s\d]*?\n([\p{L}\p{M}\p{P}\n\s\d]+?)\/s\/" announcement)
                     (last)
                     (clojure.string/trim))
            lines (clojure.string/split announcement #"\n")]
    (as->
        (reduce procurement-reducer {} lines) rec
      (filter (comp some? val) rec)
      (map (fn [[k v]] [k (clojure.string/replace v #"\s+" " ")]) rec)
      (into {} rec)
      (assoc rec :type (-> rec :type clojure.string/lower-case keyword))
      (assoc rec :description desc)
      (db/make-sql-date rec :open_date)
      (db/make-sql-datetime rec :close_date)
      (util/make-status rec)
      (assoc rec :id (java.util.UUID/randomUUID))
      (assoc rec :file_link
             (wp/create-media filename tempfile
                              :title (:title rec)
                              :alt_text (str "Announcement for "
                                             (-> rec :type name clojure.string/upper-case)
                                             "# " (:number rec) " " (:title rec))
                              :description (-> (:description rec)
                                               (#(re-find #"([\p{L}\p{Z}\p{P}\p{M}\n]*?)\n\p{Z}\n" %))
                                               (last)
                                               (cemerick.url/url-encode))
                              :slug (:id rec)))
      (map->PSAnnouncement rec))))

(extend-protocol retrieve-procurement
  java.lang.String
  (get-pns-from-db [id]
    (-> {:id (make-uuid id)}         
        (db/get-single-pnsa)
        (map->PSAnnouncement)))
  (make-uuid [id] (java.util.UUID/fromString id))

  java.util.UUID
  (get-pns-from-db [uuid]
    (-> {:id uuid}
         (db/get-single-pnsa)
         (map->PSAnnouncement)))
  (make-uuid [id] id)
  
  nil
  (get-pns-from-db [id] nil)
  (make-uuid [id] nil))


(let [f (fn [pns]
          (if (every? some? [(:number pns) (:type pns) (:id pns)])
            (-> (assoc pns :id (-> pns :id make-uuid))
                (assoc :open_date (-> pns :open_date util-dates/parse-date))
                (assoc :close_date (-> pns :close_date util-dates/parse-date-at-time))
                map->PSAnnouncement)))]
  (extend-protocol create-procurement
    clojure.lang.PersistentArrayMap
    (pns-from-map [pns] (f pns))

    clojure.lang.PersistentHashMap
    (pns-from-map [pns] (f pns))))
