(ns certification-db.handlers.api
  (:require [re-frame.core :as rf]
            [certification-db.constants :as const]
            [certification-db.util :as util]
            [ajax.core :as ajax]))

(def jq js/jQuery)

(defn bad-login []
  (rf/dispatch [:bad-login])
  (set! (.-href js/location) "#/login"))

(defn verified-token?
  [[ok response]]
  (let [admin (get-in response [:body "is-admin"])
        user (get-in response [:body "user"])
        roles (get user "roles")
        email (get user "email")]
    (if ok
      (do
        (rf/dispatch [:verified-token email admin roles])
        (rf/dispatch [:set-active-page :main]))
      (bad-login))))

(defn all-users
  [[ok {:keys [body]}]]
  (let [users (clojure.walk/keywordize-keys body)]
    (rf/dispatch [:store-users users])))

(defn all-jvas
  [[ok response]]
  (let [{:keys [body]} response
        jvas (clojure.walk/keywordize-keys body)]
    (if ok
      (rf/dispatch [:store-jvas jvas])
      (println response))))

