(ns webtools.components.roles.mhd
  (:require [re-frame.core :as rf]
            [webtools.components.grid :as grid]
            [webtools.models.mhd :as mhd]))

(defn mhd-view []
  [grid/row
   [grid/full-width-column
    (mhd/ticket-fetch :list 1 2 3)]])
