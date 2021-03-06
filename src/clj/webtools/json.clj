(ns webtools.json
  (:require [cheshire.core :as json]
            [cheshire.generate :refer [add-encoder]]
            [clj-time.core :as t]
            [clojure.walk :as walk]
            [webtools.util.dates :as util-dates]))


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
               (.writeString jg (str class))))

(defn json->data [string]   (walk/keywordize-keys (json/parse-string string)))

(defn data->json [data] (json/generate-string data))

