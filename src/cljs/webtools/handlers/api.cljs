(ns webtools.handlers.api
  (:require cljsjs.jquery
            [re-frame.core :as rf]
            [webtools.constants :as const]
            [webtools.util :as util]
            [ajax.core :as ajax]
            [klang.core :refer-macros [info! erro!]]))

(def jq js/jQuery)

(defn bad-login [res]
  (rf/dispatch [:bad-login])
  (set! (.-href js/location) "#/login"))

(defn verified-token?
  [[ok full-response]]
  (let [response (if-let [res (:response full-response)] res full-response) ;Deal with different response structures
        admin (get-in response [:body "is-admin"])
        user (get-in response [:body "user"])
        roles (get user "roles")
        email (get user "email")]
    (if ok
      (do
        (rf/dispatch [:login email admin roles])
        (rf/dispatch [:set-active-page :main]))
      (if email
        (bad-login response)))))

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
      (erro! response))))

(defn all-procurement
  [[ok response]]
  (let [{:keys [body]} response]
    (if ok
      (rf/dispatch [:store-procurement-list (clojure.walk/keywordize-keys body)])
      (erro! response))))
 
(defn all-certs
  [[ok response]]
  (let [{:keys [body]} response]
    (if ok
      (rf/dispatch [:store-cert-list (clojure.walk/keywordize-keys body)])
      (erro! response))))

(defn fns-nap
  [[ok {:keys [body status headers] :as response}]]
  (if ok
    (rf/dispatch [:store-fns-nap body])
    (erro! "Error fetching fns-nap: " status body)))

(defn cert-collision-list
  [[ok {:keys [body status headers] :as response}]]
  (if ok
    (rf/dispatch [:error-list (clojure.walk/keywordize-keys body)])
    (erro! "Error fetching cert-collision-list: " status body)))
