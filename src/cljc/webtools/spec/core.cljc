(ns webtools.spec.core
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            #?(:clj  [clj-time.core :refer [date-time]]
               :cljs [cljs-time.core :refer [date-time]])
            #?(:clj  [ring.core.spec])))

(s/def ::uuid (s/with-gen
                #(instance? #?(:clj  java.util.UUID
                               :cljs cljs.core/UUID) %)
                gen/uuid))

(s/def ::uuid-str (s/with-gen
                    string?
                    (fn [] (gen/fmap str (gen/uuid)))))

(s/def ::nil nil?)




