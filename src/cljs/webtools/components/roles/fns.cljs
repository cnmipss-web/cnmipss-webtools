(ns webtools.components.roles.fns
  (:require [clojure.walk :refer [keywordize-keys]]
            [re-frame.core :as rf]
            [webtools.components.grid :as grid]
            [webtools.components.forms :as forms]
            [webtools.util.dates :as util-dates]
            [webtools.components.tables :as tables]
            [webtools.components.error :as error]
            [webtools.components.grid :as grid]))

(defn fns-view []
  [grid/row
   [grid/full-width-column
    [forms/fns-upload-form]]
   [grid/full-width-column
    [error/reporter]]
   [grid/full-width-column
    [tables/fns-recent-results]]])
