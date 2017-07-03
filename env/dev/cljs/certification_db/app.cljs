(ns ^:figwheel-no-load certification-db.app
  (:require [certification-db.core :as core]
            [devtools.core :as devtools]))

(enable-console-print!)

(devtools/install!)

(core/init!)
