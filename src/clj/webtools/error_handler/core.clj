(ns webtools.error-handler.core
  (:require [webtools.constants.exceptions :as cex]
            [webtools.error-handler.ex-info :as info-error]
            [webtools.error-handler.sql :as sql-error]))

(defprotocol HandleErrors
  (code [x] "Return an error-code based on the type of error in terms of our data model.")
  (msg [x] "A human friendly error message based on the type of error in terms of our data model"))

(extend-protocol HandleErrors
  java.sql.BatchUpdateException
  (code [err]
    (sql-error/code err))
  (msg [err]
    (sql-error/msg err))

  clojure.lang.ExceptionInfo
  (code [err]
    (info-error/code err))
  (msg  [err]
    (info-error/msg err))
  
  Exception
  (code [err] cex/unknown-error-code)
  (msg [err] cex/unknown-error-msg))
