(ns webtools.routes.upload.fns-nap-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :refer [file]]
            [webtools.routes.upload.fns-nap :as fns-nap]
            [webtools.test.constants :as test-const]
            [webtools.test.fixtures :as fixtures]))

(use-fixtures :once fixtures/instrument)

(def ^:private valid-fns-file (file "test/clj/webtools/test/valid-fns.xlsx"))
(def ^:private valid-nap-file (file "test/clj/webtools/test/valid-nap.xlsx"))

(deftest test-parsing
  (testing "fns-parse"
    (let [result (fns-nap/fns-parse valid-fns-file)]
      (testing "should return a seq when passed a valid input file"
        (is (seq? result)))
      (testing "all values in seq should be valid FNSRegistrations"
        (let [invalid-registrations
              (filter (fn [fns-reg]
                        (not (clojure.spec.alpha/valid?
                              :webtools.meals-registration.core/fns-reg fns-reg))) result)]
          (is (empty? invalid-registrations))))))

  (testing "nap-parse"
    (let [result (fns-nap/nap-parse valid-nap-file)]
      (testing "should return a seq when passed a valid input file"
        (is (seq? result)))
      (testing "all values in seq should be valid NAPRegistrations"
        (let [invalid-registrations
              (filter (fn [nap-reg]
                        (not (clojure.spec.alpha/valid?
                              :webtools.meals-registration.core/nap-reg nap-reg))) result)]
          (is (empty? invalid-registrations)))))))
