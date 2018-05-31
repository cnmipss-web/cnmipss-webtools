(ns webtools.error-handler.sql
  (:require [clojure.string :as cstr]
            [webtools.constants.exceptions :as cex]))

;;Regexes to identify source of sql errors
(def date_null #"null value in column \"(open_date|close_date)\" violates not-null constraint")
(def bad-column #"ERROR:([\w\s]+)in column \"(\w+)\" violates (.+)")

(defn- parse-problem [message]
  (drop 1 (first (re-seq bad-column message))))

(defn- dispatch-fn [error]
  (let [message (.getMessage error)
        [problem column rule] (parse-problem message)]
    (cond
      (re-seq date_null message) :malformed-date-error
      (if (some? problem)
        (= "null value" (cstr/trim problem))) :null-sql-value-error
      "" :default)))

(defmulti code dispatch-fn)
(defmulti msg dispatch-fn)

;; Implementations for :malformed-date-error
(defmethod code :malformed-date-error [error] cex/bad-sql-date-code)
(defmethod msg :malformed-date-error [error] cex/bad-sql-date-msg)

;; Implementations for :null-sql-value-error
(defn- null-sql-value-msg [column]
  (str "Could not find " column " value.  Upload canceled.  Please make sure that " column " value is properly formatted."))

(defmethod code :null-sql-value-error [error] cex/null-sql-value-code)
(defmethod msg :null-sql-value-error [error]
  (let [column (second (parse-problem (.getMessage error)))]
    (null-sql-value-msg column)))

;; Implementations for :default
(defmethod code :default [error] cex/unknown-sql-code)
(defmethod msg :default [error] cex/unknown-sql-msg)

