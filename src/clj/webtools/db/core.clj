(ns webtools.db.core
  (:require [cheshire.core :refer [generate-string parse-string]]
            [cheshire.generate :refer [add-encoder]]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as cstr]
            [clojure.tools.logging :as log]
            [conman.core :as conman]
            [mount.core :refer [defstate]]
            [webtools.config :refer [env]]
            [webtools.constants :as const]
            [webtools.util.dates :as util-dates])
  (:import (clojure.lang IPersistentMap IPersistentVector)
           (java.sql Array)
           (org.postgresql.util PGobject)))

(defstate ^:dynamic *db*
  :start (conman/connect! {:jdbc-url (str (env :database-url)
                                          (env :database-sslmode))})
  :stop (conman/disconnect! *db*))

(conman/bind-connection *db*
                        "sql/test-seed.sql"
                        "sql/procurement-queries.sql"
                        "sql/procurement-addenda-queries.sql"
                        "sql/subscription-queries.sql"
                        "sql/jva-queries.sql"
                        "sql/user-queries.sql" 
                        "sql/cert-queries.sql"
                        "sql/fns-nap-queries.sql")

(extend-protocol jdbc/IResultSetReadColumn
  java.sql.Date
  (result-set-read-column [v _ _] (c/from-sql-date v))

  java.sql.Timestamp
  (result-set-read-column [v _ _] (c/from-sql-time v))
  
  Array
  (result-set-read-column [v _ _] (vec (.getArray v)))

  PGobject
  (result-set-read-column [pgobj _metadata _index]
    (let [type  (.getType pgobj) 
          value (.getValue pgobj)]
      (case type
        "json" (parse-string value true)
        "jsonb" (parse-string value true)
        "citext" (str value) 
        value))))

(defn to-pg-json [value]
      (doto (PGobject.)
            (.setType "jsonb")
            (.setValue (generate-string value))))

(extend-type clojure.lang.IPersistentVector 
  jdbc/ISQLParameter
  (set-parameter [v ^java.sql.PreparedStatement stmt ^long idx]
    (let [conn      (.getConnection stmt)
          meta      (.getParameterMetaData stmt)
          type-name (.getParameterTypeName meta idx)]
      (if-let [elem-type (when (= (first type-name) \_) (cstr/join (rest type-name)))]
        (.setObject stmt idx (.createArrayOf conn elem-type (to-array v)))
        (.setObject stmt idx (to-pg-json v))))))

(add-encoder org.joda.time.DateTime
             (fn [date jg]
               (if (and (zero? (t/hour date))
                        (zero? (t/minute date))
                        (zero? (t/second date)))
                 (.writeString jg (util-dates/print-date date))
                 (.writeString jg (util-dates/print-date-at-time date)))))

(extend-protocol jdbc/ISQLValue
  clojure.lang.Keyword
  (sql-value [value] (name value))
  IPersistentMap
  (sql-value [value] (to-pg-json value))
  IPersistentVector
  (sql-value [value] (to-pg-json value))
  org.joda.time.DateTime
  (sql-value [value] (c/to-sql-time value)))

(defn make-sql-date
  [m k]
  (if (= java.lang.String (type (get m k)))
    (assoc m k (try
                 (->>
                  (get m k)
                  (util-dates/parse-date)
                  (c/to-sql-date))
                 (catch Exception e
                   (log/error e)
                   nil)))
    m))

(defn make-sql-datetime
  [m k]
  (if (= java.lang.String (type (get m k)))
    (assoc m k (try
                 (->>
                  (get m k)
                  (re-find const/date-at-time-re)
                  (second)
                  ((fn [date-at-time]
                     (-> date-at-time util-dates/parse-date-at-time c/to-sql-time))))
                 (catch Exception e
                   (log/error e)
                   nil)))
    m))


