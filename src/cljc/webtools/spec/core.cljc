(ns webtools.spec.core
  #?(:clj (:require [clojure.spec.alpha :as s]
                    [clojure.spec.gen.alpha :as gen]
                    [ring.core.spec])
     :cljs (:require [clojure.spec.alpha :as s]
                     [clojure.spec.gen.alpha :as gen])))

(s/def ::uuid (s/with-gen
                #(instance? #?(:clj  java.util.UUID
                               :cljs cljs.core/UUID) %)
                gen/uuid))
