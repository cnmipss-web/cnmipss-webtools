(ns webtools.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[webtools started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[webtools has shut down successfully]=-"))
   :middleware identity})
