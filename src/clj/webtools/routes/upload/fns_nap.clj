(ns webtools.routes.upload.fns-nap
  (:require [dk.ative.docjure.spreadsheet :as ss]
            [clojure.java.io :refer [input-stream]]
            [clojure.spec.alpha :as spec]
            [clojure.string :as cstr]
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
    (and (spec/valid? ::mr/fns-type fns-type)
         (spec/valid? ::mr/last-name last-name)
         (spec/valid? ::mr/first-name first-name)
         (spec/valid? ::mr/grade grade)
         (spec/valid? ::mr/dob dob)
         (spec/valid? ::mr/school school)
         (spec/valid? ::mr/prev-school prev-school)
         (spec/valid? ::mr/date-registered date-registered)
         (spec/valid? ::mr/guardian guardian)
         (spec/valid? ::mr/gender gender)
         (spec/valid? ::mr/citizenship citizenship)
         (spec/valid? ::mr/ethnicity ethnicity)
         (spec/valid? ::mr/homeroom homeroom)
         (spec/valid? ::mr/school-year school-year)
         (spec/valid? ::mr/school-no school-no)
         (spec/valid? ::mr/uid uid)
         (spec/valid? ::mr/apid apid))))

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
    (and (spec/valid? ::mr/case-no case-no)
         (spec/valid? ::mr/last-name last-name)
         (spec/valid? ::mr/first-name first-name)
         (spec/valid? ::mr/dob dob)
         (spec/valid? ::mr/ethnicity ethnicity)
         (spec/valid? ::mr/age age))))

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
  (malgo/fuzzy-match fns-records nap-records))

(defn- -gen-matched-headers [matched]
  (let [{:keys [fns nap]} (first matched)
        parser (comp cstr/upper-case name first)]
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

(defn- -gen-unmatched-headers [[fns _]]
  (let [parser (comp cstr/upper-case name first)]
    (map parser (sort-by first fns))))

(defn- -fns-to-ss-row [fns]
  (map
   -clean-values-for-export
   (map second (sort-by first fns))))

(defn- -set-col-style [style col-no row]
  (ss/set-cell-style! (nth (ss/cell-seq row) col-no) style) row)

(defn- -create-ss-file [matched unm-fns unm-nap]
  (let [wb (ss/create-workbook "FNS-NAP Matches"
                               (vec (cons (-gen-matched-headers matched)
                                          (map -match-to-ss-row matched)))
                               "Unmatched FNS Records"
                               (vec (cons (-gen-unmatched-headers unm-fns)
                                          (map -fns-to-ss-row unm-fns)))
                               "Unmatched NAP Records"
                               (vec (cons (-gen-unmatched-headers unm-nap)
                                          (map -fns-to-ss-row unm-nap))))
        sheet (ss/select-sheet "FNS-NAP Matches" wb)
        header-row (first (ss/row-seq sheet))
        bm-date-rows (filter
                      (fn dates-dont-match? [row]
                        (let [[_ _ _ _ _ dob1 dob2] (map ss/read-cell (ss/cell-seq row))]
                          (not= dob1 dob2)))
                      (ss/row-seq sheet))
        bm-name-rows (filter
                      (fn lastnames-dont-match? [row]
                        (let [[ln1 nln] (->> (map ss/read-cell (ss/cell-seq row)) (take 18) (drop 16))
                              jr-index (cstr/index-of ln1 "Jr.")
                              fln (if (some? jr-index)
                                    (apply str (take jr-index ln1))
                                    ln1)
                              normalize (comp cstr/trim cstr/lower-case)]
                          (not= (normalize fln) (normalize nln))))
                      (ss/row-seq sheet))
        x (println (count bm-name-rows))
        x (println (count bm-date-rows))
        bm-date-style (ss/create-cell-style! wb {:background :rose
                                                 :data-format "MM/DD/YYYY"})
        bm-name-style (ss/create-cell-style! wb {:background :rose
                                                 :data-format "@"})]
    (ss/set-row-style! header-row (ss/create-cell-style! wb {:background :pale_blue
                                                             :font {:bold true}}))
    (doall (map (partial -set-col-style bm-date-style 5) bm-date-rows))
    (doall (map (partial -set-col-style bm-date-style 6) bm-date-rows))
    (doall (map (partial -set-col-style bm-name-style 16) bm-name-rows))
    (doall (map (partial -set-col-style bm-name-style 17) bm-name-rows))
    (ss/save-workbook! "spreadsheet.xlsx" wb)))

(defn process-upload [params]
  (let [{uploaded-fns :fns-file
         uploaded-nap :nap-file} params
        {fns-file :tempfile} uploaded-fns
        {nap-file :tempfile} uploaded-nap
        fns-records (fns-parse fns-file)
        nap-records (nap-parse nap-file)
        [matched-fns unmatched-fns unmatched-nap] (-matching-algorithm (:valid fns-records) (:valid nap-records))]
    (-create-ss-file matched-fns unmatched-fns unmatched-nap)
    "1234567890"))
