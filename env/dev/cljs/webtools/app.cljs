(ns ^:figwheel-no-load webtools.app
  (:require [webtools.core :as core]
            [devtools.core :as devtools]))

(enable-console-print!)

(devtools/install!)

(core/init!)
