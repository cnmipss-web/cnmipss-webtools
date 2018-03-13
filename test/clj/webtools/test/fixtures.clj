(ns webtools.test.fixtures
  (:require [webtools.config :refer [env]]
            [webtools.db.core :as db]
            [conman.core :refer [bind-connection] :as conman]
            [clojure.java.jdbc :as sql]
            [mount.core :as mount]
            [clojure.spec.test.alpha :as stest]))

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


