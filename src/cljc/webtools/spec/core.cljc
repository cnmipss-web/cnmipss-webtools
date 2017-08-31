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

(s/def ::date (s/with-gen
                (partial instance? #?(:clj org.joda.time.DateTime
                                      :cljs js/Function))
                (fn [] (gen/fmap (partial apply #(date-time %1 %2 %3 %4))
                                 (gen/tuple
                                  (gen/choose 2000 2025) ;year
                                  (gen/choose 1 12) ;month
                                  (gen/choose 1 28) ;day
                                  (gen/choose 0 23) ;hour
                                  )))))


