(ns webtools.spec.core
  (:require [clojure.spec.alpha :as spec]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as cstr]
            #?(:clj  [clj-time.core :refer [date-time]]
               :cljs [cljs-time.core :refer [date-time]])
            #?(:clj [ring.core.spec])
            [webtools.validation :refer [valid-url?]]))

(spec/def ::uuid uuid?)

(spec/def ::uuid-str (spec/with-gen
                       (spec/and string?
                                 (fn [uuid]
                                   (= 36 (count uuid))
                                   (= 32 (count (cstr/replace uuid "-" "")))))
                       (fn [] (gen/fmap str (gen/uuid)))))

(spec/def ::nil nil?)

(spec/def ::link (spec/and string? valid-url?))

(spec/def ::xlsx-file-link (spec/and ::link
                                     (fn xlsx-extension? [url]
                                       (let [ext ".xlsx"]
                                         (=
                                          (- (count url) (count ext))
                                          (cstr/index-of url ext))))))

(spec/def ::throwable? (partial instance? #?(:clj java.lang.Exception
                                             :cljs js/Error)))




