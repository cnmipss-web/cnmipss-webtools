(ns webtools.spec.certification
  (:require [clojure.spec.alpha :as spec]))

(spec/def ::cert_no string?)
(spec/def ::cert_type string?)
(spec/def ::first_name string?)
(spec/def ::mi string?)
(spec/def ::last_name string?)
(spec/def ::start_date string?)
(spec/def ::expiry_date string?)

(spec/def ::record (spec/keys :req-un [::cert_no
                                       ::cert_type
                                       ::first_name
                                       ::mi
                                       ::last_name
                                       ::start_date
                                       ::expiry_date]))

(spec/def ::cert1 ::record)
(spec/def ::cert2 ::record)

(spec/def ::collision (spec/keys :req-un [::cert1
                                          ::cert2]))
