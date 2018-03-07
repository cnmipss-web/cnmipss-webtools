(ns webtools.meals-registration.matching.algorithms
  (:require [clojure.core.reducers :as r]
            [clojure.string :as string]
            [clj-fuzzy.metrics :as smetric]
            [clj-fuzzy.phonetics :as sphon]
            [clj-time.core :as time]
            [clj-time.format :as tform]
            [webtools.constants :as const]
            [webtools.meals-registration.matching.random :refer [random-match]]))

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
(def ^:private jw-threshold-high 0.93)
(def ^:private jw-threshold-low  0.83)
;; (def ^:private dice-threshold 0.5)

(defn- -clean-name [name]
  "Clean a name by lower-casing and removing punctuation and spaces"
  (apply str (remove #((set "/.;,'") %) (clojure.string/lower-case name))))

(defn- -full-name [record]
  [(-clean-name (:first-name record))
   (-clean-name (:last-name record))])

(defn- -match-ethnicity?
  [fns nap]
  (let [eth-map {"BA" "Bangladeshi"
                 "BL" "Bangladeshi"
                 "CH" "Chamorro"
                 "CL" "Carolinian"
                 "CN" "Chinese"
                 "FL" "Filipino"
                 "KO" "Korean"
                 "MA" "Marshallese"
                 "PA" "Palauan"
                 "PO" "Pohnpeian"
                 "TA" "Thai"
                 "TI" "Thai"
                 "TR" "Chuukese"
                 "YA" "Yapese"
                 }]
    (= (get eth-map (:ethnicity nap)) (:ethnicity fns))))

(defn -calc-fuzzy-match [fns nap]
  (let [fns-name (-full-name fns)
        nap-name (-full-name nap)
        f-fns (string/join " " fns-name)
        f-nap (string/join " " nap-name)
        dob-match (time/equal? (:dob fns) (:dob nap))
        mra-first (smetric/mra-comparison (first fns-name) (first nap-name))
        mra-last (smetric/mra-comparison (last fns-name) (last nap-name))
        eth-match (-match-ethnicity? fns nap)
        low-dob-dist? (let [fns-dob (tform/unparse const/numeric-date-formatter (:dob fns))
                            fns-nap (tform/unparse const/numeric-date-formatter (:dob nap))
                            edit-dist (smetric/levenshtein fns-dob fns-nap)]
                        ;;(if (> edit-dist 2) (println fns-dob fns-nap edit-dist))
                        (<= edit-dist 2))]
    {:fns fns
     :nap nap
     ::low-dob-dist? low-dob-dist?
     ::jw-full-match? (and (< jw-threshold-low (smetric/jaro-winkler f-fns f-nap)))
     ::jw-eth-match? (and (< jw-threshold-low (smetric/jaro-winkler f-fns f-nap))
                          eth-match)
     ::jw-sep-names? (and (< jw-threshold-high (smetric/jaro-winkler (first fns-name) (first nap-name)))
                          (< jw-threshold-high (smetric/jaro-winkler (last fns-name) (last nap-name))))
     ::mra-eth-match? (and (:match mra-first)
                           (:match mra-last)
                           eth-match)
     ::jw (smetric/jaro-winkler f-fns f-nap)})) 

(defn- -fuzzy-match? [{::keys [jw-eth-match? mra-eth-match? jw-sep-names? low-dob-dist?]}]
  (and (or jw-eth-match?
           jw-sep-names?
           mra-eth-match?)
       low-dob-dist?))

(defn- -select-best-jw
  ([x y]
   (if (> (::jw x) (::jw y)) x y))
  ([] {::jw 0}))

(defn- -bin-records-by-date [fns-records nap-records]
  (let [bin-fn (fn [k records]
                 (->> (reduce (fn [coll val]
                                (update coll (:dob val) conj val)) {}
                              (sort-by :dob records))
                      (map (fn [[key val]]
                             [key {k val}]))
                      (into {})))
        sorted-fns (bin-fn :fns-coll fns-records)
        sorted-nap (bin-fn :nap-coll nap-records)]
    (->> (merge-with (fn [fns nap]
                        (merge fns nap)) sorted-fns sorted-nap))))

(defn- -bin-records-by-sx [fns-records nap-records]
  (let [bin-fn (fn [k records]
                 (->> (reduce (fn [coll val]
                                (update coll (sphon/mra-codex (:last-name val)) conj val)) {}
                              (sort-by :dob records))
                      (map (fn [[key val]]
                             [key {k val}]))
                      (into {})))
        sorted-fns (bin-fn :fns-coll fns-records)
        sorted-nap (bin-fn :nap-coll nap-records)]
    (->> (merge-with (fn [fns nap]
                        (merge fns nap)) sorted-fns sorted-nap))))

(defn jw-match-names [fns-records nap-records]
  "Attempt to match records from FNS and NAP registrations using a combination of fuzzy string metrics, DOB matching, and ethnicity matching.  O(f * n)"
  ;; Performance testing:
  
  ;; Sequence methods:
  ;; 
  ;; pmap over fns->pmap over nap  210.68 seconds for typical input w/ 2630 fns and 3252 nap
  ;; pmap over fns->map over nap   185.4 seconds for typical input w/ 2630 fns and 3252 nap
  ;; map over fns->map over nap    1046.6 seconds for typical input w/ 2630 fns and 3252 nap
  
  ;; Reducer methods:
  ;;
  ;; r/fold, r/map, r/filter 217.5 seconds for typical input w/ 2630 fns and 3252 nap
  
  (let [records-by-date (-bin-records-by-date fns-records nap-records)
        total-fns (count (set fns-records))
        total-nap (count (set nap-records))
        matched-fns (atom [])
        unmatched-fns (atom (set fns-records))
        unmatched-nap (atom (set nap-records))
        comparisons (atom 0)]
    (println "\n\n-------------------------------------------------------------------------\n\n")
    (println "Comparing" total-fns "FNS Registrations to" total-nap "NAPRegistrations\n")
    (r/foldcat
     (r/map
      (fn match-records-binned-by-date [dob {:keys [fns-coll nap-coll]}]
        (r/foldcat
         (r/map (fn compare-fns-to-nap [fns]
                  (let [match (->> (r/map (partial -calc-fuzzy-match fns) nap-coll)
                                   (r/filter -fuzzy-match?)
                                   (r/fold -select-best-jw))]
                    (when (< 0 (::jw match))
                      (swap! matched-fns #(conj % match))
                      (swap! unmatched-fns #(disj % (:fns match)))
                      (swap! unmatched-nap #(disj % (:nap match))))))
                fns-coll)))
      records-by-date))

    (let [records-by-sx (-bin-records-by-sx (vec @unmatched-fns) (vec @unmatched-nap))]
      (r/foldcat
       (r/map
        (fn match-records-binned-by-date [_ {:keys [fns-coll nap-coll]}]
          (r/foldcat
           (r/map (fn compare-fns-to-nap [fns]
                    (let [match (->> (r/map (partial -calc-fuzzy-match fns) nap-coll)
                                     (r/filter -fuzzy-match?)
                                     (r/fold -select-best-jw))]
                      (when (< 0 (::jw match))
                        (swap! matched-fns #(conj % match))
                        (swap! unmatched-fns #(disj % (:fns match)))
                        (swap! unmatched-nap #(disj % (:nap match))))))
                  fns-coll)))
        records-by-sx)))
    
    (vector @matched-fns (vec @unmatched-fns) (vec @unmatched-nap))))

(defn hybrid-match [fns-records nap-records]
  "Hybrid algorithm -- apply O(f) algorithms first, apply O(f*n) algorithm on remaining unmatched records.")
