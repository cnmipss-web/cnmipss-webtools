(ns webtools.constants
  (:require [webtools.config :refer [env]]))

(def role-list ["Certification" "HRO" "Procurement" "Manage Users" "Manage DB"])

(def wp-host (:wp-host env))

(def wp-token-route "/wp-json/jwt-auth/v1/token")

(def wp-media-route "/wp-json/wp/v2/media")

(def max-cookie-age 900)

(def date-string "MMMM dd, YYYY")

(def procurement-date-format date-string)
(def procurement-datetime-format "MMMM dd, YYYY 'at' h:mm a")
(def procurement-datetime-re #"(\w+\s\d+,\s\d{4} at \d+\:\d\d\s\w{2})")
