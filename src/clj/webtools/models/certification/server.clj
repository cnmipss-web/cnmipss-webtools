(ns webtools.models.certification.server
  (:require [webtools.models.certification.core :as cert]
            [webtools.util.dates :as util-dates]
            [clj-time.core :as t]
            [clojure.tools.logging :as log]
            [webtools.db :as db])
  (:import [webtools.models.certification.core CertificationRecord]
           [webtools.models.certification.core CertificationCollision]))

(extend-type CertificationRecord
  cert/handle-certs
  (changed? [new-cert orig-cert]
    (let [diffs (clojure.data/diff new-cert orig-cert)]
      (some some? (take 2 diffs))))

  (is-renewal? [new-cert orig-cert]
    (let [orig-start  (->> orig-cert :start_date util-dates/parse-date)
          new-start   (->> new-cert :start_date util-dates/parse-date)
          orig-expiry (->> orig-cert :expiry_date util-dates/parse-date)
          new-expiry  (->> new-cert :expiry_date util-dates/parse-date)]
      (try
        (if-let [overlap (t/overlap (t/interval orig-start orig-expiry)
                                    (t/interval new-start new-expiry))]
          (> 365 (t/in-days overlap))
          true)
        (catch Exception e
          (log/error (.getMessage e) orig-start orig-expiry new-start new-expiry "\n\n")))))

  (renew-cert! [cert]
    (let [{:keys [cert_no
                  start_date
                  expiry_date]} cert
          existing-certs        (filter (fn [cert] (re-seq (re-pattern cert_no) (:cert_no cert)))
                                        (db/get-all-certs))
          renewal-no            (count existing-certs)]
      (if (not-any? #(and (= start_date (:start_date %))
                          (= expiry_date (:expiry_date %))) existing-certs)
        (cert/save-to-db! (assoc cert :cert_no (str cert_no "-renewal-" renewal-no))))))

  (save-to-db! [cert]
    (db/create-cert! cert))

  (change-in-db! [cert]
    (db/update-cert! cert)))

(defn- full-name [{:keys [first_name mi last_name]}]
  (str first_name " " mi " " last_name))

(extend-type CertificationCollision
  cert/handle-collision
  (save-collision-to-db! [collision]
    (let [{:keys [cert1 cert2]} collision
          record {:id (java.util.UUID/randomUUID)
                  :cert_no (:cert_no cert1)
                  :name1 (full-name cert1)
                  :start_date1 (:start_date cert1)
                  :expiry_date1 (:expiry_date cert1)
                  :cert_type1 (:cert_type cert1) 
                  :name2 (full-name cert2)
                  :start_date2 (:start_date cert2)
                  :expiry_date2 (:expiry_date cert2)
                  :cert_type2 (:cert_type cert2) }]
      (db/save-collision! record))))
