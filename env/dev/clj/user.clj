(ns user
  (:require [mount.core :as mount]
            [certification-db.figwheel :refer [start-fw stop-fw cljs]]
            certification-db.core))

(defn start []
  (mount/start-without #'certification-db.core/repl-server))

(defn stop []
  (mount/stop-except #'certification-db.core/repl-server))

(defn restart []
  (stop)
  (start))


