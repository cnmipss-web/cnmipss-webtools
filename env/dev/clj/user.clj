(ns user
  (:require [mount.core :as mount]
            [webtools.figwheel :refer [start-fw stop-fw cljs]]
            webtools.core))

(defn start []
  (mount/start-without #'webtools.core/repl-server))

(defn stop []
  (mount/stop-except #'webtools.core/repl-server))

(defn restart []
  (stop)
  (start))


