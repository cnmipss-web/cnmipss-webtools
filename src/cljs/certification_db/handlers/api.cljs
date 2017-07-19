(ns certification-db.handlers.api
  (:require [re-frame.core :as rf]
            [certification-db.constants :as const]
            [certification-db.util :as util]
            [ajax.core :as ajax]))

(def jq js/jQuery)

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
          (rf/dispatch [:set-session (util/keyed [token email admin])])
          (if admin
            (rf/dispatch [:set-roles const/role-list])
            (rf/dispatch [:set-roles (clojure.string/split roles  #",")]))
          (rf/dispatch [:set-active-page :main]))
        (bad-login)))))

(defn all-users
  [[ok {:keys [body]}]]
  (let [users (clojure.walk/keywordize-keys body)]
    (rf/dispatch [:store-users users])))

(defn all-jvas
  [[ok response]]
  (let [{:keys [body]} response
        jvas (clojure.walk/keywordize-keys body)]
    (if ok
      (rf/dispatch [:store-jvas] jvas)
      (println response))))

