(ns webtools.components.roles.procurement
  (:require [webtools.components.forms.procurement :as forms]
            [webtools.components.tables.procurement :as tables]
            [webtools.components.error :as error]))

(defn pns-view []
  [:div.row
   [:div.col-xs-12
    [forms/procurement-upload]]
   [error/reporter]
   [:div.col-xs-12
    [tables/rfp-ifb-list]]])
