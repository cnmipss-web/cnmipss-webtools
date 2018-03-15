(ns webtools.components.roles.fns
  (:require [clojure.walk :refer [keywordize-keys]]
            [re-frame.core :as rf]
            [webtools.components.forms :as forms]
            [webtools.util.dates :as util-dates]
            [webtools.components.tables :as tables]
            [webtools.components.error :refer [error-reporter]]))

(defn fns-view []
  [:div.row
   [:div.col-xs-12
    [forms/fns-upload-form]]
   [:div.col-xs-12.text-center.mt-1
    [error-reporter]]
   [:div.col-xs-12
    [tables/fns-recent-results]]])
