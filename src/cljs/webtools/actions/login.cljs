(ns webtools.actions.login
  (:require 
   [re-frame.core :refer [dispatch reg-event-db]]
   [webtools.db :as db]
   [webtools.cookies :refer [set-cookie]]
   [webtools.util :as util]
   [webtools.constants :as const]))

(reg-event-db
 :bad-login
 (fn [db [_ bool]]
   (assoc db :bad-login bool)))

(reg-event-db
 :login
 (fn [db [_ email admin roles]]
   (let [roles-list (clojure.string/split roles #",")]
     (when (and
            (not admin)
            (= 1 (count roles-list)))
       (set-cookie :role (first roles-list))
       (dispatch [:set-active-role (first roles-list)])
       (dispatch [:hide-roles]))
     (dispatch [:bad-login false])
     (as-> (assoc db :session (util/keyed [email admin])) db
       (if admin
         (assoc db :roles const/role-list)
         (assoc db :roles roles-list))))))
