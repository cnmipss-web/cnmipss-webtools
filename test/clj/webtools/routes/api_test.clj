(ns webtools.routes.api-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [clojure.walk :as walk]
            [clojure.java.io :refer [file]]
            [clj-fuzzy.metrics :as measure]
            [ring.mock.request :as mock]
            [webtools.handler :refer :all]
            [webtools.util :refer :all]
            [webtools.json :refer :all]
            [webtools.config :refer [env]]
            [webtools.db.core :as db]
            [webtools.test.constants :as c-t]
            [webtools.test.fixtures :as fixtures]
            [webtools.test.tools :refer [auth-req equal-props? not-equal-props?]]
            [webtools.wordpress-api :as wp]
            [conman.core :refer [bind-connection] :as conman]
            [mount.core :as mount]))

(use-fixtures :once fixtures/prep-db)

(use-fixtures :each fixtures/with-rollback)

(deftest test-api-routes
  (testing "GET /api/all-certs"
    (let [{:keys [status headers body] :as response} ((app) (mock/request :get "/api/all-certs"))
          edn-body (json->edn body)]
      (testing "should return a list of all certifications stored in DB"
        (is (= 200 status))
        (is (= "application/json" (get headers "Content-Type")))
        (is (= 4 (count edn-body)))
        (is (every? #(and (string? (:cert_no %))
                          (string? (:first_name %))
                          (string? (:last_name %))
                          (string? (:start_date %))
                          (string? (:expiry_date %))) edn-body)))))

  (testing "GET /api/all-jvas"
    (let [{:keys [status headers body] :as response} ((app) (mock/request :get "/api/all-jvas"))
          edn-body (json->edn body)]
      (testing "should return a list of all jvas stored in DB"
        (is (= 200 status))
        (is (= "application/json" (get headers "Content-Type")))
        (is (= 3 (count edn-body)))
        (is (every? #(and (string? (:file_link %))
                          (string? (:announce_no %))
                          (string? (:position %))
                          (string? (:open_date %))
                          (string? (:location %))) edn-body)))))

  (testing "GET /api/all-procurement"
    (let [{:keys [status headers body] :as response} ((app) (mock/request :get "/api/all-procurement"))
          {:keys [rfps ifbs subscriptions addenda] :as edn-body} (json->edn body)]
      (testing "should return a map of all lists of rfps, ifbs, addenda, and subscriptions from DB"
        (is (= 200 status))
        (is (= "application/json" (get headers "Content-Type")))
        (is (= clojure.lang.PersistentArrayMap (type edn-body)))
        (is (= 3 (count rfps)))
        (is (every? #(and (-> % :rfp_no string?)
                          (-> % :open_date string?)
                          (-> % :description string?)
                          (-> % :title string?)) rfps))
        (is (= 3 (count ifbs)))
        (is (every? #(and (-> % :ifb_no string?)
                          (-> % :open_date string?)
                          (-> % :description string?)
                          (-> % :title string?)) ifbs))
        (is (= 3 (count addenda)))
        (is (every? #(and (-> % :file_link string?)
                          (-> % :addendum_number int?)
                          (or (-> % :rfp_id some?)
                              (-> % :ifb_id some?))) addenda))
        (is (= 2 (count subscriptions)))
        (is (every? #(and (-> % :company_name string?)
                          (-> % :contact_person string?)
                          (-> % :email string?)
                          (-> % :telephone int?)
                          (or (-> % :rfp_id some?)
                              (-> % :ifb_id some?))) subscriptions)))))

  (testing "POST /api/subscribe-procurement"
    (let [subscriptions
          (db/get-subscriptions {:rfp_id nil
                                 :ifb_id (java.util.UUID/fromString "cf82deed-c84f-446c-a3f0-0d826428ddbd")})]
        (is (= 0 (count subscriptions))))
    (let [{:keys [status headers body] :as response}
          ((app) (-> (mock/request :post "/api/subscribe-procurement")
                     (assoc :body (edn->json {:company "Test Centers of America"
                                              :person "TV's Adam West"
                                              :email "iambatman@gotham.tv"
                                              :tel "+1 (670) 555-6666"
                                              :ifb_id "cf82deed-c84f-446c-a3f0-0d826428ddbd"}))))]
      (is (= 200 status))
      (is (= "application/json" (get headers "Content-Type")))
      (let [subscriptions
            (db/get-subscriptions {:rfp_id nil
                                   :ifb_id (java.util.UUID/fromString "cf82deed-c84f-446c-a3f0-0d826428ddbd")})]
        (is (= 1 (count subscriptions)))
        (is (= "Test Centers of America" (-> subscriptions first :company_name)))
        (is (= "TV's Adam West" (-> subscriptions first :contact_person)))
        (is (= 16705556666 (-> subscriptions first :telephone))))))

  (testing "POST /api/verify-token"
    (testing "should respond 403 to any request without wt-token cookie"
      (let [{:keys [status]}
            ((app) (-> (mock/request :post "/api/verify-token")
                       (assoc :cookies {"wt-email" {:value "tyler.collins@cnmipss.org"}})))]
        (is (= 403 status))))

    (testing "should respond 403 to any request with a wt-token and wt-email combination that does not match token in DB"
      (let [{:keys [status]}
            ((app) (-> (mock/request :post "/api/verify-token")
                       (assoc :cookies {"wt-token" {:value "ya29.GluTBHe"}
                                        "wt-email" {:value "tyler.collins@cnmipss.org"}})))]
        (is (= 403 status))))

    (testing "should respond 200 to any request with a wt-token and wt-email combination that matches DB"
      (let [{:keys [status]}
            ((app) (-> (mock/request :post "/api/verify-token")
                       (assoc :cookies {"wt-token" {:value "ya29.GluTBHe_gy2R2PBdSedi3oZKT64AltZN7EfIQKReuLOWcdMjySQnh5VeSCLC8-_aG1wdhaBrT4baVSvWnrDoiK5z3_nJkdKpfAhiXI1c2cenTSJyd8sx-dpqBm0B"}
                                        "wt-email" {:value "tyler.collins@cnmipss.org"}})))]
        (is (= 200 status)))))

  (testing "GET /logout"
    (testing "should respond 302 and clear wt-token and wt-email cookies from client"
      (let [{:keys [status headers]}
            ((app) (-> (mock/request :get "/logout")
                       (assoc :cookies {"wt-token" {:value "ya29.GluTBHe_gy2R2PBdSedi3oZKT64AltZN7EfIQKReuLOWcdMjySQnh5VeSCLC8-_aG1wdhaBrT4baVSvWnrDoiK5z3_nJkdKpfAhiXI1c2cenTSJyd8sx-dpqBm0B"}
                                        "wt-email" {:value "tyler.collins@cnmipss.org"}})))]
        (is (= 302 status))
        (is (= "wt-token=;Max-Age=1;Path=/webtools" (-> headers (get "Set-Cookie") first)))
        (is (= "wt-email=;Max-Age=1;Path=/webtools" (-> headers (get "Set-Cookie") second))))))

  (deftest test-api-routes-with-auth
    (testing "GET /api/refresh-session"
      (let [{:keys [status headers]}
            (auth-req :get "/api/refresh-session")]
        (testing "should set client cookies with new max-age"
          (is (= "wt-token=ya29.GluTBHe_gy2R2PBdSedi3oZKT64AltZN7EfIQKReuLOWcdMjySQnh5VeSCLC8-_aG1wdhaBrT4baVSvWnrDoiK5z3_nJkdKpfAhiXI1c2cenTSJyd8sx-dpqBm0B;HttpOnly;Max-Age=900;Path=/webtools" (-> headers (get "Set-Cookie") first)))
          (is (= "wt-email=tyler.collins%40cnmipss.org;HttpOnly;Max-Age=900;Path=/webtools" (-> headers (get "Set-Cookie") second))))))

    (testing "GET /api/user"
      (let [{:keys [status body headers]}
            (auth-req :get "/api/user")]
        (testing "should return a user's account info"
          (let [edn-body (json->edn body)]
            (is (= clojure.lang.PersistentArrayMap (type edn-body)))
            (is (= "tyler.collins@cnmipss.org" (get-in edn-body [:user :email])))
            (is (= true (get-in edn-body [:user :admin])))
            (is (= nil (get-in edn-body [:user :roles])))))))

    (testing "GET /api/all-users"
      (let [{:keys [status body heades]}
            (auth-req :get "/api/all-users")]
        (testing "should return a list of all users"
          (is (= 5 (-> body json->edn count)))
          (is (every? #(and (-> % :email string?)
                            (->> % :admin (instance? Boolean))
                            (or (-> % :roles string?)
                                (-> % :roles nil?))) (json->edn body))))))

    (testing "POST /api/create-user")

    (testing "POST /api/delete-user")

    (testing "POST /api/delete-user")

    (testing "POST /api/update-jva")

    (testing "POST /api/delete-jva")

    (testing "POST /api/update-rfp")

    (testing "POST /api/delete-rfp")

    (testing "POST /api/update-ifb")

    (testing "POST /api/delete-ifb")))
