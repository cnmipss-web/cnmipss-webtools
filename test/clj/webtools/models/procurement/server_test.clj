(ns webtools.models.procurement.server-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as spec]
            [clj-time.core :as time]
            [clj-time.coerce :as c]
            [webtools.db :as db]
            [webtools.test.fixtures :as fixtures]
            [webtools.models.procurement.server]
            [webtools.models.procurement.core :as p]
            [webtools.util :as util]))

(use-fixtures :once fixtures/prep-db fixtures/instrument)

(use-fixtures :each fixtures/with-rollback)

(defn- -drop-time [date]
  ((comp c/from-sql-date c/to-sql-date) date))

(def valid-pns
  [(p/map->PSAnnouncement {:id (java.util.UUID/randomUUID)
                           :type :rfp
                           :number "17-0041"
                           :title "Title #1"
                           :description "Desc #1"
                           :open_date (-drop-time (time/today))
                           :close_date (time/plus (time/now) (time/months 1))
                           :file_link ""
                           :spec_link ""})

   (p/map->PSAnnouncement {:id (java.util.UUID/randomUUID)
                           :type :rfp
                           :number "15-0042"
                           :title "Title #3"
                           :description "Desc #3"
                           :open_date (time/minus (-drop-time (time/today)) (time/days 17))
                           :close_date (time/plus (time/now) (time/months 1))
                           :file_link ""
                           :spec_link ""})

   (p/map->PSAnnouncement {:id (java.util.UUID/randomUUID)
                           :type :ifb
                           :number "16-0041"
                           :title "Title #41"
                           :description "Desc #7"
                           :open_date (time/minus (-drop-time (time/today)) (time/weeks 13))
                           :close_date (time/plus (time/now) (time/months 12))
                           :file_link ""
                           :spec_link ""})])

(deftest test-procurement-to-db
  (testing "protocol procurement-to-db"
    (testing "webtools.models.procurement.core.PSAnnouncement"
      (testing "method save-to-db"
        (testing "should save PSAnnouncement to DB"
          (doseq [pns valid-pns]
            (p/save-to-db! pns)
            (let [record (p/get-pns-from-db (:id pns))]
              (doseq [[k v] pns]
                (is (= v (get record k))))))))

      (testing "method change-in-db"
        (testing "should save changes in PSAnnouncement to DB"
          (doseq [pns (for [orig valid-pns]
                        (assoc orig :close_date (time/now)))]
            (p/change-in-db! pns)
            (let [record (p/get-pns-from-db (:id pns))]
              (doseq [[k v] pns]
                (is (= v (get record k))))))))

      (testing "method delete-from-db"
        (testing "should remove a PSAnnouncement from the DB"
          (doseq [pns valid-pns]
            (p/delete-from-db! pns)
            (let [record (p/get-pns-from-db (:id pns))]
              (is (nil? record)))))))))

(deftest test-communicate-procurement
  (testing "protocol communicate-procurement"
    (testing "webtools.models.procurement.core.PSAnnouncement"
      (testing "method uppercase-type"
        (testing "should return an uppercase string based on :type keyword"
          (let [[a b c] (mapv p/uppercase-type valid-pns)]
            (is (= "RFP" a))
            (is (= "RFP" b))
            (is (= "IFB" c)))))

      (testing "method title-string"
        (testing "should return a correctly formatted title string"
          (let [[a b c] (mapv p/title-string valid-pns)]
            (is (= "RFP# 17-0041 Title #1" a))
            (is (= "RFP# 15-0042 Title #3" b))
            (is (= "IFB# 16-0041 Title #41" c))))))))

(deftest test-procurement-from-db
  (testing "protocol procurement-from-db"
    (testing "java.util.String"
      (testing "method make-uuid"
        (testing "should convert uuid string into uuid object"
          (let [uuid (p/make-uuid "7e0fbd1e-6c6a-4e3f-85eb-84f060bce705")]
            (is (instance? java.util.UUID uuid))))

        (testing "should raise error if string is not a uuid str"
          (try
            (p/make-uuid "123")
            (is false)
            (catch clojure.lang.ExceptionInfo ex
              (is (some? ex))))))

      (testing "method get-pns-from-db"
        (testing "should retrieve PSAnnouncement from DB matching id"
          (let [id  "1174a9a8-b45a-422a-bb46-574f814c2550"
                pns (p/get-pns-from-db id)]
            (is (= true (instance? webtools.models.procurement.core.PSAnnouncement pns)))
            (is (= (p/make-uuid id) (:id pns))))))

      (testing "method get-subs-from-db"
        (testing "should return list of subscriptions matching proc_id"
          (let [proc_id "d2b4e97c-5d7c-4ccd-8fae-a27a27c863e3"
                subs    (p/get-subs-from-db proc_id)]
            (is (= 3 (count subs)))
            (is (every? #(instance? webtools.models.procurement.core.Subscription %) subs))))))

    (testing "java.util.UUID"
      (testing "method make-uuid"
        (testing "should return the input unmodified"
          (let [uuid (java.util.UUID/randomUUID)]
            (is (= uuid (p/make-uuid uuid))))))

      (testing "method get-pns-from-db"
        (testing "should retrieve PSAnnouncement from DB matching id"
          (let [id  (p/make-uuid "1174a9a8-b45a-422a-bb46-574f814c2550")
                pns (p/get-pns-from-db id)]
            (is (= true (instance? webtools.models.procurement.core.PSAnnouncement pns)))
            (is (= id (:id pns))))))

      (testing "method get-subs-from-db"
        (testing "should return list of subscriptions matching proc_id"
          (let [proc_id (p/make-uuid "d2b4e97c-5d7c-4ccd-8fae-a27a27c863e3")
                subs    (p/get-subs-from-db proc_id)]
            (is (= 3 (count subs)))
            (is (every? #(instance? webtools.models.procurement.core.Subscription %) subs))))))

    (testing "nil"
      (testing "method make-uuid"
        (testing "should return nil"
          (is (= nil (p/make-uuid nil)))))

      (testing "method get-pns-from-db"
        (testing "should return nil"
          (let [id  nil
                pns (p/get-pns-from-db id)]
            (is (nil? pns)))))

      (testing "method get-subs-from-db"
        (testing "should return nil"
          (let [proc_id nil
                subs    (p/get-subs-from-db proc_id)]
            (is (nil? subs))))))))
