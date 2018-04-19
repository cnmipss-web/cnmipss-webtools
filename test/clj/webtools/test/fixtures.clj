(ns webtools.test.fixtures
  (:require [clojure.java.jdbc :as sql]
            [clojure.spec.test.alpha :as stest]
            [mount.core :as mount]
            [webtools.db :as db]
            [webtools.spec.certification]
            [webtools.spec.core]
            [webtools.spec.dates]
            [webtools.spec.fns-nap]
            [webtools.spec.internet]
            [webtools.spec.jva]
            [webtools.spec.procurement]
            [webtools.spec.procurement-addendum]
            [webtools.spec.subscription]
            [webtools.spec.user]))

(defn instrument [ts]
  (stest/instrument)
  (ts))

(defn prep-db
  [ts]
  (mount/start)
  (db/clear-all-tables!)
  (db/seed-users)
  (db/seed-certs)
  (db/seed-jvas)
  (db/seed-rfps)
  (db/seed-ifbs)
  (db/seed-addenda)
  (db/seed-subscriptions)
  (db/seed-fns-nap)
  (ts)
  (mount/stop))

(defn with-rollback
  [t]
  (sql/with-db-transaction [db db/*db*]
    (sql/db-set-rollback-only! db)
    (binding [db/*db* db]
      (t))))


