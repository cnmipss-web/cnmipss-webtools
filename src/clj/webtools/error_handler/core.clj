(ns webtools.error-handler.core
  (:require [webtools.error-handler.sql :as sql-error]))

(defprotocol HandleErrors
  (code [x] "Return an error-code based on the type of error in terms of our data model.")
  (msg [x] "A human friendly error message based on the type of error in terms of our data model"))

(extend-protocol HandleErrors
  java.sql.BatchUpdateException
  (code [err]
    (sql-error/code err))
  (msg [err]
    (sql-error/msg err))
  
  Exception
  (code [err] "unknown-err")
  (msg [err] "An unknown error occurred.  Please contact the developer.")
  )
