(ns webtools.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [webtools.core-test]
            [webtools.components.forms-test]))

(doo-tests 'webtools.core-test
           'webtools.components.forms-test)
