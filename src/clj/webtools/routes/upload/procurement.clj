(ns webtools.routes.upload.procurement
  "Provides public routines for processing uploaded files related to the Procurement role."
  (:require [cemerick.url :as curl]
            [clojure.string :as cstr]
            [webtools.constants :as const]
            [webtools.db.core :as db]
            [webtools.email :as email]
            [webtools.exceptions.procurement :as ex]
            [webtools.models.procurement.core :as p]
            [webtools.models.procurement.server]
            [webtools.util :as util]
            [webtools.util.dates :as util-dates]
            [webtools.wordpress-api :as wp])
  (:import (org.apache.pdfbox.pdmodel PDDocument)
           (org.apache.pdfbox.text PDFTextStripper)))

(def ^:private procurement-regexes
  {:type #"(RFP|IFB)"
   :number #"(?i)PSS\s*(RFP|IFB)\s*\#\s*\:?\s*(\d+\-\d+)"
   :open_date #"(?i)^(\.*OPEN\:\s*)(\w+\s\d{2},\s\d{4})"
   :close_date #"(?i)^(\.*CLOSE\:\s*)(\w+\s\d{2},\s\d{4}\s+at\s+\d{1,2}\:\d{2}\s+am|pm)"
   :title #"(?i)Title\:\s*([\p{L}\p{Z}\p{M}\p{P}\p{N}]+)"})

(defn- procurement-reducer [rfp next-line]
  (let [this-line (reduce merge (map (fn [[k re]] {k (util/line-parser re next-line)}) procurement-regexes))]
    (merge-with util/select-non-nil this-line rfp)))

(defn- create-pns-from-file 
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
      (p/map->PSAnnouncement rec))))

(defn process-procurement-pdf
  [params]
  (let [{:keys [ann-file spec-file]} params
        pns (create-pns-from-file ann-file spec-file)]
    (p/save-to-db! pns)))

(defn process-procurement-addendum
  [params]
  (let [{:keys [file id number type]} params
        {:keys [tempfile size filename]} file
        uuid (p/make-uuid id)
        slug (java.util.UUID/randomUUID)
        existing-addenda (filter #(= id (:proc_id %)) (db/get-all-addenda))
        file_link (wp/create-media filename tempfile
                                   :title (str "Addendum " (inc (count existing-addenda))
                                               " for "
                                               (clojure.string/upper-case (name type))
                                               " " number)
                                   :slug slug)]
    (if (nil? file_link)
      (throw (ex/wordpress-upload-failed filename))
      (do
        (future (email/notify-subscribers :addenda (p/get-pns-from-db uuid) {:file_link file_link
                                                                             :proc_id uuid}))
        (db/create-addendum! {:id slug
                              :file_link file_link
                              :proc_id uuid
                              :number (inc (count existing-addenda))})))))

