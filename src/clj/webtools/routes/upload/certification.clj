(ns webtools.routes.upload.certification
  "Provides public routines for processing uploaded files related to the Certification role."
  (:require [clojure.data :refer [diff]]
            [clojure.data.csv :as csv]
            [webtools.db :as db]
            [webtools.exceptions.certification :as ex]
            [webtools.models.certification.core :as cert]
            [webtools.models.certification.server]))

(defn- parse-new-cert
  "Convert a list represented a row in the uploaded CSV file to a hash-map representing the certification record."
  [current]
  (let [[_ lastname firstname mi _ _ type certno start expiry _] current]
    (cert/map->CertificationRecord
     {:cert_no     certno
      :cert_type   type
      :start_date  start
      :expiry_date expiry
      :first_name  firstname
      :last_name   lastname
      :mi          mi})))

(defn- match-cert?
  "Generate a function to test if a given certification shares the supplied cert-no."
  [cert-no]
  (fn [cert]
    (if (= cert-no (:cert_no cert))
      cert
      false)))

(defn- handle-collision
  "Determine how to proceed when two certifications collide based on their certification number.

  If one is a renewal of the other, create a renewal record.  
  If not, but the name is the same, overwrite the old one.
  If the new certifcation is not a renewal, and has a difference name, create an error to be returned to user."
  [new-cert orig-cert errors]
  (let [[new-only orig-only joint] (diff new-cert orig-cert)
        same-name? (not-any? nil? (map joint [:first_name :last_name])) 
        same-cert-type? (some? (:cert_type joint))
        same-dates? (not-any? nil? (map joint [:start_date :expiry_date])) ]
    (if-not (and same-name? same-cert-type?)
      (swap! errors conj (ex/single-cert-collision new-cert orig-cert))
      (if-not same-dates?
        (if (cert/is-renewal? new-cert orig-cert)
          (cert/renew-cert! new-cert)
          (cert/change-in-db! new-cert))
        :ignore-identical-existing-cert))))

(defn process-cert-csv
  "Determine whether a certificate is new, a renewal, or erroneous and store it accordingly.

  If new, save to DB.
  If renewal, save renewal to DB.
  If erroneous, return errors to user."
  [params]
  (let [{:keys [file]} params
        {:keys [tempfile size filename]} file
        data (->> tempfile slurp csv/read-csv (drop 1) (sort-by #(get % 7)))
        existing-certs (db/get-all-certs)]
    (loop [current (first data) rem (next data) errors (atom [])]
      (let [[_ last-name first-name mi _ _ type cert-no start expiry _] current
            fresh-cert (parse-new-cert current)]
        (if-let [cert (some (match-cert? cert-no) (db/get-all-certs))] ;existing-certs vs db/get-all-certs
          (if (cert/changed? fresh-cert cert)
            (handle-collision fresh-cert cert errors)
            :ignore-identical-existing-cert)
          (db/create-cert! fresh-cert)))
      (if (pos? (count rem))
        (recur (first rem) (next rem) errors)
        (if (pos? (count @errors))
          (let [five-errors (take 5 @errors)]
            (throw (ex/list-cert-collisions five-errors))))))))

