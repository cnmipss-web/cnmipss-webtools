(ns webtools.spec.procurement-addendum
  (:require [clojure.spec.alpha :as spec]))

(spec/def ::id             (spec/or :uuid :webtools.spec.core/uuid
                                    :uuid-str :webtools.spec.core/uuid-str))
(spec/def ::proc_id ::id)
(spec/def ::addendum_number  int?)
(spec/def ::file_link      string?)

(spec/def ::record
  (spec/keys :req-un [::id
                      ::proc_id
                      ::addendum_number
                      ::file_link]))

