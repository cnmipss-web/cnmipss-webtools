(ns webtools.cron.jobs-test
  (:require [clojure.test :refer :all]
            [clj-time.core :as t]
            [webtools.cron.jobs :as jobs]
            [webtools.email :as email]
            [webtools.db.core :as db]
            [webtools.test.fixtures :as fixtures]
            [webtools.test.util :refer [count-calls args-from-call]]
            [webtools.models.procurement.core :as p]
            [bond.james :refer [calls with-spy with-stub with-stub!]]))

(use-fixtures :once fixtures/prep-db)
(use-fixtures :each fixtures/with-rollback)

(deftest test-check-notify-subscribers-24hr
  
  (with-stub [[email/warning-24hr (constantly nil)]
              [db/get-all-pnsa (constantly
                                [{:id          (p/make-uuid "cf82deed-c84f-446c-a3f0-0d826428ddbd")
                                  :close_date  (t/plus (t/now) (t/hours 23) (t/minutes 30))
                                  :open_date   (t/minus (t/now) (t/hours 23) (t/minutes 30))
                                  :type        :rfp
                                  :number      ""
                                  :title       ""
                                  :description ""
                                  :file_link   ""
                                  :spec_link   ""}
                                 {:id          (p/make-uuid "d2b4e97c-5d7c-4ccd-8fae-a27a27c863e3")
                                  :close_date  (t/plus (t/now) (t/hours 230) (t/minutes 30))
                                  :open_date   (t/minus (t/now) (t/hours 23) (t/minutes 30))
                                  :type        :ifb
                                  :number      ""
                                  :title       ""
                                  :description ""
                                  :file_link   ""
                                  :spec_link   ""}])]
              [db/get-subscriptions (constantly [{}])]]
    (with-spy [t/within?]
      (jobs/check-notify-subscribers-24hr)
      (testing "should call email/warning-24hr only for every subscriber only for expiring announcements"
        (is (= 2 (count-calls t/within?)))
        (is (= 1 (count-calls email/warning-24hr)))
        (is (= (p/map->PSAnnouncement (first (db/get-all-pnsa)))
               (first (args-from-call email/warning-24hr))))
        (is
         (every?
          #(= :rfp  (-> % :args first :type))
          (calls email/warning-24hr)))))))

(deftest test-notify-subscribers-closed
  (with-stub [[email/notify-pns-closed (constantly nil)]
              [db/get-all-pnsa (constantly
                                [{:id          (java.util.UUID/randomUUID)
                                  ;; In time interval, 3 subscribers
                                  :close_date  (t/minus (t/now) (t/minutes 30))
                                  :open_date   (t/minus (t/now) (t/hours 230) (t/minutes 30))
                                  :type        :ifb
                                  :number      ""
                                  :title       ""
                                  :description ""
                                  :file_link   ""
                                  :spec_link   ""}
                                 {:id          (java.util.UUID/randomUUID)
                                  ;; Outside of time interval, no subscribers
                                  :close_date  (t/plus (t/now) (t/hours 10) (t/minutes 30))
                                  :open_date   (t/minus (t/now) (t/hours 230) (t/minutes 30))
                                  :type        :rfp
                                  :number      ""
                                  :title       ""
                                  :description ""
                                  :file_link   ""
                                  :spec_link   ""}
                                 {:id          (java.util.UUID/randomUUID)
                                  ;;In time interval, but no subscribers
                                  :close_date  (t/minus (t/now) (t/minutes 30))
                                  :open_date   (t/minus (t/now) (t/hours 230) (t/minutes 30))
                                  :type        :rfp
                                  :number      ""
                                  :title       ""
                                  :description ""
                                  :file_link   ""
                                  :spec_link   ""}])]
              [db/get-subscriptions [(fn [x] [{} {} {}])
                                     (fn [x] [])
                                     (fn [x] [])]]]
    (with-spy [t/within?]
      (jobs/notify-subscribers-closed)
      (testing "should call email/notify-pns-closed only for every subscriber only for announcements that have expired in the last hour"
        (is (= 3 (count-calls t/within?)))
        (is (= 3 (count-calls email/notify-pns-closed)))
        (is
         (every?
          #(= :ifb  (-> % :args first :type))
          (calls email/notify-pns-closed)))))))
