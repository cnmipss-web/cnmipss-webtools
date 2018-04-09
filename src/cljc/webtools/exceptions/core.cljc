(ns webtools.exceptions.core)

(defprotocol generate-ex-info
  (wrap-ex [ex data] "Wrap a Java or JS runtime Exception or Error with clojure.lang.ExceptionInfo"))

(defprotocol create-ex-info
  (null-pointer     [opts]
    "Wrap a Java NullPointer Exception or JS ReferenceError with ex-info. Accepts a map with the following keys:

    :msg   -- A message describing the source of the exception
    :data  -- A map containing data relevant to debugging the exception
    :cause -- A throwable object which is being wrapped by ex-info.

At least one of :msg or :cause must be some?")
  
  (illegal-argument [opts]
    "Wrap a Java IllegalArgument Exception or JS TypeError with ex-info. Accepts a map with the following keys:

    :msg   -- A message describing the source of the exception
    :data  -- A map containing data relevant to debugging the exception
    :cause -- A throwable object which is being wrapped by ex-info.

At least one of :msg or :cause must be some?")

  (sql-duplicate-key [opts]
    "Wrap a Java java.sql.BatchUpdateException with ex-info.  Accepts a map with the following keys:

    :msg   -- A message describing the source of the exception
    :data  -- A map containing data relevant to debugging the exception
    :cause -- A throwable object which is being wrapped by ex-info

At least one of :msg or :cause must be some?"))
