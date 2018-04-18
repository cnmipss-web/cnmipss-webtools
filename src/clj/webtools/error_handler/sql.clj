(ns webtools.error-handler.sql
  (:require [webtools.constants.exceptions :as cex]))

;;Regexes to identify source of sql errors
(def open_date_null #"null value in column \"open_date\" violates not-null constraint")

(defn- dispatch-fn [error]
  (let [message (.getMessage error)]
    (cond
      (re-seq open_date_null message) :malformed-date-error
      "" :default)))

(defmulti code dispatch-fn)
(defmulti msg dispatch-fn)

;; Implementations for :malformed-date-error
(defmethod code :malformed-date-error [error] cex/bad-sql-date-code)
(defmethod msg :malformed-date-error [error] cex/bad-sql-date-msg)

;; Implementations for :default
(defmethod code :default [error] cex/unknown-sql-code)
(defmethod msg :default [error] cex/unknown-sql-msg)

