(ns webtools.constants
  (:require [webtools.config :refer [env]]
            #?(:clj [clj-time.format :as f]
               :cljs [cljs-time.format :as f])))

(def role-list ["Certification" "HRO" "Procurement" "FNS"
                "Manage Users" "Manage DB"])

(def wp-host (:wp-host env))

(def wp-token-route "/wp-json/jwt-auth/v1/token")

(def wp-media-route "/wp-json/wp/v2/media")

(def max-cookie-age 900)

(def date-string "MMMM dd, YYYY")
(def date-at-time-string "MMMM dd, YYYY 'at' h:mm a")
(def date-time-string "MMMM dd, YYYY h:mm a")
(def nap-date-string "dd-MMM-YY")
(def numeric-date-string "dd/MM/YYYY")

(def date-formatter         (f/formatter date-string))
(def date-at-time-formatter (f/formatter date-at-time-string))
(def date-time-formatter    (f/formatter date-time-string))
(def nap-date-formatter     (f/formatter nap-date-string))
(def numeric-date-formatter (f/formatter numeric-date-string))

;(def procurement-date-format date-string)
;(def procurement-datetime-format "MMMM dd, YYYY 'at' h:mm a")
(def date-at-time-re #"(\w+\s\d+,\s\d{4} at \d+\:\d\d\s\w{2})")

(def fns-nap-ethnicity-mapping {"BA" "Bangladeshi"
                                "BL" "Bangladeshi"
                                "CH" "Chamorro"
                                "CL" "Carolinian"
                                "CN" "Chinese"
                                "FL" "Filipino"
                                "KO" "Korean"
                                "MA" "Marshallese"
                                "PA" "Palauan"
                                "PO" "Pohnpeian"
                                "TA" "Thai"
                                "TI" "Thai"
                                "TR" "Chuukese"
                                "YA" "Yapese"})
