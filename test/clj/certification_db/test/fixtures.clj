(ns certification-db.test.fixtures
  (:require [certification-db.config :refer [env]]
            [certification-db.db.core :as db]
            [conman.core :refer [bind-connection] :as conman]
            [clojure.java.jdbc :as sql]
            [mount.core :as mount]))

(defn prep-db
  [ts]
  (mount/start)
  (db/create-tables)
  (db/seed-users)
  (db/seed-certs)
  (db/seed-jvas)
  (ts)
  (mount/stop))

(defn with-rollback
  [t]
  (sql/with-db-transaction [db db/*db*]
    (sql/db-set-rollback-only! db)
    (binding [db/*db* db]
      (t))))


