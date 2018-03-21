(ns webtools.meals-registration.core
  (:require [clojure.spec.alpha :as s]))

(defrecord FNSRegistration
    [fns-type
     last-name
     first-name
     grade
     dob
     school
     prev-school
     date-registered
     guardian
     gender
     citizenship
     ethnicity
     homeroom
     school-year
     school-no
     uid
     apid])

(defrecord NAPRegistration
    [case-no
     last-name
     first-name
     ethnicity
     dob
     age])

(s/def ::fns-type #{:reduced :free :none})
(s/def ::last-name string?)
(s/def ::first-name string?)
(s/def ::grade int?)
(s/def ::dob :webtools.spec.dates/date)
(s/def ::school string?)
(s/def ::prev-school string?)
(s/def ::date-registered (s/nilable :webtools.spec.dates/date))
(s/def ::guardian string?)
(s/def ::gender #{:male :female})
(s/def ::citizenship string?)
(s/def ::ethnicity string?)
(s/def ::homeroom string?)
(s/def ::school-year string?)
(s/def ::school-no int?)
(s/def ::uid int?)
(s/def ::apid int?)

(s/def ::case-no (s/nilable int?))
(s/def ::age int?)

(s/def ::fns-reg
  (s/keys :req-un [
                   ::last-name
                   ::first-name
                   ::grade
                   ::dob
                   ::school
                   ::prev-school
                   ::guardian
                   ::gender
                   ::citizenship
                   ::ethnicity
                   ::school-year
                   ::school-no
                   ::uid
                   ::apid
                   ]
          :opt-un [
                   ::fns-type
                   ::date-registered
                   ::homeroom
                   ]))


(s/def ::nap-reg
  (s/keys :req-un [
                   ::case-no
                   ::last-name
                   ::first-name
                   ::dob
                   ::age
                   ]
          :opt-un [
                   ]))

(s/fdef ->FNSRegistration
        :ret ::fns-reg)


