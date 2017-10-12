(ns webtools.timeout
  (:require [ajax.core :as ajax]
            [webtools.constants :refer [max-cookie-age]]
            [webtools.cookies :as cookies]
            [cljs-time.core :as t]))

(def jq js/jQuery)

(def idle-time (atom 0))
(def timeout max-cookie-age)
(def interval 60)

(defn check-idle-time []
  (swap! idle-time (partial + interval))
  (when (> @idle-time timeout)
    (cookies/set-cookie :timed-out true :max-age 60 :path "/webtools")
    (.reload js/location))
  (when (< @idle-time (inc interval))
    (ajax/GET "/webtools/api/refresh-session")))

(defn reset-idle-time []
  (reset! idle-time 0))

(defn configure []
  (let [doc (jq "body")]
    (js/setInterval check-idle-time (* 1000 interval))
    (.on doc "click" reset-idle-time)
    (.on doc "mousemove"  reset-idle-time)
    (.on doc "keypress" reset-idle-time)))

(defn throttle [callback wait]
  (let [time (atom (t/now))]
    (fn []
      (when (t/after? (t/plus @time wait) (t/now))
        (println "Callback") 
        (callback)
        (reset! time (t/now))))))
