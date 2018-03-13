(ns webtools.spec.user
  (:require [clojure.spec.alpha :as spec]
            [clojure.spec.gen.alpha :as sgen]))

(spec/def ::email string?)
(spec/def ::admin boolean?)
(spec/def ::roles (spec/nilable string?))

(spec/def ::record
  (spec/keys ::req-un [::email
                       ::admin
                       ::roles]))

