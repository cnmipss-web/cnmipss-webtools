(ns webtools.email
  (:require
   [clj-time.coerce :as c]
   [clj-time.format :as f]
   [clojure.data :refer [diff]]
   [clojure.spec.alpha :as spec]
   [clojure.string :as cstr]
   [clojure.tools.logging :as log]
   [hiccup.core :refer [html]]
   [postal.core :refer [send-message]]
   [webtools.config :refer [env]]
   [webtools.db.core :as db]
   [webtools.email.templates :as templates]
   [webtools.exceptions :as w-ex]
   [webtools.models.procurement.core :as p]
   [webtools.models.procurement.server :refer :all]
   [webtools.spec.procurement]
   [webtools.spec.subscription]
   [webtools.util :as util]
   [webtools.util.dates :as util-dates]))

(def ^:private webmaster-email "webmaster@cnmipss.org")
(def ^:private procurement-email "procurement@cnmipss.org")

(defn alert-error [ex]
  (try
    (send-message {:to webmaster-email
                   :from webmaster-email
                   :subject (str "CNMI PSS Webtools Encountered an Error: "
                                 (.getMessage ex))
                   :body [{:type "text/html"
                           :content (templates/error-alert ex)}]})
    (catch Exception e
      (log/error e))))

(defn invite [user]
  (let [{:keys [email
                roles
                admin]}      user
        {:keys [wp-host
                wp-un
                server-uri]} env
        name                 (->> email
                                  (re-find #"^(.*?)\.(.*)@")
                                  (drop 1)
                                  (map cstr/capitalize)
                                  (cstr/join " "))]
    (try
      (send-message {:from    webmaster-email
                     :to      email
                     :subject "Invitation to CNMI PSS Webtools"
                     :body    [{:type    "text/html"
                                :content (templates/invitation user)}]})
      (catch Exception ex
        (alert-error ex)
        (log/error ex)))))


(defn confirm-subscription [subscription pns]
  (let [{:keys [email contact_person company_name]} subscription]
    (try
      (send-message {:from    procurement-email
                     :to      email
                     :subject "Subscription Confirmed"
                     :body    [{:type    "text/html"
                                :content (templates/confirm-subscription subscription pns)}]})
      (catch Exception ex
        (alert-error ex)
        (log/error ex)))))

(spec/fdef confirm-subscription
           :args (spec/cat :subscription :webtools.spec.subscription/record
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
  (let [title-string (str (-> new :type name cstr/upper-case)
                          "# " (:number orig) " " (:title orig))
        send-fn
        (fn [{:keys [email contact_person] :as sub}]
          (send-message {:to email
                         :from procurement-email
                         :subject (str "Changes to " title-string)
                         :body [{:type "text/html"
                                 :content (templates/notify-changes new orig sub)}]}))]
    (mapv send-fn subscribers)))

(spec/fdef notify-changes
           :args (spec/cat :new :webtools.spec.procurement/record
                           :orig :webtools.spec.procurement/record
                           :subscribers (spec/coll-of :webtools.spec.subscription/record))
           :ret nil?)

(defn id-key [k]
  (->> k name drop-last (apply str) (#(str % "_id")) keyword))

(defn match-subscriber
  [pns]
  (fn [subscriber]
    (= (:id pns) (-> subscriber :proc_id p/make-uuid))))

(defn notify-deletion [{:keys [number title] :as pns} subscribers]
  (log/info "Deleting: " pns subscribers)
  (if-let [not-closed? (:status (util/make-status pns))]
    (let [title-string (str (-> pns :type name cstr/upper-case) "# " number " " title)
          send-fn
          (fn [{:keys [email contact_person] :as sub}]
            (try
              (send-message
               {:to email
                :from procurement-email
                :subject (str title-string " has been DELETED")
                :body [{:type "text/html"
                        :content (templates/notify-deletion title-string email contact_person)}]})
              (catch Exception ex
                (alert-error ex)
                (log/error ex))))]
      (mapv send-fn subscribers))
    (log/info "PSAnnouncement already closed, skipping email notifications")))

(defn notify-addenda [addendum pns subscribers]
  (log/info "Notify-Addenda:\n\nPNS: " pns "\n\nAdd: " addendum "\n\nSubs: " subscribers)
  (let [title-string (str (-> pns :type name cstr/upper-case) "# " (:number pns) " " (:title pns))
        send-fn
        (fn [{:keys [email contact_person] :as sub}]
          (try
            (send-message
             {:to email
              :from procurement-email
              :subject (str "Addendum added to " title-string)
              :body [{:type "text/html"
                      :content (templates/notify-addenda addendum title-string contact_person email)}]})
            (catch Exception ex
              (alert-error ex)
              (log/error ex))))]
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
                                 (p/title-string pns)
                                 " are due.")
                   :body [{:type "text/html"
                           :content (templates/warning-24hr pns contact_person email)}]})
    (catch Exception ex
      (alert-error ex)
      (log/error ex))))

(spec/fdef warning-24hr
           :args (spec/cat :pns :webtools.spec.procurement/record
                           :sub :webtools.spec.subscription/record)
           :ret nil?)

(defn notify-pns-closed [pns {:keys [email contact_person :as sub]}]
  (try
    (send-message {:to email
                   :from procurement-email
                   :subject (str "CLOSED: Deadline for submissions for "
                                 (p/title-string pns)
                                 " has passed.")
                   :body [{:type "text/html"
                           :content (templates/notify-closed pns contact_person email)}]})
    (catch Exception ex
      (alert-error ex)
      (log/error ex))))

(defn notify-procurement [{:keys [company_name proc_id] :as sub}]
  (doseq [user (db/get-proc-users)]
    (let [pns (p/convert-pns-from-map (db/get-single-pnsa {:id proc_id}))
          tstr (p/title-string pns)]
      (try
        (send-message {:to (:email user)
                       :from "no-reply@cnmipss.org"
                       :subject (str company_name " has requested information regarding " tstr)
                       :body [{:type "text/html"
                               :content (templates/new-subscription-request sub pns)}]})
        (catch Exception ex
          (alert-error ex)
          (log/error ex))))))

(spec/fdef notify-procurement
           :args (spec/cat :sub :webtools.spec.subscription/record)
           :ret  nil?)
