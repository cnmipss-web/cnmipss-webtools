(ns webtools.meals-registration.matching.algorithms
  (:require [webtools.meals-registration.matching.random :refer [random-match]]
            [clj-time.core :as time]))

(defn match-dob [fns-records nap-records]
  "Match solely by date of birth, ignoring all other fields"
  (let [sorted-fns (atom (sort-by :dob fns-records))
        sorted-nap (atom (sort-by :dob nap-records))
        matched-fns (atom [])
        unmatched-fns (atom [])]
    (while (and (> (count @sorted-fns) 0)
                (> (count @sorted-nap) 0))
      (let [fns (first @sorted-fns)
            nap (first @sorted-nap)]
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
    
    (vector @matched-fns @unmatched-fns)))
