(ns webtools.db.core-test
  (:require [webtools.config :refer [env]]
            [webtools.db.core :as db]
            [webtools.procurement.core :refer [make-uuid]]
            [webtools.test.constants :as const]
            [webtools.test.fixtures :as fixtures]
            [clojure.test :refer :all]
            [conman.core :refer [bind-connection] :as conman]
            [clojure.java.jdbc :as sql]
            [mount.core :as mount]))

(deftest test-initial-seed
  (testing "database seeded correctly"
    (is (= const/user-seed-count (count (db/get-all-users))))
    (is (= const/cert-seed-count (count (db/get-all-certs))))
    (is (= const/jva-seed-count (count (db/get-all-jvas))))
    (is (= const/pnsa-seed-count (count (db/get-all-pnsa)))))
  
  (testing "delete functions" 
    (db/delete-user! {:email "tony.stark@cnmipss.org"})
    (db/delete-cert! {:cert_no "BI-003-2006"})
    (db/delete-jva!  {:announce_no "PSS-2015-311"})
    (db/delete-pnsa! {:id (make-uuid "1174a9a8-b45a-422a-bb46-574f814c2550")})
    (db/delete-pnsa! {:id (make-uuid "2fa4e278-f022-4361-b69a-0063a387933a")})
    (is (= (- const/user-seed-count 1)
           (count (db/get-all-users))))
    (is (= (- const/cert-seed-count 1) 
           (count (db/get-all-certs))))
    (is (= (- const/jva-seed-count 1)
           (count (db/get-all-jvas))))
    (is (= (- const/pnsa-seed-count 2)
           (count (db/get-all-pnsa))))))

(deftest test-final-seed
  (testing "database rolled back correctly"
    (is (= const/user-seed-count (count (db/get-all-users))))
    (is (= const/cert-seed-count (count (db/get-all-certs))))
    (is (= const/jva-seed-count (count (db/get-all-jvas))))
    (is (= const/pnsa-seed-count (count (db/get-all-pnsa)))))
  (testing "create functions"
    (is (= 1 (db/create-user! const/dummy-user)))
    (is (= 1 (db/create-jva! const/dummy-jva)))
    (is (= 1 (db/create-cert! const/dummy-cert)))
    (is (= 1 (db/create-pnsa! const/dummy-rfp)))
    (is (= 1 (db/create-pnsa! const/dummy-ifb)))
    (is (= (+ const/user-seed-count 1)
           (count (db/get-all-users))))
    (is (= (+ const/cert-seed-count 1) 
           (count (db/get-all-certs))))
    (is (= (+ const/jva-seed-count 1)
           (count (db/get-all-jvas))))
    (is (= (+ const/pnsa-seed-count 2)
           (count (db/get-all-pnsa))))))

(defn test-ns-hook
  []
  (fixtures/prep-db
   #(mapv
     (comp fixtures/with-rollback)
     [test-initial-seed
      test-final-seed])))
