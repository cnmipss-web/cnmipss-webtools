(ns webtools.components.roles.fns
  (:require [webtools.components.forms :as forms]))

(defn- -fns-recent-results []
  [:table])

(defn fns-view []
  [:div.row
   [:div.col-xs-12
    [forms/fns-upload-form]]
   [:div.col-xs-12
    [-fns-recent-results]]])
