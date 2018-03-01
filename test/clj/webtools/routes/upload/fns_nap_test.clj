(ns webtools.routes.upload.fns-nap-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :refer [file]]
            [clojure.spec.alpha :as spec]
            [webtools.routes.upload.fns-nap :as fns-nap]
            [webtools.test.constants :as test-const]
            [webtools.test.fixtures :as fixtures]))

(use-fixtures :once fixtures/instrument)

(deftest test-parsing
  (testing "fns-parse"
    (let [result (fns-nap/fns-parse test-const/valid-fns-file)]
      (testing "should return a map when passed a valid input file"
        (is (map? result)))
      (testing "all values in map should be valid FNSRegistrations"
        (is (empty? (:invalid result)))))

    (let [result (fns-nap/fns-parse test-const/typical-fns-file)]
      (testing "should return a map containing valid and invalid records"
        (is (map? result))
        (is (some? (:valid result)))
        (is (some? (:invalid result))))

      (testing ":valid values in map should be valid FNSRegistrations"
        (is (every? #(spec/valid? :webtools.meals-registration.core/fns-reg %) (:valid result))))

      (testing ":invalid values in map should be seqs of cell values"
        (is (every? seq? (:invalid result))))))

  (testing "nap-parse"
    (let [results (fns-nap/nap-parse test-const/valid-nap-file)]
      (testing "should return a map when passed a valid input file"
        (is (map? results)))
      (testing "all values in map should be valid NAPRegistrations"
        (is (empty? (:invalid results)))))

    (let [results (fns-nap/nap-parse test-const/typical-nap-file)]
      (testing "should return a map containing valid and invalid records"
        (is (map? results))
        (is (some? (:valid results)))
        (is (some? (:invalid results))))

      (testing ":valid values in map should be valid FNSRegistrations"
        (is (every? #(spec/valid? :webtools.meals-registration.core/nap-reg %) (:valid results))))

      (testing ":invalid values in map should be seqs of cell values"
        (is (every? seq? (:invalid results)))))))
