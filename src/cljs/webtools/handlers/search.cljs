(ns webtools.handlers.search
  (:require [clojure.string :as cstr]
            [re-frame.core :as rf]))

(defn search-by
  [rows & ks]
  (filter
   (fn [row] (let [searches @(rf/subscribe [:search-text])]
               (every? #(re-seq (re-pattern (str "(?i)" %))
                                (cstr/join " " (map row ks))) searches)))
   rows))
