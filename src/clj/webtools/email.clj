(ns webtools.email
  (:require [webtools.config :refer [env]]
            [postal.core :refer [send-message]]
            [hiccup.core :refer [html]]))

(defn invite [user]
  (let [{:keys [email roles admin]} user
        name (->> email
                  (re-find #"^(.*?)\.(.*)@")
                  (drop 1)
                  (map clojure.string/capitalize)
                  (clojure.string/join " "))
        {:keys [wp-host wp-un server-uri]} env]    
    (send-message {:from "webmaster@cnmipss.org"
                   :to [email]
                   :subject "Invitation to CNMI PSS Webtools"
                   :body [{:type "text/html"
                            :content (html
                                      [:html
                                       [:body
                                        [:p (str "Greetings " name ",")]
                                        [:p (str "The CNMI PSS Webmaster, "
                                                 wp-un
                                                 " has invited you to use the Webtools interface to publish content"
                                                 " to the CNMI PSS website.  You will have access to the "
                                                 (if (> (count roles) 0)
                                                   (str
                                                    (-> roles
                                                        (clojure.string/split #",")
                                                        (#(conj (vec (butlast %)) (str "and " (last %))))
                                                        (#(clojure.string/join #", " %))
                                                        (clojure.string/replace #"^and " ""))
                                                    (if admin
                                                      " roles with admin access."
                                                      " roles."))
                                                   (if admin "admin interface.")))]
                                        [:p "You can access the site at "
                                         [:a {:href server-uri} "the webtools page "]
                                         "and login using your CNMI PSS email address."]
                                        [:p "If you have any questions, please contact the Webmaster via email."]
                                        [:br]
                                        [:p "Thank you,"]
                                        [:p "Tyler Collins"
                                         [:br]
                                         "Webmaster, CNMI PSS"
                                         [:br]
                                         [:a {:href "mailto:tyler.collins@cnmipss.org"}
                                          "tyler.collins@cnmipss.org"]]]])}]})))

(defn confirm-subscription [subscription pns]
  (let [{:keys [email contact_person company_name]} subscription]
    (send-message {:from "procurement@cnmipss.org"
                   :to [email]
                   :subject "Subscription Confirmed"
                   :body [{:type "text/html"
                           :content (html
                                     [:html
                                      [:body
                                       [:p (str "Greetings " contact_person ",")]
                                       [:p (str "This email is your confirmation that you have registered to receive updates and information regarding "
                                                (if (:rfp_no pns) (str "Request for Proposal " (:rfp_no pns)))
                                                (if (:ifb_no pns) (str "Invitation for Bid " (:ifb_no pns)))
                                                ": " (:title pns) ".")]
                                       [:p (str "You will be contacted as additional information is published.  If you have any questions, please contact Kimo Rosario at kimo.rosario@cnmipss.org")]
                                       [:br]
                                       [:p "Thank you,"]
                                       [:p "Kimo Rosario"]
                                       [:p "Procurement & Supply Officer"]
                                       [:p "CNMI PSS"]]])}]})))

(defn announce-updates [update subscribers])
