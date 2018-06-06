(ns webtools.components.roles.procurement
  (:require [webtools.components.grid :as grid]
            [webtools.components.forms.procurement :as forms]
            [webtools.components.tables.procurement :as tables]
            [webtools.components.error :as error]))

(defn pns-view []
  [grid/row
   [grid/full-width-column
    [forms/procurement-upload]]
   [grid/full-width-column
    [error/reporter]]
   [grid/full-width-column
    [tables/rfp-ifb-list]]])
