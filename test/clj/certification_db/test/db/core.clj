(ns certification-db.test.db.core
  (:require [certification-db.config :refer [env]]
            [certification-db.db.core :as db]
            [clojure.test :refer :all]
            [hugsql.core :as hugsql]
            [conman.core :refer [bind-connection]]
            [clojure.java.jdbc :as sql]))

(bind-connection db/*db* "sql/test-seed.sql")

(use-fixtures :once
  (fn [ts]
    (create-tables)
    (seed-users)
    (seed-certs)
    (seed-jvas)
    (ts)))

(use-fixtures :each
  (fn [t]
    (sql/with-db-transaction [db db/*db*]
      (sql/db-set-rollback-only! db)
      (binding [db/*db* db]
        (t)))))

(deftest test-seed
  (testing "Is this real life?"
    (is (= 1 1)))
  (testing "Or is this just fantasy?"
    (is (= 5 (count (db/get-all-users))))
    (db/delete-user! {:email "tony.stark@cnmipss.org"})
    (is (= 4 (count (db/get-all-users))))))
