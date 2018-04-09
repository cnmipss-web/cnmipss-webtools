(ns webtools.exceptions
  (:require [webtools.exceptions.core :as ex]
            [webtools.util :refer [pull]]))

;; Refer fns from webtools.expcetions.core through this ns
(pull webtools.exceptions.core
      [wrap-ex
       null-pointer illegal-argument])

;; (defn exception->string [exception]
;;   "Pretty print exception data to a string and return that string."
;;   (let [sw (java.io.StringWriter.)
;;         pw (java.io.PrintWriter. sw)]
;;     (pprint exception pw)
;;     (.toString sw)))


;; Implementations of protocols defined in webtools.exceptions.core
(defn- -implement-error [error-type {:keys [data cause] :as opts}]
  (let [msg (or (:msg opts)
                (if (some? cause) (.-message cause)))
        default-data {:msg msg 
                      :type error-type}]
    (ex-info msg (merge default-data data) cause)))

(extend-protocol ex/generate-ex-info
  js/Error
  (ex/wrap-ex [ex data]
    (ex-info (.-message ex)
             (merge {:type (type ex)
                     :msg (.-message ex)} data)
             ex)))

(extend-protocol ex/create-ex-info
  cljs.core.PersistentHashMap
  (ex/null-pointer     [opts] (-implement-error js/ReferenceError opts))
  (ex/illegal-argument [opts] (-implement-error js/TypeError opts))

  cljs.core.PersistentArrayMap
  (ex/null-pointer     [opts] (-implement-error js/ReferenceError opts))
  (ex/illegal-argument [opts] (-implement-error js/TypeError opts)))
