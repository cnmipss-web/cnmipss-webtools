(ns webtools.procurement.front-end
  (:require [webtools.procurement.core :as p]))

(extend-type webtools.procurement.core/PSAnnouncement)

;(extend-type js/String)

(extend-type cljs.core/PersistentArrayMap
  p/create-procurement
  (p/pns-from-map [pns]
    (p/PSAnnouncement. (:id pns)
                      (:type pns)
                      (:number pns)
                      (:open_date pns)
                      (:close_date pns)
                      (:title pns)
                      (:description pns)
                      (:file_link pns))))

(extend-type nil)
