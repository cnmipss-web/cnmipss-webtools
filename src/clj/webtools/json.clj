(ns webtools.json
  (:require [cheshire.core :as json]
            [clojure.walk :as walk]))

(defn json->edn [string]   (walk/keywordize-keys (json/parse-string string)))

(def edn->json
  (comp json/generate-string))

