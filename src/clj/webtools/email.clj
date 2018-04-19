(ns webtools.email
  (:require [clojure.data :refer [diff]]
            [clojure.spec.alpha :as spec]
            [clojure.tools.logging :as log]
            [postal.core :refer [send-message]]
            [webtools.db :as db]
            [webtools.email.templates :as templates]
            [webtools.exceptions :as w-ex]
            [webtools.models.procurement.core :as p]
            [webtools.util :as util]))

(def ^:private webmaster-email "webmaster@cnmipss.org")
(def ^:private procurement-email "procurement@cnmipss.org")

(defn alert-error
  "Alert administrator by email of runtime exceptions."
  [ex]
  (try
    (send-message {:to webmaster-email
                   :from webmaster-email
                   :subject (str "CNMI PSS Webtools Encountered an Error: "
                                 (.getMessage ex))
                   :body [{:type "text/html"
                           :content (templates/error-alert ex)}]})
    (catch Exception e
      (log/error e))))

(spec/fdef alert-error
           :args (spec/cat :exception :webtools.spec.core/throwable?))

(defn invite
  "Send an invitation email to a new user."
  [{:keys [email] :as user}]
  (try
    (send-message {:from    webmaster-email
                   :to      email
                   :subject "Invitation to CNMI PSS Webtools"
                   :body    [{:type    "text/html"
                              :content (templates/invitation user)}]})
    (catch Exception ex
      (alert-error ex)
      (log/error ex))))

(spec/fdef invite
           :args (spec/cat :user :webtools.spec.user/record))


(defn confirm-subscription
  "Send a confirmation email to a new pns subscriber."
  [{:keys [email] :as sub} pns]
  (try
    (send-message {:from    procurement-email
                   :to      email
                   :subject "Subscription Confirmed"
                   :body    [{:type    "text/html"
                              :content (templates/confirm-subscription sub pns)}]})
    (catch Exception ex
      (alert-error ex)
      (log/error ex))))

(spec/fdef confirm-subscription
           :args (spec/cat :sub :webtools.spec.subscription/record
                           :pns :webtools.spec.procurement/record)
           :ret nil?)

(defn notify-changes [new orig subscribers]
  (log/info "Notifying subscribers of PSAnnouncement Changes:" new orig subscribers)
  (try
    (let [send-fn (fn [{:keys [email] :as sub}]
                    (send-message {:to email
                                   :from procurement-email
                                   :subject (str "Changes to " (p/title-string orig))
                                   :body [{:type "text/html"
                                           :content (templates/notify-changes new orig sub)}]}))]
      (doseq [sub subscribers]
        (send-fn sub)))
    (catch Exception ex
      (alert-error ex)
      (log/error ex))))

(spec/fdef notify-changes
           :args (spec/cat :new :webtools.spec.procurement/record
                           :orig :webtools.spec.procurement/record
                           :subscribers (spec/coll-of :webtools.spec.subscription/record))
           :ret nil?)

(defn- match-subscriber
  [{:keys [id]}]
  (fn [{:keys [proc_id]}]
    (= (p/make-uuid id) (p/make-uuid proc_id))))

(spec/fdef match-subscriber
           :args (spec/cat :map (spec/keys :req-un [:webtools.spec.procurement/id])))

(defn notify-deletion [{:keys [number title] :as pns} subscribers]
  (log/info "Deleting: " pns subscribers)
  (if-let [not-closed? (:status (util/make-status pns))]
    (try
      (let [send-fn (fn [{:keys [email] :as sub}]
                      (send-message
                       {:to email
                        :from procurement-email
                        :subject (str (p/title-string pns) " has been DELETED")
                        :body [{:type "text/html"
                                :content (templates/notify-deletion pns sub)}]}))]
        (doseq [sub subscribers]
          (send-fn sub)))
      (catch Exception ex
        (alert-error ex)
        (log/error ex)))
    (log/info "PSAnnouncement already closed, skipping email notifications")))

(spec/fdef notify-deletion
           :args (spec/cat :pns :webtools.spec.procurement/record
                           :subs (spec/coll-of :webtools.spec.subscription/record)))

(defn notify-addenda [addendum pns subscribers]
  (try
    (let [send-fn (fn [{:keys [email] :as sub}]
                    (send-message
                     {:to      email
                      :from    procurement-email
                      :subject (str "Addendum added to " (p/title-string pns))
                      :body    [{:type    "text/html"
                                 :content (templates/notify-addenda addendum pns sub)}]}))]
      (doseq [sub subscribers]
        (send-fn sub)))
    (catch Exception ex
      (alert-error ex)
      (log/error ex))))

(spec/fdef notify-addenda
           :args (spec/cat :addendum map?
                           :pns :webtools.spec.procurement/record
                           :subscribers (spec/coll-of :webtools.spec.subscription/record)))

(defn notify-subscribers [event orig new]
  (let [subscriptions (db/get-all-subscriptions)
        addenda       (db/get-all-addenda)
        changes       (take 2 (diff new orig))]
    (case event
      :update
      (when (every? some? changes)
        (notify-changes new orig (filter (match-subscriber new) subscriptions)))

      :delete
      (notify-deletion orig (filter (match-subscriber orig) subscriptions))

      :addenda
      (notify-addenda new orig (filter (match-subscriber orig) subscriptions))

      (throw (w-ex/illegal-argument {:msg  (str "Invalid event value: " event)
                                     :data {:call [#'notify-subscribers event orig new]}})))))

(spec/fdef notify-subscribers
           :args (spec/cat :event #{:update :delete :addenda}
                           :orig (spec/alt :pns :webtools.spec.procurement/record
                                           :addendum map?)
                           :new (spec/nilable :webtools.spec.procurement/record)))


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
                           :sub :webtools.spec.subscription/record))

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

(spec/fdef notify-pns-closed
           :args (spec/cat :pns :webtools.spec.procurement/record
                           :sub :webtools.spec.subscription/record))

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
           :args (spec/cat :sub :webtools.spec.subscription/record))
