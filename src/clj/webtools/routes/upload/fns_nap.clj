(ns webtools.routes.upload.fns-nap
  (:require [dk.ative.docjure.spreadsheet :as ss]
            [clojure.java.io :refer [input-stream]]
            [webtools.meals-registration.core :refer [->FNSRegistration ->NAPRegistration]]))

(defn- -nils-to-empty-string [row]
  (map (fn [cell]
         (if-not (some? cell)
           ""
           cell))
       row))

(defn- -convert-fns-type [row]
  (let [raw-type (first row)
        converted-type ({"R" :reduced
                         "F" :free
                         "No" :none
                         "" :none} raw-type)
        remainder (next row)
        new-row   (cons converted-type remainder)]
    new-row))

(defn- -convert-gender [row]
  (update (apply vector row)
          9
          #({"F" :female
             "M" :male} %)))


(defn- -numbers-to-int [row]
  (map (fn [cell]
         (if (number? cell)
             (int cell)
             cell))
       row))

(defn- -fns-normalize-dates [row]
  (let [[_ _ _ _ dob _ _ rgstr] row
        normalize-dob (if (instance? java.util.Date dob)
                        (clj-time.coerce/from-date dob)
                        nil)
        normalize-rgstr (if (instance? java.util.Date rgstr)
                          (clj-time.coerce/from-date rgstr)
                          nil) ]
    (-> (apply vector row)
        (assoc 4 normalize-dob)
        (assoc 7 normalize-rgstr)
        (seq))))

(defn fns-parse [fns-file]
  "Create a sequence of FNSRegistration records from fns-file"
  (let [row-parser (comp #(apply ->FNSRegistration %)
                         -fns-normalize-dates
                         -convert-gender
                         -convert-fns-type
                         -numbers-to-int
                         -nils-to-empty-string
                         #(map ss/read-cell %)
                         ss/cell-seq)]
    (->> (input-stream fns-file)
         (ss/load-workbook)
         (ss/sheet-seq)
         (first)
         (ss/row-seq)
         (next)
         (map row-parser))))

(defn -nap-normalize-dates [row]
  (let [[_ _ _ _ dob] row
        normalize-dob (cond
                          (instance? java.util.Date dob) (clj-time.coerce/from-date dob)
                          (string? dob) (clj-time.coerce/from-string dob)
                          :else nil)]
    (-> (apply vector row)
        (assoc 4 normalize-dob)
        (seq))))

(defn- -nap-case-nos [row]
  (let [[case-no _ _] row
        case-no-int (cond
                      (string? case-no) (Integer/parseInt case-no)
                      (number? case-no) (int case-no)
                      :else nil)]
    (-> (apply vector row)
        (assoc 0 (Integer/parseInt case-no))
        (seq))))

(defn nap-parse [nap-file]
  "Create a sequence of NAPRegistration records from nap-file"
  (let [row-parser (comp #(apply ->NAPRegistration %)
                         -nap-case-nos
                         -nap-normalize-dates
                         -numbers-to-int
                         -nils-to-empty-string
                         #(map ss/read-cell %)
                         ss/cell-seq)]
    (->> (input-stream nap-file)
         (ss/load-workbook)
         (ss/sheet-seq)
         (first)
         (ss/row-seq)
         (next)
         (map row-parser))))



(defn process-upload [params]
  (let [{uploaded-fns :fns-file
         uploaded-nap :nap-file} params
        {fns-file :tempfile} uploaded-fns
        {nap-file :tempfile} uploaded-nap
        fns-records (fns-parse fns-file)
        nap-records (nap-parse fns-file)]
    (println fns-records nap-records)
    ))
