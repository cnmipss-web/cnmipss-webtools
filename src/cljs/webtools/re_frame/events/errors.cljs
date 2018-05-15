(ns webtools.re-frame.events.errors
  (:require [re-frame.core :refer [reg-event-db dispatch]]))

(reg-event-db
 :display-error
 (fn [db [_ error]]
   (js/setTimeout #(.attr (js/jQuery "#err-msg.slow-fade") "data-invis" "true") 13000)
   (js/setTimeout (fn [] (dispatch [:clear-error])) 15000)
   (assoc db :error error)))

(reg-event-db
 :clear-error
 (fn [db _]
   (dissoc db :error)))
