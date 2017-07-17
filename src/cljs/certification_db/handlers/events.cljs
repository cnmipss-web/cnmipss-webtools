(ns certification-db.handlers.events
  (:require [re-frame.core :as rf]))

(defn set-active-role
  [role]
  (fn [event]
    (.preventDefault event)
    (rf/dispatch [:set-active-role role])))
