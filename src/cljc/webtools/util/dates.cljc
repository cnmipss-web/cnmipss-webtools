(ns webtools.util.dates
  (:require [clojure.spec.alpha :as s]
            [webtools.constants :as const]
            [webtools.spec.core]
            #?(:clj  [clj-time.core :as time]
               :cljs [cljs-time.core :as time])
            #?(:clj  [clj-time.coerce :as coerce]
               :cljs [cljs-time.coerce :as coerce])
            #?(:clj  [clj-time.format :as format]
               :cljs [cljs-time.format :as format])))

(defprotocol parse-dates
  (parse-date [date] "Convert date to formatted string")
  (parse-datetime [date] "Convert date-time to formatted string")
  (parse-date-at-time [date] "Convert date-time to formatted string w/ word 'at' between date and time"))


(s/fdef parse-date
        :args (s/cat :date (s/alt :string string?
                                  :date-time (fn [x] (instance?
                                                      #?(:clj org.joda.time.DateTime
                                                         :cljs js/Function) x))))
        :ret :webtools.spec.core/date)

(s/fdef parse-datetime
        :args (s/cat :date-time (s/alt :string string?
                                       :date-time :webtools.spec.core/date))
        :ret :webtools.spec.core/date)

(s/fdef parse-date-at-time
        :args (s/cat :date-at-time (s/or :string string?
                                         :date-time :webtools.spec.core/date))
        :ret :webtools.spec.core/date)

#?(:clj
   (extend-protocol parse-dates
     java.lang.String
     (parse-date [date] (format/parse const/date-formatter date))
     (parse-datetime [date] (format/parse const/date-time-formatter date))
     (parse-date-at-time [date] (format/parse const/date-at-time-formatter date))
     
     org.joda.time.DateTime
     (parse-date [date] date)
     (parse-datetime [date] date)
     (parse-date-at-time [date] date)

     nil
     (parse-date [date] nil)
     (parse-datetime [date] nil)
     (parse-date-at-time [date] nil))

   :cljs
   (extend-protocol parse-dates
     string
     (parse-date [date] (format/parse const/date-formatter date))
     (parse-datetime [date] (format/parse const/date-time-formatter date))
     (parse-date-at-time [date]
       (println "Parsing: " date)
       (-> (clojure.string/replace date #"at" "")  ;; Workaround for bug in cljs-time handling of 'at'
           (clojure.string/replace #"\s+" " ")
           (parse-datetime)))
     
     function
     (parse-date [date] date)
     (parse-datetime [date] date)
     (parse-date-at-time [date] date)))


(defn print-date [date] (format/unparse const/date-formatter date))
(defn print-date-at-time [date] (format/unparse const/date-at-time-formatter date))

;; (s/fdef print-date
;;         :args :webtools.spec.core/date
;;         :ret  string?)

;; (s/fdef print-date-at-time
;;         :args :webtools.spec.core/date
;;         :ret  string?)
