(ns webtools.cron.jobs
  (:require [webtools.db.core :as db]
            [webtools.models.procurement.core :as p]
            [webtools.email :as email]
            [clj-time.core :as t]))

(defn- -deadline-notifier
  [interval mailer]
  (doseq [pnsa (mapv p/convert-pns-from-map (db/get-all-pnsa))]
    (if (t/within? interval (:close_date pnsa))
      (let [subscribers (p/get-subs-from-db (:id pnsa))]
        (mapv (partial mailer pnsa) subscribers)))))

(defn check-notify-subscribers-24hr
  "Function to check all rfps/ifbs for any that are between 24-23 hours of expiring.
   If found, all subscribers to that rfp/ifb are notified by email"
  []
  (let [next-day (t/interval (t/plus (t/now) (t/hours 23))
                             (t/plus (t/now) (t/hours 24)))]
    (-deadline-notifier next-day email/warning-24hr)))

(defn notify-subscribers-closed
  "Function to check all rfps/ifbs that have closed in the last hours.
   If found, all subscribers to that rfp/ifb are notified by email."
  []
  (let [last-hour (t/interval (t/minus (t/now) (t/hours 1))
                              (t/now))]
    (-deadline-notifier last-hour email/notify-pns-closed)))
