(ns webtools.email
  (:require [webtools.config :refer [env]]
            [webtools.db.core :as db]
            [webtools.procurement :refer :all]
            [clj-time.coerce :as c]
            [clj-time.format :as f]
            [postal.core :refer [send-message]]
            [hiccup.core :refer [html]]
            [clojure.tools.logging :as log]
            [clojure.data :refer [diff]]))

(defn invite [user]
  (let [{:keys [email roles admin]} user
        name (->> email
                  (re-find #"^(.*?)\.(.*)@")
                  (drop 1)
                  (map clojure.string/capitalize)
                  (clojure.string/join " "))
        {:keys [wp-host wp-un server-uri]} env]    
    (send-message {:from "webmaster@cnmipss.org"
                   :to email
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
                   :to email
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

(defn fix-dates [data]
  (if (= java.sql.Timestamp (-> data :close_date type))
    (-> data
        (assoc :close_date (c/from-sql-time (:close_date data)))
        (assoc :open_date (c/from-sql-date (:open_date data)))
        (dissoc :status))
    data))

(defn stringify-procurement
  [data]
  (into {} (map (fn [[k v]] [k (.toString v)]) (fix-dates data))))

(def time-format (f/formatter "MMMM dd, YYYY 'at' h:mm a"))
(def date-format (f/formatter "MMMM dd, YYYY"))

(defn print-date
  [date]
  (->> date
      (f/parse (f/formatters :date-time))
      (f/unparse date-format)))

(defn print-datetime
  [datetime]
  (->> datetime
      (f/parse (f/formatters :date-time))
      (f/unparse time-format)))

(defn notify-changes [new orig subscribers]
  (let [title-string (str (-> new :type name clojure.string/upper-case)
                            "# " (:number new) " " (:title new))
        send-fn
        (fn [{:keys [email contact_person] :as sub}]
          (send-message {:to email
                         :from "procurement@cnmipss.org"
                         :subject (str "Changes to " title-string)
                         :body [{:type "text/html"
                                 :content (html (changes-email orig new sub))}]}))]
    (mapv send-fn subscribers)))

(defn id-key [k]
  (->> k name drop-last (apply str) (#(str % "_id")) keyword))

(defn match-subscriber
  [pns id-type]
  (fn [subscriber]
    (= (:id pns) (-> subscriber id-type make-uuid))))

(defn notify-deletion [pns subscribers]
  (log/info "\n\nDeleting: \n" pns "\n\n" subscribers)
  (let [send-fn
        (fn [{:keys [email contact_person] :as sub}]
          (send-message {:to email
                         :from "procurement@cnmipss.org"
                         :subject (str (if (-> sub :rfp_id some?)
                                         "RFP#"
                                         "IFB#")
                                       (if (-> sub :rfp_id some?)
                                         (:rfp_no pns)
                                         (:ifb_no pns))
                                       " " (:title pns)
                                       "has been DELETED")
                         :body [{:type "text/html"
                                 :content (html
                                           [:html
                                            [:body
                                             [:p (str "Greetings " contact_person ",")]
                                             [:p (str "We would like to notify you that "
                                                      (if (-> sub :rfp_id some?)
                                                        "RFP#"
                                                        "IFB#")
                                                      (if (-> sub :rfp_id some?)
                                                        (:rfp_no pns)
                                                        (:ifb_no pns))
                                                      " " (:title pns)
                                                      " has been withdrawn from the CNMI PSS website along with any addenda or other documentation.  You will not receive any further emails regarding this matter.")]
                                             [:br]
                                             [:p "If you have any questions, please contact Kimo Rosario at kimo.rosario@cnmipss.org"]
                                             [:br]
                                             [:p "Thank you,"]
                                             [:p "Kimo Rosario"]
                                             [:p "Procurement & Supply Officer"]
                                             [:p "CNMI PSS"]]])}]}))]
    (mapv send-fn subscribers)))

(defn notify-addenda [pns addendum subscribers]
  (println "\n\n" pns "\n\n" addendum "\n\n" subscribers)
  (let [send-fn
        (fn [{:keys [email contact_person] :as sub}]
          (send-message {:to email
                         :from "procurement@cnmipss.org"
                         :subject (str "Addendum added to "
                                       (if (-> sub :rfp_id some?)
                                         "RFP#"
                                         "IFB#")
                                       (if (-> sub :rfp_id some?)
                                         (:rfp_no pns)
                                         (:ifb_no pns))
                                       " " (:title pns))
                         :body [{:type "text/html"
                                 :content (html
                                           [:html
                                            [:body
                                             [:p (str "Greetings " contact_person ",")]
                                             [:p (str "We would like to notify you that an addendum has been added to "
                                                      (if (-> sub :rfp_id some?)
                                                        "RFP#"
                                                        "IFB#")
                                                      (:number pns) " " (:title pns)
                                                      ".  You may access the full content of this addendum through ")
                                              [:a {:href (:file_link addendum)}
                                               "this link."]]
                                             [:br]
                                             [:p "If you have any questions, please contact Kimo Rosario at kimo.rosario@cnmipss.org"]
                                             [:br]
                                             [:p "Thank you,"]
                                             [:p "Kimo Rosario"]
                                             [:p "Procurement & Supply Officer"]
                                             [:p "CNMI PSS"]]])}]}))]
    (mapv send-fn subscribers)))

(defn notify-subscribers [event k new]
  (let [subscriptions (db/get-all-subscriptions)
        addenda (db/get-all-addenda)
        id-type (id-key k)
        orig (get-pns-from-db (:id new))
        changes (take 2 (diff new orig))]
    (case event
      :update
      (when (every? some? changes)
        (notify-changes new orig (filter (match-subscriber new id-type) subscriptions)))
      :delete
      (notify-deletion orig (filter (match-subscriber new id-type) subscriptions))
      :addenda
      (notify-addenda (get-pns-from-db (:id new)) new (filter (match-subscriber orig id-type) subscriptions)))))

(defn warning-24hr [pns {:keys [email contact_person] :as sub}]
  (send-message {:to email
                 :from "procurement@cnmipss.org"
                 :subject (str "24-hour Notice: Submissions for "
                               (-> pns :type name clojure.string/upper-case) "# "
                               (:number pns)
                               " are due.")
                 :body [{:type "text/html"
                         :content (html
                                   [:html
                                    [:body
                                     [:p (str "Greetings " contact_person ",")]
                                     [:p (str "We would like to notify you that all submissions for "
                                              (-> pns :type name clojure.string/upper-case) "# "
                                              (:number pns)
                                              " " (:title pns)
                                              " must be turned in to the PSS Procurement office no later than "
                                              (f/unparse (f/formatter "MMMM dd, YYYY 'at' h:mm a") (:close_date pns)))]
                                     [:p "Any submissions turned in after that time will not be considered."]
                                     [:br]
                                     [:p "If you have any questions, please contact Kimo Rosario at kimo.rosario@cnmipss.org"]
                                     [:br]
                                     [:p "Thank you,"]
                                     [:p "Kimo Rosario"]
                                     [:p "Procurement & Supply Officer"]
                                     [:p "CNMI PSS"]]])}]}))
