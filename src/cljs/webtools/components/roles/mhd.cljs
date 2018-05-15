(ns webtools.components.roles.mhd
  (:require [re-frame.core :as rf]
            [webtools.models.mhd :as mhd]))

(defn mhd-view []
  [:div.row
   [:div.col-xs-12.col-sm-10.offset-sm-1
    (mhd/ticket-fetch :list 1 2 3)]])
