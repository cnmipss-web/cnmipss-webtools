(ns webtools.json
  (:require [cheshire.core :as json]
            [cheshire.generate :refer [add-encoder encode-str remove-encoder]]
            [clojure.walk :as walk]))

(def json->edn
  (comp walk/keywordize-keys json/parse-string))

(def edn->json
  (comp json/generate-string))

