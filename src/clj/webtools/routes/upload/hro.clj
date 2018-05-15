(ns webtools.routes.upload.hro
  "Provides public routines for processing uploaded files related to the HRO role."
  (:require [clojure.string :as cstr]
            [webtools.db :as db]
            [webtools.util :as util]
            [webtools.util.dates :as util-dates]
            [webtools.wordpress-api :as wp])
  (:import (org.apache.pdfbox.pdmodel PDDocument)
           (org.apache.pdfbox.text PDFTextStripper)))

(def ^:private jva-regexes
  "Regexes to parse JVA for key information"
  {:announce_no #"(?i)ANNOUNCEMENT\s*NO\.?:\s*(.*\S)"
   :position #"(?i)(POSITION/TITLE)\s*:\s*(.*\S)"
   :open_date #"(?i)OPEN(ING)?\s*DATE\s*:\s*(.*)\s*CL"
   :close_date #"(?i)CLOSE?(ING)?\s*DATE:\s*(.*\S)"
   :salary #"(?i)(SALARY|DIFFERENTIAL)\s*:\s*(.*\S)"
   :location #"(?i)^LOCATION\s*:\s*(.*\w)"})

(defn- jva-reducer
  "Reducer fn that turns a sequence of strings from JVA file into a map of JVA 
  data using jva-regexes to grab data from the sequence of strings."
  [jva next-line]
  (let [this-line (reduce merge (map (fn [[k re]] {k (util/line-parser re next-line)}) jva-regexes))]
    (merge-with util/select-non-nil this-line jva)))

(defn- jva-desc
  "Returns a string description based on JVA data"
  [jva]
  (str "Job Vacancy Announcement for "
       (:position jva) " open from " (:open_date jva)
       " to " (if-let [close (:close_date jva)]
                close
                "until filled.")))

(defn- format-close-date [date]
  (if-not (re-seq #"(?i)until filled" date)
    (util-dates/parse-date date)
    nil))

(defn- make-jva-record [text]
  (as-> (reduce jva-reducer {} text) jva
    (update jva :open_date util-dates/parse-date)
    (update jva :close_date format-close-date)
    (util/make-status jva)
    (assoc jva :id (java.util.UUID/randomUUID))))

(defn process-jva-pdf
  ""
  [params]
  (let [{:keys [file]} params]
    (if (= (type file) clojure.lang.PersistentArrayMap)
      (let [{:keys [tempfile
                    size
                    filename]} file
            pdf-document       (PDDocument/load tempfile)
            jva                (.getText (PDFTextStripper.) pdf-document)
            text-list          (cstr/split jva #"\n")
            jva-record         (as-> (make-jva-record text-list) jva
                                 (assoc jva :file_link
                                        (wp/create-media filename tempfile
                                                         :title (:position jva)
                                                         :alt_text (str "Job Vacancy Announcement for "
                                                                        (:position jva))
                                                         :description (jva-desc jva)
                                                         :slug (:id jva))))]
        (.close pdf-document)
        (db/create-jva! jva-record))
      (mapv (comp process-jva-pdf #(into {} [[:file %]])) file))))

(defn process-reannouncement
  [params]
  (let [{:keys [file]}                   params
        {:keys [tempfile size filename]} file
        pdf-document                     (PDDocument/load tempfile)
        jva                              (.getText (PDFTextStripper.) pdf-document)
        text-list                        (cstr/split jva #"\n")
        jva-record                       (make-jva-record text-list)
        existing-jva                     (db/get-jva jva-record)]
    (db/delete-jva! existing-jva)
    (wp/delete-media (str (:id existing-jva)))
    (.close pdf-document)
    (->(assoc jva-record :file_link
              (wp/create-media filename tempfile
                               :title (:position jva-record)
                               :alt_text (str "Job Vacancy Announcement for"
                                              (:position jva-record))
                               :description (jva-desc jva-record)
                               :slug (:id jva-record)))
       
       (db/create-jva!))))

