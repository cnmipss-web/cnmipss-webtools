(ns webtools.cron.jobs
  (:require [webtools.db.core :as db]
            [webtools.procurement :refer :all]
            [webtools.email :as email]
            [clj-time.core :as t]))

(defn- -deadline-notifier
  [interval mailer]
  (doseq [rfp (mapv pns-from-map (db/get-all-rfps))]
    (if (t/within? interval (:close_date rfp))
      (let [subscribers (db/get-subscriptions {:rfp_id (:id rfp)
                                               :ifb_id nil})]
        (mapv (partial mailer rfp) subscribers))))
  (doseq [ifb (mapv pns-from-map (db/get-all-ifbs))]
    (if (t/within? interval (:close_date ifb))
      (let [subscribers (db/get-subscriptions {:ifb_id (:id ifb)
                                               :rfp_id nil})]
        (mapv (partial mailer ifb) subscribers)))))

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
