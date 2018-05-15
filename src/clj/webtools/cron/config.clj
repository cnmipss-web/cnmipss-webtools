(ns webtools.cron.config
  (:require [webtools.cron.core :as cron]
            [webtools.cron.jobs :as job]))

(defn init!
  []
  (cron/schedule job/check-notify-subscribers-24hr cron/hourly))

(defn stop!
  []
  (cron/unschedule-all))
