(ns webtools.spec.subscription
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

(s/def :webtools.procurement.core/id :webtools.spec.core/uuid)
(s/def ::proc_id :webtools.spec.core/uuid)
(s/def ::company_name string?)
(s/def ::contact_person string?)

(s/def ::email (s/with-gen
                 #(some? (re-find #"^(([^<>()\[\]\\.,;:\s@\"]+(\.[^<>()\[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$" %))
                 #(gen/elements ["test@test.com" "thisis@notreal.email" "fake@steak.org"])))

(s/def ::telephone (s/with-gen
                     #(some? (re-find #"^(\+\d{1,2}\s)?(\(?\d{3}\)?)?[\s.-]?\d{3}[\s.-]?\d{4}$" %))
                     #(gen/fmap webtools.util/format-tel-num (gen/choose 1000000 9999999))))

(s/def ::subscription_number pos-int?)

(s/def ::record
  (s/keys :req-un [::id
                   ::proc_id
                   ::subscription_number
                   ::company_name
                   ::contact_person
                   ::email
                   ::telephone]))
