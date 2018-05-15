(ns webtools.components.roles.hro
  (:require [re-frame.core :as rf]
            [webtools.components.error :as error]
            [webtools.components.forms.hro :as forms]
            [webtools.components.tables.hro :as tables]))

(defn hro-view []
  [:div.row
   [:div.col-xs-12.col-sm-10.offset-sm-1
    [forms/jva-upload]]
   [:div.col-xs-12.col-sm-10.offset-sm-1
    [error/reporter]]
   [:div.col-xs-12.col-sm-10.offset-sm-1
    [forms/search-jva-records]
    [tables/jva-list]]])
