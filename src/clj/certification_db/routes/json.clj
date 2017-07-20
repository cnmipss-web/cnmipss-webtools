(ns certification-db.util
  (:require [cheshire.core :as json]
            [clojure.walk :as walk]))

(def json->edn
  (comp walk/keywordize-keys json/parse-string))

(def edn->json
  (comp json/generate-string))

