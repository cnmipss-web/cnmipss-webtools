(ns webtools.email-test
  (:require [clojure.test :refer :all]
            [webtools.email :as email :refer [notify-changes notify-deletion notify-addenda]]
            [webtools.procurement.core :refer :all]
            [webtools.db.core :as db]
            [webtools.util :as util]
            [webtools.util.dates :as util-dates]
            [webtools.test.fixtures :as fixtures]
            [webtools.spec.internet]
            [bond.james :refer [calls with-spy with-stub!]]
            [postal.core :refer [send-message]]
            [clj-time.core :as t]
            [clj-time.format :as f]))

(use-fixtures :once fixtures/prep-db fixtures/instrument)

(use-fixtures :each fixtures/with-rollback)

(defmacro validate-emails
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

(deftest test-invite
  (with-stub! [[send-message (constantly nil)]]
    (let [user {:email "user@fake.org"
                :admin false
                :roles "Things,To,Do"}]
      (email/invite user)
      (testing "should send invitation to user's email from webmaster"
        (is (= 1 (-> send-message calls count)))
        (is (= "webmaster@cnmipss.org" (-> send-message calls first :args first :from)))
        (is (= "user@fake.org" (-> send-message calls first :args first :to))))

      (validate-emails (-> send-message calls)))))

(deftest test-confirm-subscription
  (with-stub! [[send-message (constantly nil)]
               [email/unsubscribe-option (constantly nil)]]
    (let [subscription (webtools.procurement.core/map->Subscription
                        {:email "subscriber@gimmes.pam"
                         :contact_person "Busy Reader"
                         :company_name "Reader's Digest"
                         :proc_id (make-uuid "9d3ee41e-f79f-4e91-8a9a-535d959ba374")
                         :id (java.util.UUID/randomUUID)
                         :subscription_number 1
                         :telephone "789-4561"})
          open_date (util-dates/parse-date "August 3, 2000")
          close_date (util-dates/parse-date-at-time "August 3, 2100 at 10:00 pm")
          pns (webtools.procurement.core.PSAnnouncement. (make-uuid "9d3ee41e-f79f-4e91-8a9a-535d959ba374")
                                                         :rfp "123" open_date close_date
                                                         "Title" "D" "L" "S")]
      (email/confirm-subscription subscription pns)
      (testing "should send a message when called"
        (validate-emails (-> send-message calls) [subscription] "procurement@cnmipss.org"))
      (testing "should offer the option to unsubscribe from further emails"
        (is (= 1 (count (calls email/unsubscribe-option))))))))

(deftest test-notify-subscribers
  (testing "should switch on event key and call correct function"
    (with-stub! [[email/notify-changes (constantly nil)]
                 [email/notify-deletion (constantly nil)]
                 [email/notify-addenda (constantly nil)]]
      (let [rfp (get-pns-from-db (make-uuid "d0002906-6432-42b5-b82b-35f0d710f827"))
            new-rfp (assoc rfp :title "New Title")
            rfp-addendum (db/get-addenda {:proc_id (make-uuid "d2b4e97c-5d7c-4ccd-8fae-a27a27c863e3")})
            ifb-addendum (db/get-addenda {:proc_id (make-uuid "5c052995-12c5-4fcc-b57e-bcbf7323f174")})]
        (testing ":update event"
          (email/notify-subscribers :update rfp new-rfp)

          (testing "should call notify-changes if record has changed"
            (is (= 1 (-> email/notify-changes calls count)))
            (is (= new-rfp (-> email/notify-changes calls first :args first)))
            (is (= rfp (-> email/notify-changes calls first :args second))))

          (testing "should NOT call notify-changes if record has not changed"
            (email/notify-subscribers :update rfp rfp)

            (is (= 1 (-> email/notify-changes calls count)))))

        (testing ":addenda :rfps event"
          (email/notify-subscribers :addenda
                                    (first rfp-addendum)
                                    (get-pns-from-db (make-uuid "d2b4e97c-5d7c-4ccd-8fae-a27a27c863e3")))

          (testing "should call notify-addendum"
            (is (= 1 (-> email/notify-addenda calls count)))
            (is (= (-> rfp-addendum first :proc_id get-pns-from-db) (-> email/notify-addenda calls first :args first)))
            (is (= (first rfp-addendum) (-> notify-addenda calls first :args second)))))

        (testing ":addenda :ifbs event"
          (email/notify-subscribers :addenda
                                    (first ifb-addendum)
                                    (get-pns-from-db (make-uuid "5c052995-12c5-4fcc-b57e-bcbf7323f174")))

          (testing "should call notify-addendum"
            (is (= 2 (-> notify-addenda calls count)))
            (is (= (-> ifb-addendum first :proc_id get-pns-from-db) (-> notify-addenda calls second :args first)))
            (is (= (first ifb-addendum) (-> notify-addenda calls second :args second)))))

        (testing ":delete event"
          (email/notify-subscribers :delete rfp nil)

          (testing "should call notify-deletion if PSAnnouncement is open"
            (is (= 1 (-> notify-deletion calls count)))
            (is (= rfp (-> notify-deletion calls first :args first)))))))))

(deftest test-notify-changes
  (with-stub! [[send-message (constantly nil)]
               [email/unsubscribe-option (constantly "UNSUBSCRIBE STRING")]]
    (let [open_date (util-dates/parse-date "August 3, 2000")
          close_date (util-dates/parse-date-at-time "August 3, 2100 at 10:00 pm")
          new_date (util-dates/parse-date-at-time "August 3, 2100 at 10:00 am")
          original (->PSAnnouncement
                    (make-uuid "9d3ee41e-f79f-4e91-8a9a-535d959ba374")
                    :rfp "123" open_date close_date
                    "Title" "D" "L" "S")
          new-vers (->PSAnnouncement
                    (make-uuid "9d3ee41e-f79f-4e91-8a9a-535d959ba374")
                    :rfp "123" open_date new_date
                    "New Title" "D" "L" "S")
          subscribers [(map->Subscription {:id (java.util.UUID/randomUUID)
                                           :proc_id (make-uuid "9d3ee41e-f79f-4e91-8a9a-535d959ba374")
                                           :subscription_number 0
                                           :company_name "Thing 1"
                                           :contact_person "Thing 2"
                                           :email "thing2@thing.one"
                                           :telephone (util/format-tel-num 7894561)})
                       (map->Subscription {:id (java.util.UUID/randomUUID)
                                           :proc_id (make-uuid "9d3ee41e-f79f-4e91-8a9a-535d959ba374")
                                           :subscription_number 1
                                           :company_name "Testers Inc"
                                           :contact_person "Chief Tester"
                                           :email "test@11thethi.ngs"
                                           :telephone (util/format-tel-num 4567891)})]]
      (notify-changes new-vers original subscribers)
      (validate-emails (-> send-message calls) subscribers "procurement@cnmipss.org")

      (testing "should offer the option to unsubscribe from further emails"
          (is (= (count subscribers) (count (calls email/unsubscribe-option))))

          (let [unsubscribe-option (->> send-message
                                        calls
                                        (map :args)
                                        (map #(-> % first :body first :content))
                                        (map #(re-seq #"UNSUBSCRIBE STRING</body>" %)))]
            (is (every? some? unsubscribe-option)))))))

(deftest test-notify-addenda
  (testing "should send notification message of new addenda"
    (with-stub! [[send-message (constantly nil)]
                 [email/unsubscribe-option (constantly "UNSUBSCRIBE STRING")]]
      (let [proc-id (make-uuid "d2b4e97c-5d7c-4ccd-8fae-a27a27c863e3")
            rfp (get-pns-from-db proc-id)
            addenda {:id "84ee3bd5-9077-449f-a576-08eec5b26028"
                     :proc_id proc-id
                     :file_link ""}
            subscribers (db/get-subscriptions {:proc_id proc-id})]
        (notify-addenda addenda rfp subscribers)
        (validate-emails (-> send-message calls) subscribers "procurement@cnmipss.org")

        (testing "should generate correct subject lines"
          (let [subject (->> send-message calls first :args first :subject)]
            (is (= "Addendum added to RFP# 17-015 Test Request for Proposal #3" subject))))

        (testing "should offer the option to unsubscribe from further emails"
          (is (= (count subscribers) (count (calls email/unsubscribe-option))))

          (let [unsubscribe-option (->> send-message
                                        calls
                                        (map :args)
                                        (map #(-> % first :body first :content))
                                        (map #(re-seq #"UNSUBSCRIBE STRING</body>" %)))]
            (is (every? some? unsubscribe-option))))))))

(deftest test-notify-deletion
  (testing "should send notification message if open PSAnnouncement is deleted"
    (with-stub! [[send-message (constantly nil)]
                 [email/unsubscribe-option (constantly "UNSUBSCRIBE STRING")]]
      (let [rfp (-> (make-uuid "d2b4e97c-5d7c-4ccd-8fae-a27a27c863e3")
                    (get-pns-from-db)
                    (util/make-status))
            subscribers (db/get-subscriptions {:proc_id (make-uuid "d2b4e97c-5d7c-4ccd-8fae-a27a27c863e3")})]
        (notify-deletion rfp subscribers)
        (validate-emails (-> send-message calls) subscribers "procurement@cnmipss.org")

        (testing "should generate correct subject lines"
          (let [subject (->> send-message calls first :args first :subject)]
            (is (= "RFP# 17-015 Test Request for Proposal #3 has been DELETED" subject))))

        (testing "should offer the option to unsubscribe from further emails"
          (is (= (count subscribers) (count (calls email/unsubscribe-option))))

          (let [unsubscribe-option (->> send-message
                                        calls
                                        (map :args)
                                        (map #(-> % first :body first :content))
                                        (map #(re-seq #"UNSUBSCRIBE STRING</body>" %)))]
            (is (every? some? unsubscribe-option)))))))

  (testing "should not send notification message if closed PSAnnouncement is deleted"
    (with-stub! [[send-message (constantly nil)]]
      (let [rfp (-> (make-uuid "d2b4e97c-5d7c-4ccd-8fae-a27a27c863e3")
                    (get-pns-from-db)
                    (util/make-status))
            subscribers (db/get-subscriptions {:proc_id (make-uuid "d2b4e97c-5d7c-4ccd-8fae-a27a27c863e3")})]
        (notify-deletion (assoc rfp :close_date (t/minus (t/now) (t/days 1))) subscribers)
        (validate-emails (-> send-message calls))
        (is (= 0 (-> send-message calls count)))))))

(deftest test-unsubscribe-option
  (testing "should generate hiccup markup"
    (let [markup (email/unsubscribe-option "test@test.com" :procurement)]
      (is (vector? markup))
      (is (= :div (first markup)))))

  (testing "should throw an error for null url keys"
    (try
      (let [markup (email/unsubscribe-option "test@test.com" :incorrect-key)]
        (is (nil? markup)))
      (catch Exception err
        (is (instance? NullPointerException err))))))
