(ns webtools.re-frame.events.fns
  (:require [clojure.walk :refer [keywordize-keys]]
            [re-frame.core :refer [reg-event-db]]))

(reg-event-db
 :store-fns-nap
 (fn [db [_ fns-nap]]
   (assoc db :fns-nap (map keywordize-keys fns-nap))))
