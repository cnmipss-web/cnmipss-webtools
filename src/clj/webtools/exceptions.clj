(ns webtools.exceptions
  (:require [clojure.pprint :refer [pprint]]
            [webtools.exceptions.core :as ex]
            [webtools.util :refer [pull]]))

;; Refer fns from webtools.expcetions.core through this ns
(pull webtools.exceptions.core
  [wrap-ex
   null-pointer illegal-argument sql-duplicate-key])

(defn exception->string [exception]
  "Pretty print exception data to a string and return that string."
  (let [sw (java.io.StringWriter.)
        pw (java.io.PrintWriter. sw)]
    (pprint exception pw)
    (.toString sw)))


;; Implementations of protocols defined in webtools.exceptions.core
(defn- -implement-error [error-type {:keys [data cause] :as opts}]
  (let [msg (or (:msg opts)
                (if (some? cause) (.getMessage cause)))
        default-data {:msg msg 
                      :type error-type}]
    (ex-info msg (merge default-data data) cause)))


(extend-protocol ex/generate-ex-info
  Exception
  (ex/wrap-ex [ex data]
    (ex-info (.getMessage ex)
             (merge {:type (type ex)
                     :msg (.getMessage ex)} data)
             ex)))

(extend-protocol ex/create-ex-info
  clojure.lang.PersistentHashMap
  (ex/sql-duplicate-key [opts] (-implement-error java.sql.BatchUpdateException opts))
  (ex/null-pointer      [opts] (-implement-error NullPointerException opts))
  (ex/illegal-argument  [opts] (-implement-error IllegalArgumentException opts))

  clojure.lang.PersistentArrayMap
  (ex/sql-duplicate-key [opts] (-implement-error java.sql.BatchUpdateException opts))
  (ex/null-pointer      [opts] (-implement-error NullPointerException opts))
  (ex/illegal-argument  [opts] (-implement-error IllegalArgumentException opts)))

