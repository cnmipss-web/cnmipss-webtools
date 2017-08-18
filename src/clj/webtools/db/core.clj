(ns webtools.db.core
  (:require
   [clojure.data.json :as json]
   [cheshire.core :refer [generate-string parse-string]]
   [cheshire.generate :refer [add-encoder encode-str encode-date]]
   [clj-time.jdbc]
   [clj-time.format :as f]
   [clj-time.coerce :as c]
   [clj-time.core :as t]
   [clj-time.local :as l]
   [clojure.java.jdbc :as jdbc]
   [conman.core :as conman]
   [webtools.config :refer [env]]
   [webtools.constants :as const]
   [mount.core :refer [defstate]])
  (:import org.postgresql.util.PGobject
           java.sql.Array
           clojure.lang.IPersistentMap
           clojure.lang.IPersistentVector
           [java.sql
            BatchUpdateException
            PreparedStatement]))

(defstate ^:dynamic *db*
  :start (conman/connect! {:jdbc-url (str (env :database-url)
                                          (env :database-sslmode))})
  :stop (conman/disconnect! *db*))

(conman/bind-connection *db*
                        "sql/test-seed.sql"
                        "sql/procurement-addenda-queries.sql"
                        "sql/subscription-queries.sql"
                        "sql/ifb-queries.sql"
                        "sql/rfp-queries.sql"
                        "sql/jva-queries.sql"
                        "sql/user-queries.sql" 
                        "sql/cert-queries.sql")

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
      (if-let [elem-type (when (= (first type-name) \_) (apply str (rest type-name)))]
        (.setObject stmt idx (.createArrayOf conn elem-type (to-array v)))
        (.setObject stmt idx (to-pg-json v))))))

(add-encoder org.joda.time.DateTime
             (fn [date jg]
               (if (and (= (t/hour date) 0)
                        (= (t/minute date) 0)
                        (= (t/second date) 0))
                 (.writeString jg (f/unparse (f/formatter "MMMM dd, YYYY") date))
                 (.writeString jg (f/unparse (f/formatter const/procurement-datetime-format) date)))))

(extend-protocol jdbc/ISQLValue
  IPersistentMap
  (sql-value [value] (to-pg-json value))
  IPersistentVector
  (sql-value [value] (to-pg-json value)))

(defn make-sql-date
  [m k]
  (if (= java.lang.String (type (get m k)))
    (assoc m k (try (->> (get m k)
                         (f/parse (f/formatter "MMMM dd, YYYY"))
                         (c/to-sql-date))
                    (catch Exception e
                      nil)))
    m))

(defn make-sql-datetime
  [m k]
  (if (= java.lang.String (type (get m k)))
    (assoc m k (try (->> (get m k)
                         (re-find const/procurement-datetime-re)
                         (second)
                         ((fn [date-time]
                            (let [f-date-time (partial f/parse (f/formatter const/procurement-datetime-format))]
                              (-> date-time f-date-time c/to-sql-time)))))
                    (catch Exception e
                      (println e))))
    m))


