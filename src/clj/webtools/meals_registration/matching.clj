(ns webtools.meals-registration.matching
  (:require [clojure.spec.alpha :as s]
            [webtools.meals-registration.core]
            [webtools.meals-registration.matching.random :refer [random-match]])
  (:import (webtools.meals_registration.core FNSRegistration
                                             NAPRegistration)))

(defprotocol matching-registration
  (match-probability [source target] "Estimate the likelihood of a match between two registration records based on string distances and other parameters"))

(s/def ::registration (s/alt :fns :webtools.meals-registration.core/fns-reg
                             :nap :webtools.meals-registration.core/nap-reg))
(s/fdef match-probability
        :args (s/cat :source ::registration
                     :target ::registration)
        :ret float?

        :fn (s/and (fn [{:keys [args ret]}]
                     (or (and (s/valid? :webtools.meals-registration.core/fns-reg (:source args))
                              (s/valid? :webtools.meals-registration.core/nap-reg (:target args)))
                         
                         (and (s/valid? :webtools.meals-registration.core/nap-reg (:source args))
                              (s/valid? :webtools.meals-registration.core/fns-reg (:target args)))))))

(extend-protocol matching-registration
  FNSRegistration
  (match-probability [source target]
    (random-match source target))

  NAPRegistration
  (match-probability [target source]
    (random-match source target)))


