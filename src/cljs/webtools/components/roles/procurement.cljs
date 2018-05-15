(ns webtools.components.roles.procurement
  (:require [webtools.components.forms.procurement :as forms]
            [webtools.components.tables.procurement :as tables]
            [webtools.components.error :as error]))

(defn pns-view []
  [:div.row
   [:div.col-xs-12.col-sm-10.offset-sm-1
    [forms/procurement-upload]]
   [:div.col-xs-12.col-sm-10.offset-sm-1
    [error/reporter]]
   [:div.col-xs-12.col-sm-10.offset-sm-1
    [tables/rfp-ifb-list]]])
