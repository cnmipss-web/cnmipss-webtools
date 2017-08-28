(ns webtools.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [webtools.core-test]))

(doo-tests 'webtools.core-test)

