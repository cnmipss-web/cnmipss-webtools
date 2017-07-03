(ns certification-db.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [certification-db.core-test]))

(doo-tests 'certification-db.core-test)

