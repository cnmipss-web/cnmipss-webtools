(ns certification-db.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[certification-db started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[certification-db has shut down successfully]=-"))
   :middleware identity})
