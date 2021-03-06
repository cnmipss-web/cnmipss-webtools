(ns webtools.spec.procurement
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [webtools.spec.core]))

(s/def ::id             :webtools.spec.core/uuid)
(s/def ::type           #{:rfp :ifb "rfp" "ifb"})
(s/def ::number         string?)
(s/def ::open_date      (s/or :date :webtools.spec.dates/date
                                            :date-str :webtools.spec.dates/date-str
                                            :nil :webtools.spec.core/nil))
(s/def ::close_date     (s/or :date :webtools.spec.dates/date
                              :date-time-str :webtools.spec.dates/date-at-time-str
                              :nil :webtools.spec.core/nil))
(s/def ::title          string?)
(s/def ::description    string?)
(s/def ::file_link      string?)
(s/def ::spec_link      string?)

(s/def ::record
  (s/keys :req-un [::id
                   ::type
                   ::number
                   ::open_date
                   ::close_date
                   ::title
                   ::description
                   ::file_link
                   ::spec_link]))

