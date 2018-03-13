(ns webtools.validation
  (:require [struct.core :as st])
  #?(:clj (:import  [org.apache.commons.validator.routines UrlValidator])))

#?(:clj (def validator (UrlValidator. UrlValidator/ALLOW_LOCAL_URLS)))

(defn valid-url? [url]
  #?(:clj (.isValid validator url)
     :cljs true))

