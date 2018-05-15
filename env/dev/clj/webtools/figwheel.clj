(ns webtools.figwheel
  (:require [figwheel-sidecar.repl-api :as ra]
            [webtools.sass :as sass]))

(defn start-fw []
  (ra/start-figwheel!)
  (sass/start!))

(defn stop-fw []
  (ra/stop-figwheel!)
  (sass/stop!))

(defn cljs []
  (ra/cljs-repl))

