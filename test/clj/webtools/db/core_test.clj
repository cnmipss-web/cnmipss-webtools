(ns webtools.db.core-test
  (:require [webtools.config :refer [env]]
            [webtools.db.core :as db]
            [webtools.test.constants :as c-t]
            [webtools.test.fixtures :as fixtures]
            [clojure.test :refer :all]
            [conman.core :refer [bind-connection] :as conman]
            [clojure.java.jdbc :as sql]
            [mount.core :as mount]))

(deftest test-initial-seed
  (testing "database seeded correctly"
    (is (= c-t/user-seed-count (count (db/get-all-users))))
    (is (= c-t/cert-seed-count (count (db/get-all-certs))))
    (is (= c-t/jva-seed-count (count (db/get-all-jvas))))
    (is (= c-t/rfp-seed-count (count (db/get-all-rfps))))
    (is (= c-t/ifb-seed-count (count (db/get-all-ifbs)))))
  (testing "delete functions" 
    (db/delete-user! {:email "tony.stark@cnmipss.org"})
    (db/delete-cert! {:cert_no "BI-003-2006"})
    (db/delete-jva! {:announce_no "PSS-2015-311"})
    (db/delete-rfp! {:rfp_no "15-010"})
    (db/delete-ifb! {:ifb_no "15-007"})
    (is (= (- c-t/user-seed-count 1)
           (count (db/get-all-users))))
    (is (= (- c-t/cert-seed-count 1) 
           (count (db/get-all-certs))))
    (is (= (- c-t/jva-seed-count 1)
           (count (db/get-all-jvas))))
    (is (= (- c-t/rfp-seed-count 1)
           (count (db/get-all-rfps))))
    (is (= (- c-t/ifb-seed-count 1)
           (count (db/get-all-ifbs))))))

(deftest test-final-seed
  (testing "database rolled back correctly"
    (is (= c-t/user-seed-count (count (db/get-all-users))))
    (is (= c-t/cert-seed-count (count (db/get-all-certs))))
    (is (= c-t/jva-seed-count (count (db/get-all-jvas))))
    (is (= c-t/rfp-seed-count (count (db/get-all-rfps))))
    (is (= c-t/ifb-seed-count (count (db/get-all-ifbs)))))
  (testing "create functions"
    (is (= 1 (db/create-user! c-t/dummy-user)))
    (is (= 1 (db/create-jva! c-t/dummy-jva)))
    (is (= 1 (db/create-cert! c-t/dummy-cert)))
    (is (= 1 (db/create-rfp! c-t/dummy-rfp)))
    (is (= 1 (db/create-ifb! c-t/dummy-ifb)))
    (is (= (+ c-t/user-seed-count 1)
           (count (db/get-all-users))))
    (is (= (+ c-t/cert-seed-count 1) 
           (count (db/get-all-certs))))
    (is (= (+ c-t/jva-seed-count 1)
           (count (db/get-all-jvas))))
    (is (= (+ c-t/rfp-seed-count 1)
           (count (db/get-all-rfps))))
    (is (= (+ c-t/ifb-seed-count 1)
           (count (db/get-all-ifbs))))))

(defn test-ns-hook
  []
  (fixtures/prep-db
   #(mapv
     (comp fixtures/with-rollback)
     [test-initial-seed
      test-final-seed])))
