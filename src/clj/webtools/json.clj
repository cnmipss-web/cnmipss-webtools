(ns webtools.json
  (:require [cheshire.core :as json]
            [cheshire.generate :refer [add-encoder]]
            [clojure.walk :as walk]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [webtools.util.dates :as util-dates]
            ))


;; Create custom JSON encoders that we will use
(add-encoder org.joda.time.DateTime
             (fn [date jg]
               (if (and (zero? (t/hour date))
                        (zero? (t/minute date))
                        (zero? (t/second date)))
                 (.writeString jg (util-dates/print-date date))
                 (.writeString jg (util-dates/print-date-at-time date)))))

(add-encoder java.lang.Class
             (fn [class jg]
               (.writeString jg (.toString class))))

                                        ;(add-encoder compojure.core.)


(defn json->edn [string]   (walk/keywordize-keys (json/parse-string string)))

(def edn->json
  (comp json/generate-string))

