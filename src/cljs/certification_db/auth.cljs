(ns certification-db.auth
  (:require [ajax.core :as ajax]))

(defn login
  [e]
  (.preventDefault e)
  (ajax/GET "/oauth/oauth-init" {:headers {"x-csrf-token" js/csrfToken}} #(.log js/console %)))
