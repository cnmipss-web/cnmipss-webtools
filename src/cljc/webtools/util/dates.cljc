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
  (parse-date [date] "Read date from formatted string")
  (parse-datetime [date] "Read date-time from formatted string")
  (parse-date-at-time [date] "Read date-time from formatted string w/ word 'at' between date and time")
  (parse-nap-date [date] "Read date from NAP registration records "))

;;These specs cause cryptic errors in clojurescript
#?(:clj
   (s/fdef parse-date
           :args (s/cat :date (s/alt :string :webtools.spec.dates/date-str
                                     :date-time :webtools.spec.dates/date
                                     :nil :webtools.spec.core/nil))
           :ret :webtools.spec.dates/date))
   
#?(:clj
   (s/fdef parse-datetime
           :args (s/cat :date-time (s/alt :string :webtools.spec.dates/date-time-str
                                          :date-time :webtools.spec.dates/date
                                          :nil :webtools.spec.core/nil))
           :ret :webtools.spec.dates/date))
   
#?(:clj
   (s/fdef parse-date-at-time
           :args (s/cat :date-at-time (s/alt :string :webtools.spec.dates/date-at-time-str
                                             :date-time :webtools.spec.dates/date
                                             :nil :webtools.spec.core/nil))
           :ret :webtools.spec.dates/date))

#?(:clj
   (extend-protocol parse-dates
     java.lang.String
     (parse-date [date] (format/parse const/date-formatter date))
     (parse-datetime [date] (format/parse const/date-time-formatter date))
     (parse-date-at-time [date] (format/parse const/date-at-time-formatter date))
     (parse-nap-date [date] (format/parse const/nap-date-formatter date))
     
     org.joda.time.DateTime
     (parse-date [date] date)
     (parse-datetime [date] date)
     (parse-date-at-time [date] date)
     (parse-nap-date [date] date)

     nil
     (parse-date [date] nil)
     (parse-datetime [date] nil)
     (parse-date-at-time [date] nil)
     (parse-nap-date [date] nil))

   :cljs
   (extend-protocol parse-dates
     string
     (parse-date [date] (format/parse const/date-formatter date))
     (parse-datetime [date] (format/parse const/date-time-formatter date))
     (parse-date-at-time [date]
       (-> (clojure.string/replace date #"at" "")  ;; Workaround for bug in cljs-time handling of 'at'
           (clojure.string/replace #"\s+" " ")
           (parse-datetime)))
     
     function
     (parse-date [date] date)
     (parse-datetime [date] date)
     (parse-date-at-time [date] date)

     object
     (parse-date [date] date)
     (parse-datetime [date] date)
     (parse-date-at-time [date] date)

     nil
     (parse-date [date] nil)
     (parse-datetime [date] nil)
     (parse-date-at-time [date] nil)))


(defn print-date [date] (format/unparse const/date-formatter date))

(s/fdef print-date
        :args (s/cat :date :webtools.spec.dates/date)
        :ret  (s/cat :date-str :webtools.spec.dates/date-str))

(defn print-date-at-time [date] (format/unparse const/date-at-time-formatter date))

(s/fdef print-date
        :args (s/cat :date :webtools.spec.dates/date)
        :ret  (s/cat :date-at-time-str :webtools.spec.dates/date-at-time-str))
