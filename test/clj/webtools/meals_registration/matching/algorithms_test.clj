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

(def ^:private fns-records (-> test-const/typical-fns-file fns-parse :valid))
(def ^:private nap-records (-> test-const/typical-nap-file nap-parse :valid))

(deftest test-match-dob
  (let [[matches unmatched] (malgo/match-dob fns-records nap-records)]
    ;; (println "Matched: " (count matches) "Unmatched: " (count unmatched))
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
