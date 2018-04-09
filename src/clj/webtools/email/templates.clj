(ns webtools.email.templates
  (:require [clj-time.core :as time]
            [clojure.string :as cstr]
            [hiccup.core :refer [html]]
            [webtools.config :refer [env]]
            [webtools.exceptions :as w-ex]
            [webtools.models.procurement.core :refer :all]
            [webtools.util :as util]
            [webtools.util.dates :as util-dates]
            [webtools.db.core :as db])) 

(defn unsubscribe-option [id k]
  (let [routes {:procurement (str "https://cnmipss.org/webtools/api/unsubscribe-procurement/"
                                  id)}]
    (if (some? (get routes k))
      [:div
       [:a {:href (get routes k)} "Unsubscribe"]]
      (throw (w-ex/illegal-argument
              {:cause (IllegalArgumentException.
                       (str "No such link in webtools.email/unsubscribe-option for key " k))
               :data {:call `(unsubscribe-option ~id ~k)
                      :valid-options routes}})))))

(defn invitation [{:keys [email roles admin] :as user}]
  (let [{:keys
         [wp-host
          wp-un
          server-uri]} env
        name           (util/email->name email)]
    (html
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
                       (cstr/split #",")
                       (#(conj (vec (butlast %)) (str "and " (last %))))
                       (#(cstr/join #", " %))
                       (cstr/replace #"^and " ""))
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
         "tyler.collins@cnmipss.org"]]]])))

(defn confirm-subscription [{:keys [id contact_person company_name] :as subscription} pns]
  (html
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
     (unsubscribe-option id :procurement)]]))

(defn notify-changes [new orig {email :email
                                id :id
                                :as sub}]
  (html [:html
         (conj (changes-email orig new sub)
               (unsubscribe-option id :procurement))]))

(defn notify-deletion [pns {:keys [id contact_person] :as sub}]
  (html
   [:html
    [:body
     [:p (str "Greetings " contact_person ",")]
     [:p (str "We would like to notify you that "
              (title-string pns)
              " has been withdrawn from the CNMI PSS website along with any addenda or other documentation.  You will not receive any further emails regarding this matter.")]
     [:br]
     [:p "If you have any questions, please contact Kimo Rosario at kimo.rosario@cnmipss.org"]
     [:br]
     [:p "Thank you,"]
     [:p "Kimo Rosario"]
     [:p "Procurement & Supply Officer"]
     [:p "CNMI PSS"]
     (unsubscribe-option id :procurement)]]))

(defn notify-addenda [addendum pns {:keys [id contact_person]}]
  (let [title (title-string pns)]
    (html
     [:html
      [:body
       [:p (str "Greetings " contact_person ",")]
       [:p (str "We would like to notify you that an addendum has been added to "
                title
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
       (unsubscribe-option id :procurement)]])))

(defn warning-24hr [pns contact email]
  (let [db-sub (first (db/get-users-subscription {:email email :proc_id (:id pns)}))]
    (html
     [:html
      [:body
       [:p (str "Greetings " contact ",")]
       [:p (str "We would like to notify you that all submissions for "
                (-> pns :type name cstr/upper-case) "# "
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
       (unsubscribe-option (:id db-sub) :procurement)]])))

(defn notify-closed [pns contact email]
  (let [db-sub (first (db/get-users-subscription {:email email :proc_id (:id pns)}))]
    (html
     [:html
      [:body
       [:p (str "Greetings " contact ",")]
       [:p (str "We would like to notify you that the deadline to submit a response for "
                (-> pns :type name cstr/upper-case) "# "
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
       (unsubscribe-option (:id db-sub) :procurement)]])))

(defn error-alert
  [exception]
  (let [ex-string (as-> (w-ex/exception->string exception) ex
                    (cstr/split ex #"\n")
                    (cstr/join "<br />&emsp;&emsp;" ex))]
    (html
     [:html
      [:body
       [:p
        "An error as occurred in the running instance of CNMI PSS Webtools on "
        (util-dates/print-date-at-time
         (time/from-time-zone
          (time/now)
          (time/time-zone-for-offset +10)))
        " HST."]
       [:p ex-string]]])))

(defn new-subscription-request
  [{:keys [company_name contact_person email telephone] :as sub}
   {:keys [type number title] :as pns}]
  (let [title-str (title-string pns)]
    (html
     [:html
      [:body
       [:p (str contact_person " at " company_name
                " has subscribed to receive more information regarding "
                title-str)]
       [:p (str "They may be contact by email at " email
                " or by telephone at " (util/format-tel-num telephone))]
       [:p (str "They will be automatically notified by email regarding any changes or updates made to "
                title-str " via CNMI PSS Webtools.")]]])))
