(ns webtools.components.roles.fns
  (:require [clojure.walk :refer [keywordize-keys]]
            [re-frame.core :as rf]
            [webtools.components.forms :as forms]
            [webtools.util.dates :as util-dates]
            [webtools.components.tables :as tables]
            [webtools.components.error :as error]
            [webtools.components.grid :as grid]))

(defn fns-view []
  [:div.row
   [grid/full-width-column
    [forms/fns-upload-form]]
   [:div.col-xs-12.col-sm-10.offset-sm-1
    [error/reporter]]
   [:div.col-xs-12.col-sm-10.offset-sm-1
    [tables/fns-recent-results]]])
