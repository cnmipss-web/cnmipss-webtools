(ns webtools.cron.jobs-test
  (:require [clojure.test :refer :all]
            [clj-time.core :as t]
            [webtools.cron.jobs :as jobs]
            [webtools.email :as email]
            [webtools.procurement :as p]
            [webtools.db.core :as db]
            [webtools.test.fixtures :as fixtures]
            [bond.james :refer [calls with-spy with-stub with-stub!]]))

(use-fixtures :once fixtures/prep-db)
(use-fixtures :each fixtures/with-rollback)

(deftest test-check-notify-subscribers-24hr
  (with-stub [[email/warning-24hr (constantly nil)]
              [db/get-all-rfps (constantly [{:id (java.util.UUID/randomUUID)
                                             :close_date (t/plus (t/now) (t/hours 23) (t/minutes 30))
                                             :open_date (t/minus (t/now) (t/hours 23) (t/minutes 30))
                                             :type :rfp
                                             :number ""
                                             :title ""
                                             :description ""
                                             :file_link ""}])]
              [db/get-all-ifbs (constantly [{:id (java.util.UUID/randomUUID)
                                             :close_date (t/plus (t/now) (t/hours 230) (t/minutes 30))
                                             :open_date (t/minus (t/now) (t/hours 23) (t/minutes 30))
                                             :type :ifb
                                             :number ""
                                             :title ""
                                             :description ""
                                             :file_link ""}])]
              [db/get-subscriptions (fn [x]
                                      (if (instance? java.util.UUID (:rfp_id x))
                                        [true true true]
                                        []))]]
    (with-spy [t/within?]
      (jobs/check-notify-subscribers-24hr)
      (testing "should call email/warning-24hr only for every subscriber only for expiring announcements"
        (is (= 2 (-> t/within? calls count)))
        (is (= 3 (-> email/warning-24hr calls count)))
        (is
         (every?
          #(= :rfp  (-> % :args first :type))
          (calls email/warning-24hr)))))))

(deftest test-notify-subscribers-closed
  (with-stub [[email/notify-pns-closed (constantly nil)]
              [db/get-all-rfps (constantly [{:id (java.util.UUID/randomUUID) ;; Outside of time interval, no subscribers
                                             :close_date (t/plus (t/now) (t/hours 10) (t/minutes 30))
                                             :open_date (t/minus (t/now) (t/hours 230) (t/minutes 30))
                                             :type :rfp
                                             :number ""
                                             :title ""
                                             :description ""
                                             :file_link ""}
                                            {:id (java.util.UUID/randomUUID) ;;In time interval, but no subscribers
                                             :close_date (t/minus (t/now) (t/minutes 30))
                                             :open_date (t/minus (t/now) (t/hours 230) (t/minutes 30))
                                             :type :rfp
                                             :number ""
                                             :title ""
                                             :description ""
                                             :file_link ""}])]
              [db/get-all-ifbs (constantly [{:id (java.util.UUID/randomUUID) ;; In time interval, 2 subscribers
                                             :close_date (t/minus (t/now) (t/minutes 30))
                                             :open_date (t/minus (t/now) (t/hours 230) (t/minutes 30))
                                             :type :ifb
                                             :number ""
                                             :title ""
                                             :description ""
                                             :file_link ""}])]
              [db/get-subscriptions (fn [x]
                                      (if (instance? java.util.UUID (:ifb_id x))
                                        [true true]
                                        []))]]
    (with-spy [t/within?]
      (jobs/notify-subscribers-closed)
      (testing "should call email/notify-pns-closed only for every subscriber only for announcements that have expired in the last hour"
        (is (= 3 (-> t/within? calls count)))
        (is (= 2 (-> email/notify-pns-closed calls count)))
        (is
         (every?
          #(= :ifb  (-> % :args first :type))
          (calls email/notify-pns-closed)))))))
