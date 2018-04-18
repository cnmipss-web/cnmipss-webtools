(ns webtools.models.certification.core
  (:require [clojure.spec.alpha :as spec]
            [clojure.spec.gen.alpha :as sgen]
            [webtools.spec.certification]))

(defrecord CertificationRecord
    [cert_no
     cert_type
     start_date
     expiry_date
     first_name
     last_name
     mi])

(spec/def ::certification
  (spec/with-gen
    :webtools.spec.certification/record
    (fn [] (sgen/fmap (partial apply ->CertificationRecord)
                      (sgen/tuple
                       (spec/gen :webtools.spec.certification/cert-no)
                       (spec/gen :webtools.spec.certification/cert-type)
                       (spec/gen :webtools.spec.certification/start-date)
                       (spec/gen :webtools.spec.certification/expiry-date)
                       (spec/gen :webtools.spec.certification/first-name)
                       (spec/gen :webtools.spec.certification/last-name)
                       (spec/gen :webtools.spec.certification/mi))))))

(defprotocol handle-certs
  (changed? [new-cert orig-cert]
    "Compare two certification records and return true if they differ, false if they do not.")
  (is-renewal?   [new-cert orig-cert]
    "Compare dates on two certifications.  

  If the certifications overlap by less than one year, the second is considered a renewal and this function returns true.

  If the certificaitons overlap by more than one year, the second is considered an erroneous collision and this function returns false.")

  (renew-cert! [cert]
    "Create a renewal record for a given certification record

  Renewal records take the form of a new certificaiton with -renewal-# added as a suffix to the certification number.")

  (save-to-db! [cert]
    "Save certification record to db, applying any typecasting as needed")

  (change-in-db! [cert]
    "update a certification record in the db, applying any typecasting as needed"))

(spec/fdef cert-changed?
           :args (spec/cat :new-cert ::certification
                           :orig-cert ::certification)
           :ret boolean?)

(spec/fdef is-renewal?
           :args (spec/cat :new-cert ::certification
                           :orig-cert ::certification)
           :ret boolean?)

(spec/fdef renew-cert!
           :args (spec/cat :cert ::certification)
           :ret boolean?)

(spec/fdef save-to-db!
           :args (spec/cat :cert ::certification)
           :ret boolean?)
