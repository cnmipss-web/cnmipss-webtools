(ns webtools.routes.api-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.data.json :as json]
            [clojure.walk :as walk]
            [clojure.java.io :refer [file]]
            [clj-fuzzy.metrics :as measure]
            [ring.mock.request :as mock]
            [bond.james :refer [calls with-spy with-stub!]]
            [webtools.handler :refer [app]]
            [webtools.util :refer :all]
            [webtools.json :refer :all]
            [webtools.config :refer [env]]
            [webtools.db.core :as db]
            [webtools.email :as email]
            [webtools.procurement.core :refer :all]
            [webtools.test.constants :as c-t]
            [webtools.test.fixtures :as fixtures]
            [webtools.test.tools :refer [auth-req equal-props? not-equal-props?]]
            [webtools.wordpress-api :as wp]
            [conman.core :refer [bind-connection] :as conman]
            [mount.core :as mount]))

(use-fixtures :once fixtures/prep-db fixtures/instrument)

(use-fixtures :each fixtures/with-rollback)

(deftest test-api-certification
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
                          (string? (:expiry_date %))) edn-body))))))

(deftest test-api-hro
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
                          (string? (:location %))) edn-body))))))

(deftest test-api-p&s
  (testing "GET /api/all-procurement"
    (let [{:keys [status headers body] :as response} ((app) (mock/request :get "/api/all-procurement"))
          {:keys [pnsa subscriptions addenda] :as edn-body} (json->edn body)]
      (testing "should return a map of all lists of rfps, ifbs, addenda, and subscriptions from DB"
        (is (= 200 status))
        (is (= "application/json" (get headers "Content-Type")))
        (is (= clojure.lang.PersistentArrayMap (type edn-body)))

        (is (= 6 (count pnsa)))
        (is (every? #(s/valid? :webtools.spec.procurement/record %) (map convert-pns-from-map pnsa)))

        (is (= 3 (count addenda)))
        (is (every? #(and (-> % :file_link string?)
                          (-> % :addendum_number int?)
                          (-> % :proc_id some?)) addenda))

        (is (= 4 (count subscriptions)))
        (is (every? #(s/valid? :webtools.spec.subscription/record %) (map convert-sub-from-map subscriptions))))))

  (testing "POST /api/subscribe-procurement"
    (with-stub! [[email/confirm-subscription (constantly nil)]]
      (testing "should handle subscriptions to rfps"
        (let [subscriber {:company "Test Centers of America"
                          :person "TV's Adam West"
                          :email "iambatman@gotham.tv"
                          :tel "+1 (670) 555-6666"
                          :proc_id "d2b4e97c-5d7c-4ccd-8fae-a27a27c863e3"}
              {:keys [status headers body] :as response}
              ((app) (-> (mock/request :post "/api/subscribe-procurement")
                         (assoc :body (edn->json subscriber))))]
          (testing "should return status 200"
            (is (= 200 status)))
          
          (testing "should return JSON"
            (is (= "application/json" (get headers "Content-Type"))))
          
          (testing "should add subscription to the DB"
            (let [subscriptions
                  (db/get-subscriptions {:proc_id (make-uuid "d2b4e97c-5d7c-4ccd-8fae-a27a27c863e3")})]
              (is (= 4 (count subscriptions)))
              (is (= "Test Centers of America" (-> subscriptions last :company_name)))
              (is (= "TV's Adam West" (-> subscriptions last :contact_person)))
              (is (= 16705556666 (-> subscriptions last :telephone)))))

          (testing "should send confirmation email to subscriber"
            (is (= 1 (-> email/confirm-subscription calls count)))
            (is (= 2 (-> email/confirm-subscription calls first :args count)))
            (let [contact (-> email/confirm-subscription calls first :args first)]
              (is (= (:company subscriber) (:company_name contact)))
              (is (= (:person subscriber) (:contact_person contact)))
              (is (= (:email subscriber) (:email contact)))))))
      
      (testing "should handle subscriptions to ifbs"
        (let [subscriber {:company "Test Centers of America"
                          :person "TV's Adam West"
                          :email "iambatman@gotham.tv"
                          :tel "+1 (670) 555-6666"
                          :rfp_id "d2b4e97c-5d7c-4ccd-8fae-a27a27c863e3"}
              {:keys [status headers body] :as response}
              ((app) (-> (mock/request :post "/api/subscribe-procurement")
                         (assoc :body (edn->json {:company "Test Centers of America"
                                                  :person "TV's Adam West"
                                                  :email "iambatman@gotham.tv"
                                                  :tel "+1 (670) 555-6666"
                                                  :proc_id "cf82deed-c84f-446c-a3f0-0d826428ddbd"}))))]
          (testing "should return status 200"
            (is (= 200 status)))
          
          (testing "should return JSON"
            (is (= "application/json" (get headers "Content-Type"))))
          
          (testing "should add subscription to the DB"
            (let [subscriptions
                  (db/get-subscriptions {:proc_id (make-uuid "cf82deed-c84f-446c-a3f0-0d826428ddbd")})]
              (is (= 2 (count subscriptions)))
              (is (= "Test Centers of America" (-> subscriptions last :company_name)))
              (is (= "TV's Adam West" (-> subscriptions last :contact_person)))
              (is (= 16705556666 (-> subscriptions last :telephone)))))

          (testing "should send confirmation email to subscriber"
            (is (= 2 (-> email/confirm-subscription calls count)))
            (is (= 2 (-> email/confirm-subscription calls first :args count)))
            (let [contact (-> email/confirm-subscription calls first :args first)]
              (is (= (:company subscriber) (:company_name contact)))
              (is (= (:person subscriber) (:contact_person contact)))
              (is (= (:email subscriber) (:email contact))))))))))

(deftest test-api-authentication
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
        (is (= "wt-email=;Max-Age=1;Path=/webtools" (-> headers (get "Set-Cookie") second)))))))

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

  (testing "POST /api/create-user"
    (with-stub! [[email/invite (constantly nil)]]
      (let [{:keys [status body header]}
            (auth-req :post "/api/create-user"
                      (assoc :body {:email "test@test.com"
                                    :admin "true"
                                    :roles "Testing"}))]

        (testing "should return status 200"
          (is (= 200 status)))

        (testing "should add a record of user to DB"
          (let [user (db/get-user-info {:email "test@test.com"})]
            (is (= true (:admin user)))
            (is (= "Testing" (:roles user)))))

        (testing "should email user a notification that they have been invited"
          (let [user (db/get-user-info {:email "test@test.com"})]
            (is (= 1 (-> email/invite calls count)))
            (is (= (dissoc user :token) (-> email/invite calls first :args first))))))))

  (testing "POST /api/update-user"
    (let [{:keys [status body headers]}
          (auth-req :post "/api/update-user"
                    (assoc :body {:email "john.doe@cnmipss.org"
                                  :admin true
                                  :roles nil}))]

      (testing "should return status 200"
        (is (= 200 status)))

      (testing "should update user in DB"
        (let [user (db/get-user-info {:email "john.doe@cnmipss.org"})]
          (is (:admin user))
          (is (nil? (:roles user)))))))

  (testing "POST /api/delete-user"
    (let [{:keys [status body headers]}
          (auth-req :post "/api/delete-user"
                    (assoc :body {:email "test@test.com"}))]
      (testing "should return status 200"
        (is (= 200 status)))

      (testing "should remove user from db"
        (let [user (db/get-user-info {:email "test@test.com"})]
          (is (nil? user))))))

  (testing "POST /api/update-jva"
    (let [{:keys [status body headers]}
          (auth-req :post "/api/update-jva"
                    (assoc :body {:id "8d893df0-1afc-4dd6-8e20-eb74a6e4e50b"
                                  :announce_no "PSS-2015-311"
                                  :position "New Job Title"
                                  :status true
                                  :open_date "December 22, 2016"
                                  :close_date nil
                                  :salary "Moolah"
                                  :location "Remote"
                                  :file_link "dummyli.nk"}))]

      (testing "should return status 200"
        (is (= 200 status)))

      (testing "should modify the jva record in DB"
        (let [jva (db/get-jva {:announce_no "PSS-2015-311"})]
          (is (= "New Job Title" (:position jva)))
          (is (= nil (:close_date jva)))
          (is (= "Moolah" (:salary jva)))
          (is (= "Remote" (:location jva)))))))

  (testing "POST /api/delete-jva"
    (with-stub! [[wp/delete-media (constantly nil)]]
      (let [{:keys [status body headers]}
            (auth-req :post "/api/delete-jva"
                      (assoc :body {:announce_no "PSS-2015-311"}))]

        (testing "should return status 200"
          (is (= 200 status)))

        (testing "should remove record of jva from DB"
          (let [jva (db/get-jva {:announce_no "PSS-2015-311"})]
            (is (nil? jva))))

        (testing "should delete related media"
          (is (= 1 (-> wp/delete-media calls count)))
          (is (= "8d893df0-1afc-4dd6-8e20-eb74a6e4e50b" (-> wp/delete-media
                                                            ((comp first calls))
                                                            ((comp first :args)))))))))

  (testing "POST /api/update-procurement"
    (with-stub! [[email/notify-subscribers (constantly nil)]]
      (let [new-title "New Title for Proposal #2"
            new-desc "The description of this proposal has changed."
            rfp (-> (get-pns-from-db "d0002906-6432-42b5-b82b-35f0d710f827")
                    (#(into {} %))
                    (assoc :title new-title)
                    (assoc :description new-desc))
            orig (get-pns-from-db (:id rfp))
            {:keys [status body headers]}
            (auth-req :post "/api/update-procurement"
                      (assoc :body rfp))]
        (testing "should return status 200"
          (is (= 200 status)))

        (testing "should alter record of rfp in the database"
          (let [new (get-pns-from-db "d0002906-6432-42b5-b82b-35f0d710f827")]
            (is (= new-title (:title new)))
            (is (= new-desc (:description new)))))

        (testing "should notify subscribers of the updated rfp"
          (is (= 1 (-> email/notify-subscribers calls count)))
          (is (= :update (-> email/notify-subscribers calls first :args first)))
          (is (= orig (-> email/notify-subscribers calls first :args second)))
          (is (= (convert-pns-from-map rfp) (-> email/notify-subscribers calls first :args last))))))

    (with-stub! [[email/notify-subscribers (constantly nil)]]
      (let [new-title "New Title for Invitation #2"
            new-desc "The description of this invitation has changed."
            ifb (-> (get-pns-from-db "2fa4e278-f022-4361-b69a-0063a387933a")
                    (#(into {} %))
                    (assoc :title new-title)
                    (assoc :description new-desc))
            orig (get-pns-from-db (:id ifb))
            {:keys [status body headers]}
            (auth-req :post "/api/update-procurement"
                      (assoc :body ifb))]
        (testing "should return status 200"
          (is (= 200 status)))

        (testing "should alter record of ifb in the database"
          (let [new (get-pns-from-db "2fa4e278-f022-4361-b69a-0063a387933a")]
            (is (= new-title (:title new)))
            (is (= new-desc (:description new)))))

        (testing "should notify subscribers of the updated rfp"
          (is (= 1 (-> email/notify-subscribers calls count)))
          (is (= :update (-> email/notify-subscribers calls first :args first)))
          (is (= orig (-> email/notify-subscribers calls first :args second)))
          (is (= (convert-pns-from-map ifb) (-> email/notify-subscribers calls first :args last)))))))

  (testing "POST /api/delete-rfp"
    (with-stub! [[email/notify-subscribers (constantly nil)]
                 [wp/delete-media (constantly nil)]]
      (let [rfp (into {} (get-pns-from-db "d0002906-6432-42b5-b82b-35f0d710f827"))
            {:keys [status body headers]}
            (auth-req :post "/api/delete-rfp"
                      (assoc :body rfp))]
        (testing "should return status 200"
          (is (= 200 status)))

        (testing "should remove record of rfp from the database"
          (let [rfp (get-pns-from-db "d0002906-6432-42b5-b82b-35f0d710f827")]
            (is (every? nil? (vals rfp)))
            (is (= 2 (count (filter #(= "rfp" (:type %)) (db/get-all-pnsa)))))))

        (testing "should notify subscribers that rfp has been deleted"
          (is (= 1 (-> email/notify-subscribers calls count)))
          (is (= :delete (-> email/notify-subscribers calls first :args first)))
          (is (= :rfps (-> email/notify-subscribers calls first :args second)))
          (is (= (convert-pns-from-map rfp) (-> email/notify-subscribers calls first :args last))))

        (testing "should delete related addenda"
          (let [addenda (db/get-addenda {:proc_id (:id rfp)})]
            (is (empty? addenda))))

        (testing "should delete related subscribers"
          (let [subscribers (db/get-subscriptions {:proc_id (:id rfp)})]
            (is (empty? subscribers))))

        (testing "should delete related media: file_link, spec_link"
          (is (= 2 (-> wp/delete-media calls count)))
          (is (= (:id rfp) (-> wp/delete-media calls first :args first)))))))

  (testing "POST /api/delete-ifb"
    (with-stub! [[email/notify-subscribers (constantly nil)]
                 [wp/delete-media (constantly nil)]]
      (let [ifb (into {} (get-pns-from-db "cf82deed-c84f-446c-a3f0-0d826428ddbd"))
            {:keys [status body headers]}
            (auth-req :post "/api/delete-ifb"
                      (assoc :body ifb))]
        (testing "should return status 200"
          (is (= 200 status)))

        (testing "should remove record of ifb from the database"
          (let [ifb (get-pns-from-db "cf82deed-c84f-446c-a3f0-0d826428ddbd")]
            (is (every? nil? (vals ifb)))
            (is (= 2 (count (filter #(= "ifb" (:type %)) (db/get-all-pnsa)))))))

        (testing "should notify subscribers that ifb has been deleted"
          (is (= 1 (-> email/notify-subscribers calls count)))
          (is (= :delete (-> email/notify-subscribers calls first :args first)))
          (is (= :ifbs (-> email/notify-subscribers calls first :args second)))
          (is (= (convert-pns-from-map ifb) (-> email/notify-subscribers calls first :args last))))

        (testing "should delete related addenda"
          (let [addenda (db/get-addenda {:proc_id (:id ifb)})]
            (is (empty? addenda))))

        (testing "should delete related subscribers"
          (let [subscribers (db/get-subscriptions {:proc_id (:id ifb)})]
            (is (empty? subscribers))))

        (testing "should delete related media: file_link, spec_link"
          (is (= 2 (-> wp/delete-media calls count)))
          (is (= (:id ifb) (-> wp/delete-media calls first :args first))))))))
