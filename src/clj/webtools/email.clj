(ns webtools.email
  (:require [webtools.config :refer [env]]
            [webtools.db.core :as db]
            [webtools.procurement.core :refer :all]
            [webtools.procurement.server :refer :all]
            [webtools.util :as util]
            [webtools.util.dates :as util-dates]
            [clj-time.coerce :as c]
            [clj-time.format :as f]
            [postal.core :refer [send-message]]
            [hiccup.core :refer [html]]
            [clojure.tools.logging :as log]
            [clojure.data :refer [diff]]
            [clojure.spec.alpha :as s]
            [webtools.spec.procurement]
            [webtools.spec.subscription]))

(def ^:private webmaster-email "webmaster@cnmipss.org")
(def ^:private procurement-email "procurement@cnmipss.org")

(defn unsubscribe-option [email k]
  (let [routes {:procurement "https://cnmipss.org/webtools/api/procurement-unsubscribe"}]
    (if (some? (get routes k))
      [:div
       [:a {:href (get routes k)} "Unsubscribe"]]
      (throw (NullPointerException. (str "Null URL used in webtools.email/unsubscribe-option for key " k))))))

(s/fdef unsubscribe-option
        :args (s/cat :email :webtools.spec.internet/email-address
                     :key keyword?)
        :ret vector?)

(defn invite [user]
  (let [{:keys [email roles admin]} user
        name (->> email
                  (re-find #"^(.*?)\.(.*)@")
                  (drop 1)
                  (map clojure.string/capitalize)
                  (clojure.string/join " "))
        {:keys [wp-host wp-un server-uri]} env]
    (try
      (log/info (str "Inviting new user: " name " with "
                     (if (> (count roles) 0)
                       (str roles " roles " (if admin (str "and admin access")))
                       (if admin ("admin access.")))))
      (send-message {:from webmaster-email
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
                                            "tyler.collins@cnmipss.org"]]]])}]})
      (catch Exception e
        (log/error e)))))


(defn confirm-subscription [subscription pns]
  (let [{:keys [email contact_person company_name]} subscription]
    (try
      (send-message {:from procurement-email
                     :to email
                     :subject "Subscription Confirmed"
                     :body [{:type "text/html"
                             :content (html
                                        [:html
                                         [:body
                                          [:p (str "Greetings " contact_person ",")]
                                          [:p (str "This email is your confirmation that you have registered to receive updates and information regarding "
                                                   (case (:type pns)
                                                     :rfp "Request for Proposal "
                                                     :ifb "Invitation for Bid ")
                                                   ": " (:title pns) ".")]
                                          [:p (str "You will be contacted as additional information is published.  If you have any questions, please contact Kimo Rosario at kimo.rosario@cnmipss.org")]
                                          [:br]
                                          [:p "Thank you,"]
                                          [:p "Kimo Rosario"]
                                          [:p "Procurement & Supply Officer"]
                                          [:p "CNMI PSS"]
                                          (unsubscribe-option email :procurement)]])}]})
      (catch Exception e
        (log/error e)))))

(s/fdef confirm-subscription
        :args (s/cat :subscription :webtools.spec.subscription/record
                     :pns :webtools.spec.procurement/record)
        :ret nil?)

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

(defn notify-changes [new orig subscribers]
  (log/info "Notifying subscribers of PSAnnouncement Changes:" new orig subscribers)
  (let [title-string (str (-> new :type name clojure.string/upper-case)
                          "# " (:number orig) " " (:title orig))
        send-fn
        (fn [{:keys [email contact_person] :as sub}]
          (send-message {:to email
                         :from procurement-email
                         :subject (str "Changes to " title-string)
                         :body [{:type "text/html"
                                 :content (html [:html
                                                 (conj (changes-email orig new sub)
                                                       (unsubscribe-option email :procurement))])}]}))]
    (mapv send-fn subscribers)))

(s/fdef notify-changes
        :args (s/cat :new :webtools.spec.procurement/record
                     :orig :webtools.spec.procurement/record
                     :subscribers (s/coll-of :webtools.spec.subscription/record))
        :ret nil?)

(defn id-key [k]
  (->> k name drop-last (apply str) (#(str % "_id")) keyword))

(defn match-subscriber
  [pns]
  (fn [subscriber]
    (= (:id pns) (-> subscriber :proc_id make-uuid))))

(defn notify-deletion [{:keys [number title] :as pns} subscribers]
  (log/info "Deleting: " pns subscribers)
  (if-let [not-closed? (:status (util/make-status pns))]
    (let [title-string (str (-> pns :type name clojure.string/upper-case) "# " number " " title)
          send-fn
          (fn [{:keys [email contact_person] :as sub}]
            (try
              (send-message
                {:to email
                 :from procurement-email
                 :subject (str title-string " has been DELETED")
                 :body [{:type "text/html"
                         :content (html
                                    [:html
                                     [:body
                                      [:p (str "Greetings " contact_person ",")]
                                      [:p (str "We would like to notify you that "
                                               title-string
                                               " has been withdrawn from the CNMI PSS website along with any addenda or other documentation.  You will not receive any further emails regarding this matter.")]
                                      [:br]
                                      [:p "If you have any questions, please contact Kimo Rosario at kimo.rosario@cnmipss.org"]
                                      [:br]
                                      [:p "Thank you,"]
                                      [:p "Kimo Rosario"]
                                      [:p "Procurement & Supply Officer"]
                                      [:p "CNMI PSS"]
                                      (unsubscribe-option email :procurement)]])}]})
              (catch Exception e
                (log/error e))))]
      (mapv send-fn subscribers))
    (log/info "PSAnnouncement already closed, skipping email notifications")))

(defn notify-addenda [addendum pns subscribers]
  (log/info "Notify-Addenda:\n\nPNS: " pns "\n\nAdd: " addendum "\n\nSubs: " subscribers)
  (let [title-string (str (-> pns :type name clojure.string/upper-case) "# " (:number pns) " " (:title pns))
        send-fn
        (fn [{:keys [email contact_person] :as sub}]
          (try
            (send-message
              {:to email
               :from procurement-email
               :subject (str "Addendum added to " title-string)
               :body [{:type "text/html"
                       :content (html
                                  [:html
                                   [:body
                                    [:p (str "Greetings " contact_person ",")]
                                    [:p (str "We would like to notify you that an addendum has been added to "
                                             title-string
                                             ".  You may access the full content of this addendum through ")
                                     [:a {:href (:file_link addendum)}
                                      "this link."]]
                                    [:br]
                                    [:p "If you have any questions, please contact Kimo Rosario at kimo.rosario@cnmipss.org"]
                                    [:br]
                                    [:p "Thank you,"]
                                    [:p "Kimo Rosario"]
                                    [:p "Procurement & Supply Officer"]
                                    [:p "CNMI PSS"]
                                    (unsubscribe-option email :procurement)]])}]})
            (catch Exception e
              (log/error e))))]
    (mapv send-fn subscribers)))

(defn notify-subscribers [event orig new]
  (let [subscriptions (db/get-all-subscriptions)
        addenda (db/get-all-addenda)
        changes (take 2 (diff new orig))]
    (case event
      :update
      (when (every? some? changes)
        (notify-changes new orig (filter (match-subscriber new) subscriptions)))
      :delete
      (notify-deletion orig (filter (match-subscriber new) subscriptions))
      :addenda
      (notify-addenda new orig (filter (match-subscriber orig) subscriptions)))))


(defn warning-24hr [pns {:keys [email contact_person] :as sub}]
  (try
    (send-message {:to email
                   :from procurement-email
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
                                                 (util-dates/print-date-at-time (:close_date pns)))]
                                        [:p "Any submissions turned in after that time will not be considered."]
                                        [:br]
                                        [:p "If you have any questions, please contact Kimo Rosario at kimo.rosario@cnmipss.org"]
                                        [:br]
                                        [:p "Thank you,"]
                                        [:p "Kimo Rosario"]
                                        [:p "Procurement & Supply Officer"]
                                        [:p "CNMI PSS"]
                                        (unsubscribe-option email :procurement)]])}]})
    (catch Exception e
      (log/error e))))

(s/fdef warning-24hr
        :args (s/cat :pns :webtools.spec.procurement/record
                     :sub :webtools.spec.subscription/record)
        :ret nil?)

(defn notify-pns-closed [pns {:keys [email contact_person :as sub]}]
  (try
    (send-message {:to email
                   :from procurement-email
                   :subject (str "CLOSED: Deadline for submissions for "
                                 (-> pns :type name clojure.string/upper-case) "# "
                                 (:number pns)
                                 " has passed.")
                   :body [{:type "text/html"
                           :content (html
                                     [:html
                                      [:body
                                       [:p (str "Greetings " contact_person ",")]
                                       [:p (str "We would like to notify you that the deadline to submit a response for "
                                                (-> pns :type name clojure.string/upper-case) "# "
                                                (:number pns)
                                                " " (:title pns)
                                                " has passed as of "
                                                (util-dates/print-date-at-time (:close_date pns))
                                                ".  No further submissions will be accepted after this time.")]
                                       [:br]
                                       [:p "If you have any questions, please contact Kimo Rosario at kimo.rosario@cnmipss.org"]
                                       [:br]
                                       [:p "Thank you,"]
                                       [:p "Kimo Rosario"]
                                       [:p "Procurement & Supply Officer"]
                                       [:p "CNMI PSS"]
                                       (unsubscribe-option email :procurement)]])}]})
    (catch Exception e
      (log/error e))))

(defn notify-procurement [{:keys [company_name proc_id] :as sub} pns]
  (doseq [user (db/get-proc-users)]
    (let [tstr (p/title-string pns)]
      (try
        (send-message {:to (:email user)
                       :from "no-reply@cnmipss.org"
                       :subject (str company_name " has requested information regarding " title-string)
                       :body [{:type "text/html"
                               :content (html
                                         [:html
                                          [:body
                                           [:p (str contact_person " at " company_name
                                                    " has subscribed to receive more information regarding "
                                                    title-string)]
                                           [:p (str "They may be contact by email at " email
                                                    " or by telephone at " (util/format-tel-num telephone))]
                                           [:p (str "They will be automatically notified by email regarding any changes or updates made to "
                                                    title-string " via CNMI PSS Webtools.")]]])}]})
        (catch Exception e
          (log/error e))))))
