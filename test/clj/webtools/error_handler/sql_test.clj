(ns webtools.error-handler.sql-test
  (:require [clojure.test :refer :all]
            [clojure.string :as cstr]
            [webtools.constants.exceptions :as cex]
            [webtools.test.fixtures :as fixtures]
            [webtools.error-handler.sql :as eh]))

(use-fixtures :once fixtures/instrument)

(deftest test-malformed-date-error
  (let [error (java.sql.BatchUpdateException.
               (Exception. "ERROR: null value in column \"open_date\" violates not-null constraint"))
        code (eh/code error)
        msg (eh/msg error)]
    (is (= cex/bad-sql-date-code code))
    (is (= cex/bad-sql-date-msg msg)))

  (let [error (java.sql.BatchUpdateException.
               (Exception. "ERROR: null value in column \"close_date\" violates not-null constraint"))
        code (eh/code error)
        msg (eh/msg error)]
    (is (= cex/bad-sql-date-code code))
    (is (= cex/bad-sql-date-msg msg))))

(deftest test-null-sql-value-error
  (let [error (java.sql.BatchUpdateException.
               (Exception. "ERROR: null value in column \"salary\" violates not-null constraint"))
        code (eh/code error)
        msg (eh/msg error)]
    (is (= cex/null-sql-value-code code))
    (is (= "Could not find salary value.  Upload canceled.  Please make sure that salary value is properly formatted." msg)))

  (let [error (java.sql.BatchUpdateException.
               (Exception. "ERROR: null value in column \"some_column\" violates not-null constraint"))
        code (eh/code error)
        msg (eh/msg error)]
    (is (= cex/null-sql-value-code code))
    (is (= "Could not find some_column value.  Upload canceled.  Please make sure that some_column value is properly formatted." msg))))

(deftest test-default-error
  (let [error (Exception. "default error")
        code (eh/code error)
        msg (eh/msg error)]
    (is (= cex/unknown-sql-code code))
    (is (= cex/unknown-sql-msg msg))))
