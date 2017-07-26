(ns certification-db.db.core-test
  (:require [certification-db.config :refer [env]]
            [certification-db.db.core :as db]
            [certification-db.constants-test :as c-t]
            [certification-db.test.fixtures :as fixtures]
            [clojure.test :refer :all]
            [conman.core :refer [bind-connection] :as conman]
            [clojure.java.jdbc :as sql]
            [mount.core :as mount]))

(deftest test-initial-seed
  (testing "database seeded correctly"
    (is (= c-t/user-seed-count (count (db/get-all-users))))
    (is (= c-t/cert-seed-count (count (db/get-all-certs))))
    (is (= c-t/jva-seed-count (count (db/get-all-jvas)))))
  (testing "delete functions" 
    (db/delete-user! {:email "tony.stark@cnmipss.org"})
    (db/delete-cert! {:cert_no "BI-003-2006"})
    (db/delete-jva! {:announce_no "PSS-2015-311"})
    (is (= (- c-t/user-seed-count 1)
           (count (db/get-all-users))))
    (is (= (- c-t/cert-seed-count 1) 
           (count (db/get-all-certs))))
    (is (= (- c-t/jva-seed-count 1)
           (count (db/get-all-jvas))))))

(deftest test-final-seed
  (testing "database rolled back correctly"
    (is (= 5 (count (db/get-all-users))))
    (is (= 4 (count (db/get-all-certs)))) 
    (is (= 3 (count (db/get-all-jvas)))))
  (testing "create functions"
    (is (= 1 (db/create-user! c-t/dummy-user)))
    (is (= 1 (db/create-jva! c-t/dummy-jva)))
    (is (= 1 (db/create-cert! c-t/dummy-cert)))
    (is (= 6 (count (db/get-all-users))))
    (is (= 5 (count (db/get-all-certs))))
    (is (= 4 (count (db/get-all-jvas))))))

(defn test-ns-hook
  []
  (fixtures/prep-db
   #(mapv
     (comp fixtures/with-rollback)
     [test-initial-seed
      test-final-seed])))
