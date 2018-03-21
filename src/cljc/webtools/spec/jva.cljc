(ns webtools.spec.jva
  (:require [clojure.spec.alpha :as spec]))

(spec/def ::file_link   string?)
(spec/def ::announce_no string?)
(spec/def ::position    string?)
(spec/def ::open_date   string?)
(spec/def ::location    string?)
(spec/def ::close_date  (spec/nilable string?))

(spec/def ::record (spec/keys :req-un [::file_link
                                       ::announce_no
                                       ::position
                                       ::location
                                       ::open_date
                                       ::close_date]))
