(ns webtools.meals-registration.matching.algorithms
  (:require [clojure.core.reducers :as r]
            [clojure.string :as cstr]
            [clj-fuzzy.metrics :as smetric]
            [clj-fuzzy.phonetics :as sphon]
            [clj-time.core :as time]
            [clj-time.format :as tform]
            [webtools.constants :as const]
            [webtools.meals-registration.matching.random :refer [random-match]]
            [webtools.meals-registration.matching.algorithms.fuzzy
             :refer [apply-fuzzy-match-algorithm]]))

(defn dob-match [fns-records nap-records]
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


(defn fuzzy-match [fns-records nap-records]
  "Attempt to match records from FNS and NAP registrations using a combination 
  of fuzzy string metrics, DOB matching, and ethnicity matching.  O(f * n), but 
  fast enough due to binning into many small groups resulting in effectively
  O( f * n / b ) where b is the number of bins."
  (apply-fuzzy-match-algorithm fns-records nap-records))
