(ns webtools.error-handler.sql)


;;Regexes to identify source of sql errors
(def open_date_null #"null value in column \"open_date\" violates not-null constraint")


(defprotocol ReportError
  (code [x] "Return an error-code based on the type of error in terms of our data model.")
  (msg [x]  "A human friendly error message based on the type of error in terms of our data model"))

(defrecord malformed-date [error]
  ReportError
  (code [x] "bad-date")
  (msg [x]  "One of the required dates is incorrectly formatted."))

(defrecord unknown-error [error]
  ReportError
  (code [x] "unknown")
  (msg  [x] "Unknown error.  Please contact the developer."))

(defn type
  "Determine the type of error in terms of our data model"
  [error]
  (let [message (.getMessage error)]
    (cond
      (re-seq open_date_null message) (->malformed-date error)
      "" (->unknown-error error))))
