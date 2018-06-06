(ns webtools.components.roles.hro
  (:require [re-frame.core :as rf]
            [webtools.components.grid :as grid]
            [webtools.components.error :as error]
            [webtools.components.forms.hro :as forms]
            [webtools.components.tables.hro :as tables]))

(defn hro-view []
  [grid/row
   [grid/full-width-column
    [forms/jva-upload]]
   [grid/full-width-column
    [error/reporter]]
   [grid/full-width-column
    [forms/search-jva-records]
    [tables/jva-list]]])
