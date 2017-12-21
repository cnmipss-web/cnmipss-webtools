(ns webtools.spec.dates
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            #?(:clj  [clj-time.core :refer [date-time]]
               :cljs [cljs-time.core :refer [date-time]])))

(s/def ::date (s/with-gen
                (partial instance? #?(:clj org.joda.time.DateTime
                                      :cljs js/Object))
                (fn [] (gen/fmap (partial apply #(date-time %1 %2 %3))
                                 (gen/tuple
                                  (gen/choose 2000 2025) ;year
                                  (gen/choose 1 12) ;month
                                  (gen/choose 1 28) ;day
                                  )))))

(s/def ::date-str (s/with-gen
                    string?
                    (fn [] (gen/fmap (partial apply #(str %1 " " %2 ", " %3))
                                     (gen/tuple
                                      (s/gen ::month)
                                      (gen/choose 1 28) ;day
                                      (gen/choose 2000 2025) ;year
                                      )))))

(s/def ::date-time-str (s/with-gen
                            string?
                            (fn [] (gen/fmap (partial apply #(str %1 " " %2 ", " %3 " " %4 ":" %5 " " %6))
                                             (gen/tuple
                                              (s/gen ::month)
                                              (gen/choose 1 28) ;day
                                              (gen/choose 2000 2025) ;year
                                              (gen/choose 10 22) ;hour
                                              (gen/choose 10 59) ; minute
                                              (s/gen #{"AM" "PM"})
                                              )))))

(s/def ::date-at-time-str (s/with-gen
                            string?
                            (fn [] (gen/fmap (partial apply #(str %1 " " %2 ", " %3 " at " %4 ":" %5 " " %6))
                                             (gen/tuple
                                              (s/gen ::month)
                                              (gen/choose 1 28) ;day
                                              (gen/choose 2000 2025) ;year
                                              (gen/choose 10 22) ;hour
                                              (gen/choose 10 59) ; minute
                                              (s/gen #{"AM" "PM"})
                                              )))))


(s/def ::month #{"January" "February" "March" "April" "May" "June"
                 "July" "August" "September" "October" "November" "December"})
