(ns webtools.meals-registration.matching.algorithms
  (:require [webtools.meals-registration.matching.random :refer [random-match]]
            [clj-time.core :as time]
            [clj-fuzzy.metrics :as smetric]))

(defn match-dob [fns-records nap-records]
  "Match solely by date of birth, ignoring all other fields"
  (let [sorted-fns (atom (sort-by :dob fns-records))
        sorted-nap (atom (sort-by :dob nap-records))
        matched-fns (atom [])
        unmatched-fns (atom [])
        comparisons (atom 0)]
    (while (and (> (count @sorted-fns) 0)
                (> (count @sorted-nap) 0))
      (let [fns (first @sorted-fns)
            nap (first @sorted-nap)]
        (swap! comparisons inc)
        (when (time/equal? (:dob fns) (:dob nap))
          (swap! matched-fns #(conj % {:fns fns :nap nap}))
          (swap! sorted-fns next)
          (swap! sorted-nap next))
        (when (time/before? (:dob fns) (:dob nap))
          (swap! unmatched-fns #(conj % fns))
          (swap! sorted-fns next))
        (when (time/after? (:dob fns) (:dob nap))
          (swap! sorted-nap next))))
    (when (> (count @sorted-fns) 0)
      (swap! unmatched-fns #(conj %)))
    (println "Total Comparisons: " @comparisons)
    (vector @matched-fns @unmatched-fns)))


;; Matching using jaro-winkler string metrics
(def ^:private jw-threshold-h 0.9)
(def ^:private jw-threshold-l 0.5)
;; (def ^:private dice-threshold 0.5)

(defn- -clean-name [name]
  "Clean a name by lower-casing and removing punctuation and spaces"
  (apply str (remove #((set " .;,'") %) (clojure.string/lower-case name))))

(defn- -full-name [record]
  (-clean-name (str (:first-name record) " " (:last-name record))))

(defn- -full-name-match? [fns nap]
  "Returns true if :first-name and :last-name values of fns and nap are within the jw-threshold-HIGH"
  (let [f-fns (-full-name fns)
        f-nap (-full-name nap)]
    (< jw-threshold-h (smetric/jaro-winkler f-fns f-nap))))

(defn- -partial-name-match? [fns nap]
  "Returns true if :first-name and :last-name values of fns and nap are within the jw-threshold-LOW"
  (let [f-fns (-full-name fns)
        f-nap (-full-name nap)]
    (< jw-threshold-l (smetric/jaro-winkler f-fns f-nap))))

(defn jw-match-names [fns-records nap-records]
  (let [sorted-fns (atom (sort-by :dob fns-records))
        total-fns (count @sorted-fns)
        sorted-nap (atom (sort-by :dob nap-records))
        total-nap (count @sorted-nap)
        matched-fns (atom [])
        unmatched-fns (atom [])
        comparisons (atom 0)]
    (println "\n\n-------------------------------------------------------------------------\n\n")
    (println "Comparing" total-fns "FNS Registrations to" total-nap "NAPRegistrations\n")
    (doall
     (pmap
      (fn [fns]
        (let [match-found (atom false)]
          (dorun
           (map (fn [nap]
                  (when (not @match-found)
                    (swap! comparisons inc)
                    (let [f-match (-full-name-match? fns nap)
                          p-match (-partial-name-match? fns nap)
                          
                          d-match (time/equal? (:dob fns) (:dob nap))
                          any-match (or f-match
                                        (and p-match
                                             d-match))]
                      (when any-match
                        (reset! match-found true)
                        (swap! matched-fns #(conj % {:fns fns :nap nap}))
                        (reset! sorted-nap (filter (partial not= nap) @sorted-nap))))))
                @sorted-nap))
          (if (not @match-found)
            (swap! unmatched-fns #(conj % fns)))
          (let [completed (+ (count @unmatched-fns) (count @matched-fns))]
            (if (= 0 (mod completed 25))
              (println "Completed" completed "/" total-fns)))))
      @sorted-fns))
    (println "Total Comparisons: " @comparisons)
    (println "Max Possible Comparisons" (* total-nap total-fns))
    (vector @matched-fns @unmatched-fns)))
