(ns webtools.test.util
  (:require [bond.james :as bond]))

(defn count-calls
  "Return the number of times stub was called"
  [stub]
  ((comp count bond/calls) stub))

(defn args-from-call
  "Return a vector containing all the args from call number n.  n defaults to 1 (i.e. first)"
  ([stub] (args-from-call stub 1))
  ([stub n]
   (-> stub bond/calls (get (- n 1)) :args vec)))
