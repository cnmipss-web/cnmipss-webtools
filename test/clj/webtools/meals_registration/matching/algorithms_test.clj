(ns webtools.meals-registration.matching.algorithms-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :refer [file]]
            [clj-time.core :as time]
            [webtools.meals-registration.matching.algorithms :as malgo]
            [webtools.routes.upload.fns-nap :refer [fns-parse nap-parse]]
            [webtools.test.fixtures :as fixtures]
            [webtools.test.constants :as test-const])
  (:import [webtools.meals_registration.core FNSRegistration NAPRegistration]))

(use-fixtures :once fixtures/instrument)

(def ^:private fns-records (-> test-const/full-fns-file fns-parse :valid))
(def ^:private nap-records (-> test-const/full-nap-file nap-parse :valid))

(deftest test-dob-match
  (println "\n------------------------------------------------------------\n\n")
  (println "testing w.m.m.a/dob-match: ")
  (let [[matches unmatched] (time (malgo/dob-match fns-records nap-records))]
    (println "Matched: " (count matches) "Unmatched: " (count unmatched))
    (testing "should return vector containing two seqable elements"
      (is (seqable? matches))
      (is (seqable? unmatched)))

    (testing "all matches should contain a fns-nap pair with matching :dob values"
      (let [bad-matches (filter (fn [{:keys [fns nap]}]
                                  (not (and (instance? FNSRegistration fns)
                                            (instance? NAPRegistration nap)
                                            (time/equal? (:dob fns) (:dob nap)))))
                                matches)]
        (is (empty? bad-matches))))

    (testing "all unmatched values are instances of FNSRegistration"
      (is (every? (partial instance? FNSRegistration) unmatched)))))

(deftest test-fuzzy-match
  (println "\n------------------------------------------------------------\n\n")
  (println "testing w.m.m.a/jw-match-names: ")
  (let [[matches um-fns um-nap] (time (malgo/fuzzy-match fns-records nap-records))]
    (println "Matched: " (count matches)
             "Unmatched FNS: " (count um-fns)
             "Unmatched NAP: " (count um-nap))
    (testing "should return vector containing two seqable elements"
      (is (seqable? matches))
      (is (seqable? um-fns))
      (is (seqable? um-nap)))

    (testing "all matches should contain a fns-nap pair with matching :dob values"
      (let [bad-matches (filter (fn [{:keys [fns nap]}]
                                  (not (and (instance? FNSRegistration fns)
                                            (instance? NAPRegistration nap))))
                                matches)]
        (is (empty? bad-matches))))

    (testing "all unmatched fns values are instances of FNSRegistration"
      (is (every? (partial instance? FNSRegistration) um-fns)))

    (testing "all unmatched nap values are instances of NAPRegistration"
      (is (every? (partial instance? NAPRegistration) um-nap)))))
