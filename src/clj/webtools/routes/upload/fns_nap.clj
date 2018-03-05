(ns webtools.routes.upload.fns-nap
  (:require [dk.ative.docjure.spreadsheet :as ss]
            [clojure.java.io :refer [input-stream]]
            [clojure.spec.alpha :as s]
            [webtools.meals-registration.core :refer [->FNSRegistration ->NAPRegistration] :as mr]
            [webtools.util.dates :refer [parse-nap-date]]
            [webtools.meals-registration.matching.algorithms :as malgo]))

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

(defn- -is-valid-fns? [row]
  "Predicate function to evaluate whether row represents a valid FNSRegistration"
  (let [[fns-type last-name first-name
         grade dob school prev-school
         date-registered guardian gender
         citizenship ethnicity homeroom
         school-year school-no uid apid] row]
    (and (s/valid? ::mr/fns-type fns-type)
         (s/valid? ::mr/last-name last-name)
         (s/valid? ::mr/first-name first-name)
         (s/valid? ::mr/grade grade)
         (s/valid? ::mr/dob dob)
         (s/valid? ::mr/school school)
         (s/valid? ::mr/prev-school prev-school)
         (s/valid? ::mr/date-registered date-registered)
         (s/valid? ::mr/guardian guardian)
         (s/valid? ::mr/gender gender)
         (s/valid? ::mr/citizenship citizenship)
         (s/valid? ::mr/ethnicity ethnicity)
         (s/valid? ::mr/homeroom homeroom)
         (s/valid? ::mr/school-year school-year)
         (s/valid? ::mr/school-no school-no)
         (s/valid? ::mr/uid uid)
         (s/valid? ::mr/apid apid))))

(def ^:private -fns-row-parser (comp -fns-normalize-dates
                                     -convert-gender
                                     -convert-fns-type
                                     -numbers-to-int
                                     -nils-to-empty-string
                                     (partial map ss/read-cell)))

(defn- -separate-invalid-fns [{:keys [valid invalid]} row]
  "Reducer function to iterate over rows in fns-file and create a hash-map containing
   a seq of :valid values and a seq of :invalid values."
  (let [value-row (-fns-row-parser row)]
    (if (-is-valid-fns? value-row)
      {:valid (conj valid value-row)
       :invalid invalid}
      {:valid valid
       :invalid (conj invalid value-row)})))

(defn- -ss-file-to-seq [file]
  "Convert a java.io.File object pointing at a MS Excel file to a seq of rows, 
   each row being a seq of cell values"
  (->> (input-stream file)
       (ss/load-workbook)
       (ss/sheet-seq)
       (first)                 ;; take first sheet
       (ss/row-seq)           
       (next)                  ;; skip header row
       (map ss/cell-seq)))

(defn fns-parse [fns-file]
  "Create a sequence of FNSRegistration records from fns-file"
  (let [result-map (reduce -separate-invalid-fns 
                           {:valid nil :invalid nil} 
                           (-ss-file-to-seq fns-file))]
    (update result-map :valid (partial map (partial apply ->FNSRegistration)))))

(defn -nap-normalize-dates [row]
  (let [[_ _ _ _ dob] row
        nap-date-fmt (re-pattern "\\d{2}\\-[a-zA-z]{3}\\-\\d{2}")
        normalize-dob (cond
                          (instance? java.util.Date dob) (clj-time.coerce/from-date dob)
                          (and (string? dob)
                               (re-matches nap-date-fmt dob)) (parse-nap-date dob)
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

(defn- -is-valid-nap? [row]
  "Predicate function to evaluate whether row represents a valid FNSRegistration"
  (let [[case-no last-name first-name
         ethnicity dob age] row]
    (and (s/valid? ::mr/case-no case-no)
         (s/valid? ::mr/last-name last-name)
         (s/valid? ::mr/first-name first-name)
         (s/valid? ::mr/dob dob)
         (s/valid? ::mr/ethnicity ethnicity)
         (s/valid? ::mr/age age))))

(def ^:private -nap-row-parser (comp -nap-case-nos
                                     -nap-normalize-dates
                                     -numbers-to-int
                                     -nils-to-empty-string
                                     (partial map ss/read-cell)))

(defn- -separate-invalid-nap [{:keys [valid invalid]} row]
  "Reducer function to iterate over rows in fns-file and create a hash-map containing
   a seq of :valid values and a seq of :invalid values."
  (let [value-row (-nap-row-parser row)]
    (if (-is-valid-nap? value-row)
      {:valid (conj valid value-row)
       :invalid invalid}
      {:valid valid
       :invalid (conj invalid value-row)})))

(defn nap-parse [nap-file]
  "Create a sequence of NAPRegistration records from nap-file"
  (let [result-map (reduce -separate-invalid-nap 
                           {:valid nil :invalid nil} 
                           (-ss-file-to-seq nap-file))]
    (update result-map :valid (partial map (partial apply ->NAPRegistration)))))


(defn -matching-algorithm [fns-records nap-records]
  (malgo/jw-match-names fns-records nap-records))

(defn- -gen-matched-headers [matched]
  (let [{:keys [fns nap]} (first matched)
        parser (comp clojure.string/upper-case name first)]
    (map parser (sort-by first (concat (seq fns) (seq nap))))))

(defn- -clean-values-for-export [val]
  (cond
    (keyword? val) (name val)
    (instance? org.joda.time.DateTime val) (clj-time.coerce/to-date val)
    :else val))

(defn- -match-to-ss-row [{:keys [fns nap]}]
  (map
   -clean-values-for-export
   (map second (sort-by first (concat (seq fns) (seq nap))))))

(defn- -create-ss-file [matched unmatched]
  (let [wb (ss/create-workbook "FNS-NAP Matches"
                               (vec (cons (-gen-matched-headers matched)
                                          (map -match-to-ss-row matched))))
        sheet (ss/select-sheet "FNS-NAP Matches" wb)
        header-row (first (ss/row-seq sheet))]
    (ss/set-row-style! header-row (ss/create-cell-style! wb {:background :yellow,
                                                             :font {:bold true}}))
    (ss/save-workbook! "spreadsheet.xlsx" wb)))

(defn process-upload [params]
  (let [{uploaded-fns :fns-file
         uploaded-nap :nap-file} params
        {fns-file :tempfile} uploaded-fns
        {nap-file :tempfile} uploaded-nap
        fns-records (fns-parse fns-file)
        nap-records (nap-parse nap-file)
        [matched-fns unmatched-fns] (-matching-algorithm (:valid fns-records) (:valid nap-records))]
    (-create-ss-file matched-fns unmatched-fns)
    "1234567890"))
