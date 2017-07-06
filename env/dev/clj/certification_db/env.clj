(ns certification-db.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [certification-db.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[certification-db started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[certification-db has shut down successfully]=-"))
   :middleware wrap-dev})
