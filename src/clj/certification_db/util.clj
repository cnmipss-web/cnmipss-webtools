(ns certification-db.util
  (:require [clojure.data.json :as json]
            [clojure.walk :as walk]))

(def json->edn
  (comp walk/keywordize-keys json/read-str))

(def edn->json
  (comp json/write-str))
