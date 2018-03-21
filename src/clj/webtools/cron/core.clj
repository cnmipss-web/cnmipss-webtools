(ns webtools.cron.core
  (:require [chime :refer [chime-at]]
            [clj-time.core :as t]
            [clj-time.periodic :as p]))

(def ^:private jobs (atom {}))

(defn- -schedule [f start interval]
  (let [times (->> (p/periodic-seq start
                                   interval))]
    (chime-at times (fn [t] (f)))))

(def ^:private today-at-midnight (.. (t/now) (withTime 0 0 0 0)))

(defn hourly
  "Calls a function every hour at the start of the hour"
  ([f]
   (-schedule f today-at-midnight (t/hours 1)))
  ([t f]
   (-schedule f t (t/hours 1))))

(defn daily
  "Calls a function every day at a specified time"
  ([f]
   (-schedule f today-at-midnight (t/days 1)))
  ([t f]
   (-schedule f t (t/days 1))))

(defn weekly
  "Calls a function every week on a specified day and time"
  ([f]
   (-schedule f today-at-midnight (t/weeks 1)))
  ([dt f]
   (-schedule f dt (t/weeks 1))))

(defn monthly
  "Calls a function every month on a specified day and time."
  ([f]
   (-schedule f today-at-midnight (t/months 1)))
  ([dt f]
   (-schedule f dt (t/months 1))))

(defprotocol cron-job
  (schedule [this sched] "Calls a function on a schedule")
  (unschedule [this sched] "Removes a function from the schedule"))

(extend-type clojure.lang.IFn
  cron-job
  (schedule
    [this sched]
    (let [s-fns (get @jobs sched)]
      (if (some? (get s-fns this))
        (throw (Exception. (str "Function already scheduled to run " sched)))
        (swap! jobs (fn [a]
                      (assoc a sched
                             (assoc s-fns this (sched this))))))))
  (unschedule
    [this sched]
    (let [s-fns (get @jobs sched)]
      (if (some? (get s-fns this))
        (swap! jobs (fn [a]
                      ((-> a (get sched) (get this)))
                      (assoc a sched
                             (dissoc s-fns this))))
        (throw (Exception. (str "Function " this " is not currently scheduled to run " sched)))))))

(defn unschedule-all []
  (doseq [set (vals @jobs)]
    (doseq [f (vals set)] (f)))
  (reset! jobs {}))
