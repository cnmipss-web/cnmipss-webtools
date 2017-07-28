(ns webtools.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [webtools.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[webtools started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[webtools has shut down successfully]=-"))
   :middleware wrap-dev})
