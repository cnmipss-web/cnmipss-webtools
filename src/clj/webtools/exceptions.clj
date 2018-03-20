(ns webtools.exceptions
  (:require [clojure.pprint :refer [pprint]])
  (:import [java.io StringWriter PrintWriter]))

(defn exception->string [exception]
  "Pretty print exception data to a string and return that string."
  (let [sw (java.io.StringWriter.)
        pw (java.io.PrintWriter. sw)]
    (pprint exception pw)
    (.toString sw)))

(defprotocol generate-ex-info
  (wrap-ex [ex data] "Wrap a Java runtime Exception with clojure.lang.ExceptionInfo"))

(defn null-pointer
  "Generate a custom null-pointer exception using ex-info.  Accepts a map
  with the following keys:

    :msg   -- A message describing the source of the exception
    :data  -- A map containing data relevant to debugging the exception
    :cause -- A throwable object which is being wrapped by ex-info.

  At least one of :msg or :cause must be some?"
  [{:keys [msg data cause]}]
  (let [default-data {:msg (if (some? cause) (.getMessage cause))
                      :type NullPointerException}]
    (ex-info msg (merge default-data data) cause)))

(defn illegal-argument
  "Generate a custom illegal-argument exception using ex-info.  Accepts a map
  with the following keys:

    :msg   -- A message describing the source of the exception
    :data  -- A map containing data relevant to debugging the exception
    :cause -- A throwable object which is being wrapped by ex-info.

  At least one of :msg or :cause must be some?"
  [{:keys [msg data cause]}]
  (let [default-data {:msg (if (some? cause) (.getMessage cause))
                      :type IllegalArgumentException}]
    (ex-info msg (merge default-data data) cause)))

(extend-protocol generate-ex-info
  IllegalArgumentException
  (wrap-ex [ex data]
    (illegal-argument (merge data {:cause ex})))
  
  NullPointerException
  (wrap-ex [ex data]
    (null-pointer (merge data {:cause ex}))))
