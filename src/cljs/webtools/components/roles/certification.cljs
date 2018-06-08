(ns webtools.components.roles.certification
  (:require [re-frame.core :as rf]
            [webtools.components.grid :as grid]
            [webtools.components.error :as error]
            [webtools.components.forms.certification :as cforms]
            [webtools.components.forms.generic :as gforms]
            [webtools.components.tables.certification :as tables]
            [webtools.cookies :refer [get-cookie]]
            [webtools.handlers.events :as events]))

(defn certification-view []
  [grid/row
   [grid/full-width-column
    [gforms/upload-form {:path     (.-hash js/location)
                         :action   "/webtools/upload/certification-csv"
                         :accept   "csv"
                         :label    "Upload CSV File"
                         :multiple false}]]
   [grid/full-width-column
    [error/reporter]]
   [grid/full-width-column
    (when-let [error @(rf/subscribe [:error])]
      (events/get-collision-list)
      [tables/error-table])]
   [grid/full-width-column
    [cforms/search-certification-records]
    [tables/existing-certifications]]])
