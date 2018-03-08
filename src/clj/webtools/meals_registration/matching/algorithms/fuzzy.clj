(ns webtools.meals-registration.matching.algorithms.fuzzy
  "Implementation of fuzzy string matching algorithm for matching FNS 
  Registrations with NAP Registrations.  Uses clojure.core.reducers to
  multithread the matching process."
  (:require [clojure.spec.alpha :as spec]
            [clojure.spec.gen.alpha :as sgen]
            [clojure.core.reducers :as r]
            [clojure.string :as cstr]
            [clj-fuzzy.metrics :as smetric]
            [clj-fuzzy.phonetics :as sphon]
            [clj-time.core :as time]
            [clj-time.format :as tform]
            [webtools.constants :as const]))


;; Define match thresholds for Jaro-Winkler matching
(def ^:private jw-threshold-high 0.93)
(def ^:private jw-threshold-low  0.83)

;; Private methods
(defn- -clean-name [name]
  "Clean a name by lower-casing and removing punctuation and spaces"
  (apply str (remove #((set "/.;,'") %) (cstr/lower-case name))))

(defn- -full-name [record]
  "Clean :first-name and :last-name of punctuation and join first & last names 
  into a vector."
  [(-clean-name (:first-name record))
   (-clean-name (:last-name record))])

(defn- -ethnicity-match? [fns nap]
  "Returns true if :ethnicity values of fns and nap match, false if they do not."
  (let [fns-eth (:ethnicity fns)
        nap-eth (get const/fns-nap-ethnicity-mapping (:ethnicity nap))]
    (= fns-eth nap-eth)))

(defn- -fuzzy-match-data [fns nap]
  "Builds and returns a map containing information about the results of fuzzy 
  matching critera applied to fns and nap.  Returned map has the following values:

    :fns             -- An instance of FNSRegistration
    :nap             -- An instance of NAPRegistration
    ::low-dob-dist?  -- True if the the edit distance between the numeric
                        representations of (:dob fns) and (:dob nap) is 
                        less than or equal to two.
    ::jw-eth-match?  -- True if the -full-name from fns and nap pass the LOW
                        Jaro-Winkler threshold and the ethnicities match.
    ::jw-sep-names?  -- True if the :first-name and :last-name values from fns 
                        and nap pass the HIGH Jaro-Winkler threshold independently.
    ::mra-eth-match? -- True if the :first and :last-name value from fns and nap 
                        are MRA matches and the ethnicities match.
    ::jw             -- Returns the Jaro-Winkler similarity between the -full-name 
                        from fns and nap."
  (let [[fns-name nap-name] (map -full-name [fns nap])
        [fns-full-name nap-full-name] (map (partial cstr/join " ") [fns-name nap-name])
        jw-full (smetric/jaro-winkler fns-full-name nap-full-name)
        jw-first (smetric/jaro-winkler (first fns-name) (first nap-name))
        jw-last (smetric/jaro-winkler (last fns-name) (last nap-name))
        mra-first (smetric/mra-comparison (first fns-name) (first nap-name))
        mra-last (smetric/mra-comparison (last fns-name) (last nap-name))
        dob-match (time/equal? (:dob fns) (:dob nap))
        eth-match (-ethnicity-match? fns nap)
        low-dob-dist (let [fns-dob (tform/unparse const/numeric-date-formatter (:dob fns))
                           fns-nap (tform/unparse const/numeric-date-formatter (:dob nap))
                           edit-dist (smetric/levenshtein fns-dob fns-nap)]
                       (<= edit-dist 2))]
    {:fns fns
     :nap nap
     ::low-dob-dist? low-dob-dist
     ::jw-eth-match? (and (< jw-threshold-low jw-full)
                          eth-match)
     ::jw-sep-names? (and (< jw-threshold-high
                             jw-first)
                          (< jw-threshold-high
                             jw-last))
     ::mra-eth-match? (and (:match mra-first)
                           (:match mra-last)
                           eth-match)
     ::jw jw-full}))

(defn- -fuzzy-match? [{::keys [jw-eth-match? mra-eth-match? jw-sep-names? low-dob-dist?]}]
  "Takes the result of -fuzzy-match-data as input.
  
  Returns true if at least one match criteria is true AND the edit distance between 
  the numeric dates is less than or equal to 2."
  (and (or jw-eth-match?
           jw-sep-names?
           mra-eth-match?)
       low-dob-dist?))

(defn- -select-best-jw
  "Reducing function applied to a sequence of -fuzzy-match-data results.
  Returns the element with the highest Jaro-Winkler value."
  ([]     {::jw 0})
  ([& xs] (apply max-key ::jw xs)))

(defn- -bin-fn [key-fn]
  "Returns a function that will break a large sequence into a map of sequences
  grouped by the return value of key-fn on each element of the original sequence"
  (fn -bin-collection [coll]
    (reduce (fn -assoc-key-fn-and-v [c v]
              (update c (key-fn v) conj v))
            {}
            coll)))


(defn- -merge-binned [binned-fns binned-nap]
  "Merge two maps so that for each matching key in the source maps, the new map has
  the following value for the same key:

    {:fns-coll fns
     :nap-coll nap}

  Where either source map has no value for a given key in the other map, nil should
  be used."
  (let [fns-converted (reduce (fn [m [k v]]
                                  (assoc m k {::fns-coll v}))
                                {}
                                binned-fns)
        nap-converted (reduce (fn [m [k v]]
                                  (assoc m k {::nap-coll v}))
                                {}
                                binned-nap)]
    (merge-with into fns-converted nap-converted)))

(defn- -bin-by-dob [records]
  "Returns a map of records grouped by date of birth."
  (let [bin-fn (-bin-fn :dob)]
    (bin-fn records)))

(defn- -bin-by-mra [records]
  "Returns a map of records grouped by the mra codex value of their :last-name"
  (let [bin-fn (-bin-fn #(sphon/mra-codex (:last-name %)))]
    (bin-fn records)))

;; Public Method
(defn apply-fuzzy-match-algorithm [fns-records nap-records]
  "Attempt to match records from FNS and NAP registrations using a combination of fuzzy string metrics, DOB matching, and ethnicity matching.  O(f * n)"
  (let [fns-set (set fns-records)
        nap-set (set nap-records)
        total-fns (count fns-set)
        total-nap (count nap-set)
        fns-by-date (-bin-by-dob fns-set)
        nap-by-date (-bin-by-dob nap-set)
        combined-records-by-date (-merge-binned fns-by-date nap-by-date)
        matched-fns (atom [])
        unmatched-fns (atom fns-set)
        unmatched-nap (atom nap-set)

        compare-fns-to-nap-coll  (fn compare-fns-to-nap [nap-coll fns]
                                   (let [match (->> (r/map (partial -fuzzy-match-data fns) nap-coll)
                                                    (r/filter -fuzzy-match?)
                                                    (r/fold -select-best-jw))]
                                     (when (< 0 (::jw match))
                                       (swap! matched-fns conj match)
                                       (swap! unmatched-fns disj (:fns match))
                                       (swap! unmatched-nap disj (:nap match)))))

        match-binned-records (fn match-binned-records [dob {::keys [fns-coll nap-coll]}]
                               (r/foldcat (r/map
                                           (partial compare-fns-to-nap-coll nap-coll)
                                           fns-coll)))

        run-matching-algo (fn [records] (r/foldcat (r/map match-binned-records records)))]

    (run-matching-algo combined-records-by-date)

    (let [fns-by-mra (-bin-by-mra (seq @unmatched-fns))
          nap-by-mra (-bin-by-mra (seq @unmatched-nap))
          combined-records-by-mra (-merge-binned fns-by-mra nap-by-mra)]

      (run-matching-algo combined-records-by-mra))
      
    (vector @matched-fns (seq @unmatched-fns) (seq @unmatched-nap))))

(spec/def
  ::fns-records
  (partial every? (partial instance? webtools.meals_registration.core.FNSRegistration)))

(spec/def
  ::nap-records
  (partial every? (partial instance? webtools.meals_registration.core.NAPRegistration)))

(spec/fdef apply-fuzzy-match-algorithm
           :args (spec/cat :fns ::fns-records
                           :nap ::nap-records)
           :ret vector?)
