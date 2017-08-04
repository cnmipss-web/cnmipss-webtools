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
