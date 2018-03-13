(ns webtools.spec.fns-nap
  (:require [clojure.spec.alpha :as spec]
            [clojure.spec.gen.alpha :as sgen]
            [webtools.spec.core]))

(spec/def ::id
  (spec/or :uuid     :webtools.spec.core/uuid
           :uuid-str :webtools.spec.core/uuid-str))

(spec/def ::date_created
  (spec/or :date     :webtools.spec.dates/date
           :date-str :webtools.spec.dates/date-str))

(spec/def ::fns_file_link     :webtools.spec.core/xlsx-file-link)
(spec/def ::nap_file_link     :webtools.spec.core/xlsx-file-link)
(spec/def ::matched_file_link :webtools.spec.core/xlsx-file-link)

(spec/def ::record
  (spec/keys ::req-un [::id
                       ::date_created
                       ::fns_file_link
                       ::nap_file_link
                       ::matched_file_link]))
