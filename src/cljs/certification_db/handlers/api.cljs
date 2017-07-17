(ns certification-db.handlers.api
  (:require [re-frame.core :as rf]
            [certification-db.constants :as const]))

(defn bad-login []
  (rf/dispatch [:bad-login])
  (set! (.-href js/location) "#/"))

(defn verified-token?
  [email token]
  (fn [[ok response]]
    (let [admin (get-in response [:body "is-admin"])
          user (get-in response [:body "user"])
          roles (get user "roles")]
      (if ok
        (do
          (rf/dispatch [:set-session {:account token
                                      :email email
                                      :admin admin}])
          (if admin
            (rf/dispatch [:set-roles const/role-list])
            (rf/dispatch [:set-roles (clojure.string/split roles  #",")]))
          (rf/dispatch [:set-active-page :main]))
        (bad-login)))))

(defn all-users
  [[ok {:keys [body]}]]
  (let [users (get body "users")]
    (println "All users: " users)
    (rf/dispatch [:store-users users])))
