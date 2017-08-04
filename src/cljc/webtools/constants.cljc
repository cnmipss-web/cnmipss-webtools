(ns webtools.constants
  (:require [webtools.config :refer [env]]))

(def role-list ["Certification" "HRO" "Procurement" "Manage Users" "Manage DB"])

(def wp-host (:wp-host env))

(def wp-token-route "/wp-json/jwt-auth/v1/token")

(def wp-media-route "/wp-json/wp/v2/media")

(def max-cookie-age 900)
