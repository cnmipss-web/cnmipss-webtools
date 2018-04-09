(ns webtools.models.mhd
  (:require [webtools.models.hro.mhd :as mhd]
            [webtools.exceptions :as w-ex]
            [webtools.util :refer [pull]]
            [clojure.string :as cstr]))

(pull webtools.models.hro.mhd
      [ticket-fetch])

(defmethod ticket-fetch :list [k & args]
  (cstr/join (cons "Fetched a list of tickets!" args))) 

(defmethod ticket-fetch :single [k & args]
  "Fetched a single ticket!")

(defmethod ticket-fetch :default [k & args]
  (throw (w-ex/illegal-argument {:msg "Invalid first arg passed to webtools.models.hro.mhd/ticket-fetch.  First arg should be one of #{:list :single}"
                                 :data {:called `(ticket-fetch ~k ~@args)
                                        :first-arg `(~k)
                                        :other-args args}})))
