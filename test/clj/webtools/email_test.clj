(ns webtools.email-test
  (:require [bond.james :refer [calls with-stub! with-spy]]
            [clj-time.core :as t]
            [clojure.test :refer :all]
            [postal.core :refer [send-message]]
            [webtools.db.core :as db]
            [webtools.email :as email :refer [notify-addenda
                                              notify-deletion]]
            [webtools.email.templates :as etemp]
            [webtools.models.procurement.core :as p]
            [webtools.test.fixtures :as fixtures]
            [webtools.test.util :refer [args-from-call count-calls]]
            [webtools.test.constants :as const]
            [webtools.util :as util]
            [webtools.util.dates :as util-dates])
  (:import [org.apache.commons.validator.routines UrlValidator]))

(use-fixtures :once fixtures/prep-db fixtures/instrument)

(use-fixtures :each fixtures/with-rollback)

(defmacro validate-emails
  "Assert that all calls to send-message supplied valid emails as arguments."
  ([calls]
   `(testing "should sent valid html emails"
      (is (every? #(some? (-> % :args first :subject)) ~calls))
      (is (every? #(= "text/html"
                      (-> % :args first :body first :type)) ~calls))
      (is (every? #(= java.lang.String
                      (type (-> % :args first :body first :content))) ~calls))))
  ([calls to from]
   `(do
      (validate-emails ~calls)
      (testing (str "should send emails from " ~from " to all subscribers")
        (is (= (count ~to) (count ~calls)))
        (is (every? #(= ~from (-> % :args first :from)) ~calls))
        (is (every? (fn [a#]
                      (some (fn [b#] (= (-> b# :args first :to) (:email a#)))
                            ~calls))
                    ~to))))))

(defmacro check-unsubscribe-option
  "Test that email included the unsubscribe option"
  []
  `(testing "should offer the option to unsubscribe from further emails"
     (is (= (count-calls send-message)
            (count-calls etemp/unsubscribe-option)))

     (let [unsubscribe-option# (->> (calls send-message)
                                    (map :args)
                                    (map #(-> % first :body first :content))
                                    (map #(re-seq const/unsubscribe-url-re %)))]
       (is (every? some? unsubscribe-option#)))))

(deftest test-invite
  (with-stub! [[send-message (constantly nil)]]
    (let [user {:email "user.john@fake.org"
                :admin false
                :roles "Things,To,Do"}]
      (email/invite user)
      (testing "should send invitation to user's email from webmaster"
        (is (= 1 (count-calls send-message)))
        (is (= "webmaster@cnmipss.org" ((comp :from first) (args-from-call send-message))))
        (is (= "user.john@fake.org" ((comp :to first) (args-from-call send-message)))))

      (validate-emails (calls send-message)))))

(deftest test-confirm-subscription
  (with-spy [etemp/unsubscribe-option]
    (with-stub! [[send-message (constantly nil)]]
      (let [pid (p/make-uuid "9d3ee41e-f79f-4e91-8a9a-535d959ba374")
            subscription (p/map->Subscription
                          {:email               "subscriber@gimmes.pam"
                           :contact_person      "Busy Reader"
                           :company_name        "Reader's Digest"
                           :proc_id             pid
                           :id                  (java.util.UUID/randomUUID)
                           :subscription_number 1
                           :telephone           "789-4561"})
            open_date    (util-dates/parse-date "August 3, 2000")
            close_date   (util-dates/parse-date-at-time "August 3, 2100 at 10:00 pm")
            pns          (p/map->PSAnnouncement
                          {:id pid
                           :type :rfp
                           :number "123"
                           :open_date open_date
                           :close_date close_date
                           :title "Title"
                           :description "D"
                           :file_link "L"
                           :spec_link "S"})]
        (email/confirm-subscription subscription pns)
        (testing "should send a message when called"
          (validate-emails (calls send-message) [subscription] "procurement@cnmipss.org"))
        (testing "should offer the option to unsubscribe from further emails"
          (is (= 1 (count-calls etemp/unsubscribe-option))))
        (check-unsubscribe-option)))))

(deftest test-notify-subscribers
  (testing "should switch on event key and call correct function"
    (with-stub! [[email/notify-changes (constantly nil)]
                 [email/notify-deletion (constantly nil)]
                 [email/notify-addenda (constantly nil)]]
      (let [pid          (p/make-uuid "d0002906-6432-42b5-b82b-35f0d710f827")
            rfp          (p/get-pns-from-db pid)
            new-rfp      (assoc rfp :title "New Title")
            rpid         (p/make-uuid "d2b4e97c-5d7c-4ccd-8fae-a27a27c863e3")
            rfp-addendum (first (db/get-addenda {:proc_id rpid}))
            ipid         (p/make-uuid "5c052995-12c5-4fcc-b57e-bcbf7323f174")
            ifb-addendum (first (db/get-addenda {:proc_id ipid}))]
        (testing ":update event"
          (email/notify-subscribers :update rfp new-rfp)

          (testing "should call notify-changes if record has changed"
            (is (= 1 (count-calls email/notify-changes)))
            (is (= [new-rfp rfp '()]
                   (args-from-call email/notify-changes))))

          (testing "should NOT call notify-changes if record has not changed"
            (email/notify-subscribers :update rfp rfp)

            (is (= 1 (count-calls email/notify-changes)))))

        (testing ":addenda :rfps event"
          (email/notify-subscribers :addenda
                                    rfp-addendum
                                    (p/get-pns-from-db rpid))

          (testing "should call notify-addendum"
            (is (= 1 (count-calls email/notify-addenda)))
            (is (= [(p/get-pns-from-db rpid) rfp-addendum '()]
                   (args-from-call email/notify-addenda)))))

        (testing ":addenda :ifbs event"
          (email/notify-subscribers :addenda
                                    ifb-addendum
                                    (p/get-pns-from-db ipid))

          (testing "should call notify-addendum"
            (is (= 2 (count-calls notify-addenda)))
            (is (= [(p/get-pns-from-db ipid) ifb-addendum '()]
                   (args-from-call notify-addenda 2)))))

        (testing ":delete event"
          (email/notify-subscribers :delete rfp nil)

          (testing "should call notify-deletion if PSAnnouncement is open"
            (is (= 1 (count-calls notify-deletion)))
            (is (= rfp (first (args-from-call notify-deletion))))))))))

(deftest test-notify-changes
  (with-spy [etemp/unsubscribe-option]
    (with-stub! [[send-message (constantly nil)]]
      (let [open_date   (util-dates/parse-date "August 3, 2000")
            close_date  (util-dates/parse-date-at-time "August 3, 2100 at 10:00 pm")
            new_date    (util-dates/parse-date-at-time "August 3, 2100 at 10:00 am")
            pid         (p/make-uuid "9d3ee41e-f79f-4e91-8a9a-535d959ba374")
            original    (p/map->PSAnnouncement
                         {:id          pid
                          :type        :rfp
                          :number      "123"
                          :open_date   open_date
                          :close_date  close_date
                          :title       "New Title"
                          :description "D"
                          :file_link   "L"
                          :spec_link   "S"})
            new-vers    (assoc original :close_date new_date)
            subscribers [(p/map->Subscription {:id                  (java.util.UUID/randomUUID)
                                               :proc_id             pid
                                               :subscription_number 0
                                               :company_name        "Thing 1"
                                               :contact_person      "Thing 2"
                                               :email               "thing2@thing.one"
                                               :telephone           (util/format-tel-num 7894561)})
                         (p/map->Subscription {:id                  (java.util.UUID/randomUUID)
                                               :proc_id             pid
                                               :subscription_number 1
                                               :company_name        "Testers Inc"
                                               :contact_person      "Chief Tester"
                                               :email               "test@11thethi.ngs"
                                               :telephone           (util/format-tel-num 4567891)})]]
        (email/notify-changes new-vers original subscribers)
        (validate-emails (calls send-message) subscribers "procurement@cnmipss.org")

        (testing "should generate correct subject lines"
          (let [{:keys [subject]} (first (args-from-call send-message))]
            (is (= (str "Changes to " (p/title-string original)) subject))))

        (testing "should email every registered subscriber"
          (is (= (count subscribers)
                 (count-calls send-message))))
        (check-unsubscribe-option)))))

(deftest test-notify-addenda
  (with-spy [etemp/unsubscribe-option]
    (with-stub! [[send-message (constantly nil)]]
      (testing "should send notification message of new addenda"
        (let [pid         (p/make-uuid "d2b4e97c-5d7c-4ccd-8fae-a27a27c863e3")
              rfp         (p/get-pns-from-db pid)
              addenda     {:id        "84ee3bd5-9077-449f-a576-08eec5b26028"
                           :proc_id   pid
                           :file_link ""}
              subscribers (p/get-subs-from-db pid)]
          (email/notify-addenda addenda rfp subscribers)
          (validate-emails (calls send-message) subscribers "procurement@cnmipss.org")

          (testing "should generate correct subject lines"
            (let [{:keys [subject]} (first (args-from-call send-message))]
              (is (= (str "Addendum added to " (p/title-string rfp)) subject))))

          (testing "should email every registered subscriber"
            (is (= (count subscribers)
                   (count-calls send-message))))

          (check-unsubscribe-option))))))

(deftest test-notify-deletion
  (with-spy [etemp/unsubscribe-option]
    (with-stub! [[send-message (constantly nil)]]
      (testing "should send notification message if open PSAnnouncement is deleted"
        (let [pid         (p/make-uuid "d2b4e97c-5d7c-4ccd-8fae-a27a27c863e3")
              rfp         (p/get-pns-from-db pid)
              subscribers (p/get-subs-from-db pid)]
          (email/notify-deletion rfp subscribers)
          (validate-emails (calls send-message) subscribers "procurement@cnmipss.org")

          (testing "should generate correct subject lines"
            (let [{:keys [subject]} (first (args-from-call send-message))]
              (is (= (str (p/title-string rfp) " has been DELETED") subject))))

          (testing "should email every registered subscriber"
            (is (= (count subscribers)
                   (count-calls send-message))))

          (check-unsubscribe-option)))))

  (testing "should not send notification message if closed PSAnnouncement is deleted"
    (with-stub! [[send-message (constantly nil)]]
      (let [pid         (p/make-uuid "d2b4e97c-5d7c-4ccd-8fae-a27a27c863e3")
            closed-rfp  (assoc (p/get-pns-from-db pid)
                               :close_date
                               (t/minus (t/now) (t/days 1))) 
            subscribers (p/get-subs-from-db pid)]
        (email/notify-deletion closed-rfp subscribers)
        (validate-emails (calls send-message))
        (is (= 0 (count-calls send-message)))))))

(deftest test-unsubscribe-option
  (let [markup (etemp/unsubscribe-option "8eb76ca0-8751-4561-a9a3-903d93a42f65" :procurement)]
    (testing "should generate hiccup markup"
      (is (vector? markup))
      (is (= :div (first markup))))
    
    (testing "should include valid link to unsubscribe including uuid of subscription record"
      (let [{href :href} (get-in markup [2 2 1])
            url? #(.isValid (UrlValidator.) %)]
        (is (url? href))
        (is (some? (re-seq const/unsubscribe-url-re href))))))

  (testing "should throw an error for null url keys"
    (try
      (let [markup (etemp/unsubscribe-option "test@test.com" :incorrect-key)]
        (is (nil? markup)))
      (catch Exception err
        (let [data (ex-data err)]
          (is (instance? clojure.lang.ExceptionInfo err))
          (is (= IllegalArgumentException (:type data))))))))

(deftest test-notify-procurement
  (with-stub! [[send-message (constantly nil)]
               [etemp/new-subscription-request (constantly "HTML STRING")]]
    (let [pid (p/make-uuid "d2b4e97c-5d7c-4ccd-8fae-a27a27c863e3")
          rfp (p/get-pns-from-db pid)
          sub (p/convert-sub-from-map (first (db/get-subscriptions {:proc_id pid})))]
      (email/notify-procurement sub)
      (validate-emails (calls send-message))
      (is (= 1 (count-calls send-message)))
      (is (= 1 (count-calls etemp/new-subscription-request)))
      (is (= [sub rfp] (args-from-call etemp/new-subscription-request))))))
