(ns webtools.routes.api-test
  (:require [bond.james :refer [calls with-stub!]]
            [clojure.spec.alpha :as spec]
            [clojure.string :as cstr]
            [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [webtools.db.core :as db]
            [webtools.email :as email]
            [webtools.handler :refer [app]]
            [webtools.models.procurement.core :refer :all]
            [webtools.test.fixtures :as fixtures]
            [webtools.test.tools :refer [auth-req unauth-req]]
            [webtools.test.util :refer [args-from-call count-calls]]
            [webtools.util :as util]
            [webtools.json :as json]
            [webtools.wordpress-api :as wp]))

(use-fixtures :once fixtures/prep-db fixtures/instrument)

(use-fixtures :each fixtures/with-rollback)

(def ^:dynamic body    nil)
(def ^:dynamic status  nil)
(def ^:dynamic headers nil)

(defmacro testing-route 
  "Macro to abstract boilerplate for testing api routes.  Accepts a map of options 
  and a body of assertions to be evaluated.  Assertions may use the status, body, 
  and headers symbols which will be dynamically bound to the results of the
  relevant api requests.

    :method  -- :get | :post | :delete | :put
    :route   -- API route to run the tests against.  Should be a valid url or path
    :auth    -- true | false
    :body    -- EDN data to be transmitted as JSON in request body"
  {:style/indent 1}
  [opts & assertions]
  `(testing (str (-> ~opts :method name cstr/upper-case) " " (:route ~opts))
     (let [auth?#         (:auth ~opts)
           auth-req-fn#   (fn [] (auth-req (:method ~opts) (:route ~opts)
                                   (assoc :body (:body ~opts))))
           unauth-req-fn# (fn [] (unauth-req (:method ~opts) (:route ~opts)
                                   (assoc :body (:body ~opts))))
           response#      (if auth?#
                            (auth-req-fn#)
                            (unauth-req-fn#))]
       (binding [status  (:status response#)
                 headers (:headers response#)
                 body    (json/json->edn (:body response#))]
         ~@assertions
         (if auth?#
           (testing "should reject unauthorized requests"
             (let [response# ((app) (mock/request (:method ~opts) (:route ~opts)))]
               (binding [status (:status response#)]
                 (is (= 403 status))))))))))

(deftest test-api-certification
  (testing-route {:route "/api/all-certs"
                  :method :get}
    (testing "should return a list of all certifications stored in DB"
      (is (= 200 status))
      (is (= "application/json" (get headers "Content-Type")))
      (is (= 4 (count body)))
      (is (every? (partial spec/valid? :webtools.spec.certification/record) body)))))

(deftest test-api-hro
  (testing-route {:route "/api/all-jvas"
                  :method :get}
    (testing "should return a list of all jvas stored in DB"
      (is (= 200 status))
      (is (= "application/json" (get headers "Content-Type")))
      (is (= 3 (count body)))
      (is (every? (partial spec/valid? :webtools.spec.jva/record) body)))))

(deftest test-api-p&s

  (testing-route {:route "/api/all-procurement"
                  :method :get}
    (let [{:keys [pnsa subscriptions addenda]} body]
      (testing "should return a map of all lists of rfps, ifbs, addenda, and subscriptions from DB"
        (is (= 200 status))
        (is (= "application/json" (get headers "Content-Type")))
        (is (= clojure.lang.PersistentArrayMap (type body)))

        (is (= 6 (count pnsa)))
        (is (every? (partial spec/valid? :webtools.spec.procurement/record)
                    (map convert-pns-from-map pnsa)))

        (is (= 3 (count addenda)))
        (is (every? (partial spec/valid? :webtools.spec.procurement-addendum/record) addenda))

        (is (= 4 (count subscriptions)))
        (is (every? (partial spec/valid? :webtools.spec.subscription/record)
                    (map convert-sub-from-map subscriptions)))))))

(deftest test-api-rfp-subscriptions
  (with-stub! [[email/confirm-subscription (constantly nil)]
               [email/notify-procurement (constantly nil)]]
    (let [rfp-id     (make-uuid "d2b4e97c-5d7c-4ccd-8fae-a27a27c863e3")
          subscriber {:company "Test Centers of America"
                      :person  "TV's Adam West"
                      :email   "iambatman@gotham.tv"
                      :tel     "+1 (670) 555-6666"}]
      (testing-route {:route  "/api/subscribe-procurement"
                      :method :post
                      :body   (json/edn->json (assoc subscriber :proc_id rfp-id))}
        (testing "should handle subscriptions to rfps"
          (testing "should return status 200"
            (is (= 200 status)))
          
          (testing "should return JSON"
            (is (= "application/json" (get headers "Content-Type"))))
          
          (testing "should add subscription to the DB"
            (let [subscriptions (get-subs-from-db rfp-id)]
              (is (= 4 (count subscriptions)))
              (is (= "Test Centers of America" (-> subscriptions last :company_name)))
              (is (= "TV's Adam West" (-> subscriptions last :contact_person)))
              (is (= "+1 (670) 555-6666" (-> subscriptions last :telephone)))))

          (testing "should send confirmation email to subscriber"
            (is (= 1 (count-calls email/confirm-subscription)))
            (is (= 2 (count (args-from-call email/confirm-subscription))))
            (let [contact (first (args-from-call email/confirm-subscription))]
              (is (= (:company subscriber) (:company_name contact)))
              (is (= (:person subscriber) (:contact_person contact)))
              (is (= (:email subscriber) (:email contact)))))))

      (testing-route {:route  "/api/subscribe-procurement"
                      :method :post
                      :body   (json/edn->json (assoc subscriber :proc_id rfp-id))}
        (testing "should only allow a single subscription per subscriber"
          (testing "should return status 500"
            (is (= 500 status))
            (is (= "Duplicate subscription.  You have already subscribed to this announcement with that email address." (:message body)))
            (is (= (:message body) (get-in body [:ex-data :msg])))
            (is (= "class java.sql.BatchUpdateException" (get-in body [:ex-data :type])))))))))

(deftest test-api-ifb-subscriptions
  (with-stub! [[email/confirm-subscription (constantly nil)]
               [email/notify-procurement (constantly nil)]]
    (let [ifb-id (make-uuid "cf82deed-c84f-446c-a3f0-0d826428ddbd")
          subscriber{:company "Test Centers of America"
                     :person "TV's Adam West"
                     :email "iambatman@gotham.tv"
                     :tel "+1 (670) 555-6666"}]
      (testing-route {:route "/api/subscribe-procurement"
                      :method :post
                      :body (json/edn->json (assoc subscriber :proc_id ifb-id))}
        (testing "should handle subscriptions to ifbs"
          (testing "should return status 200"
            (is (= 200 status)))

          (testing "should return JSON"
            (is (= "application/json" (get headers "Content-Type"))))

          (testing "should add subscription to the DB"
            (let [subscriptions (get-subs-from-db ifb-id)]
              (is (= 2 (count subscriptions)))
              (is (= "Test Centers of America" (-> subscriptions last :company_name)))
              (is (= "TV's Adam West" (-> subscriptions last :contact_person)))
              (is (= "+1 (670) 555-6666" (-> subscriptions last :telephone)))))

          (testing "should send confirmation email to subscriber"
            (is (= 1 (count-calls email/confirm-subscription)))
            (is (= 2 (count (args-from-call email/confirm-subscription))))
            (let [contact (first (args-from-call email/confirm-subscription))]
              (is (= (:company subscriber) (:company_name contact)))
              (is (= (:person subscriber) (:contact_person contact)))
              (is (= (:email subscriber) (:email contact)))))))
      (testing-route {:route "/api/subscribe-procurement"
                      :method :post
                      :body (json/edn->json (assoc subscriber :proc_id ifb-id))}
        (testing "should only allow a single subscription per subscriber"
          (testing "should return status 500"
            (is (= 500 status))
            (is (= "Duplicate subscription.  You have already subscribed to this announcement with that email address." (:message body)))
            (is (= (:message body) (get-in body [:ex-data :msg])))
            (is (= "class java.sql.BatchUpdateException" (get-in body [:ex-data :type])))))))))

(deftest test-api-unsubscribe-procurement
  (testing-route {:route "/api/unsubscribe-procurement/2af76ca0-3711-7551-a9a3-903d93a42f65"
                  :method :get}
    (testing "should redirect user the the /unsubscribed page after successfully unsubscribing"
      (is (= 302 status))

      (let [sub (db/get-users-subscription {:email "tyler.collins@cnmipss.org"
                                            :proc_id (make-uuid "cf82deed-c84f-446c-a3f0-0d826428ddbd")})]
        (is (not (:active sub)))))))

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
  (testing-route {:route  "/api/refresh-session"
                  :method :get
                  :auth   true}
    (testing "should set client cookies with new max-age"
      (let [cookies (map util/cookie->map (get headers "Set-Cookie"))]
        (is (every? #(= "900" (get % "Max-Age")) cookies)))))
  
  (testing-route {:route  "/api/user"
                  :method :get
                  :auth   true}
    (testing "should return a user's account info"
      (is (= "tyler.collins@cnmipss.org" (get-in body [:user :email])))
      (is (= true (get-in body [:user :admin])))
      (is (= nil (get-in body [:user :roles])))))

  (testing-route {:route  "/api/all-users"
                  :method :get
                  :auth   true}
    (testing "should return a list of all 5 users"
      (is (= 5 (count body)))
      (is (every? (fn valid-user? [user] (spec/valid? :webtools.spec.user/record user)) body))))
  
  (testing-route {:route  "/api/fns-nap"
                  :method :get
                  :auth   true}
    (testing "should return a list of all 5 fns-nap match documents"
      (is (= 200 status))
      (is (= 5 (count body)))
      (is (every? #(spec/valid? :webtools.spec.fns-nap/record %) body))))
  
  (with-stub! [[email/invite (constantly nil)]]
    (testing-route {:route  "/api/create-user"
                    :method :post
                    :auth   true
                    :body   (json/edn->json {:email "test@test.com"
                                             :admin "true"
                                             :roles "Testing"})}
      (testing "should return status 200"
        (is (= 200 status)))
      
      (testing "should add a record of user to DB"
        (let [user (db/get-user-info {:email "test@test.com"})]
          (is (= true (:admin user)))
          (is (= "Testing" (:roles user)))))
      
      (testing "should email user a notification that they have been invited"
        (let [user (db/get-user-info {:email "test@test.com"})]
          (is (= 1 (-> email/invite calls count)))
          (is (= (dissoc user :token) (-> email/invite calls first :args first)))))))

  (testing-route {:route  "/api/update-user"
                  :method :post
                  :auth   true
                  :body   {:email "john.doe@cnmipss.org"
                           :admin true
                           :roles nil}}

    (testing "should return status 200"
      (is (= 200 status)))

    (testing "should update user in DB"
      (let [user (db/get-user-info {:email "john.doe@cnmipss.org"})]
        (is (:admin user))
        (is (nil? (:roles user))))))

  (testing-route {:route  "/api/delete-user"
                  :method :post
                  :auth   true
                  :body   {:email "test@test.com"}}

    (testing "should return status 200"
      (is (= 200 status)))

    (testing "should remove user from db"
      (let [user (db/get-user-info {:email "test@test.com"})]
        (is (nil? user)))))

  (testing-route {:route  "/api/update-jva"
                  :method :post
                  :auth   true
                  :body   {:id          "8d893df0-1afc-4dd6-8e20-eb74a6e4e50b"
                           :announce_no "PSS-2015-311"
                           :position    "New Job Title"
                           :status      true
                           :open_date   "December 22, 2016"
                           :close_date  nil
                           :salary      "Moolah"
                           :location    "Remote"
                           :file_link   "dummyli.nk"}}

    (testing "should return status 200"
      (is (= 200 status)))

    (testing "should modify the jva record in DB"
      (let [{:keys [position close_date salary location]} (db/get-jva {:announce_no "PSS-2015-311"})]
        (is (= "New Job Title" position))
        (is (= nil             close_date))
        (is (= "Moolah"        salary))
        (is (= "Remote"        location)))))

  (with-stub! [[wp/delete-media (constantly nil)]]
    (testing-route {:route "/api/delete-jva"
                    :method :post
                    :auth true
                    :body {:announce_no "PSS-2015-311"}}

      (testing "should return status 200"
        (is (= 200 status)))

      (testing "should remove record of jva from DB"
        (let [jva (db/get-jva {:announce_no "PSS-2015-311"})]
          (is (nil? jva))))

      (testing "should delete related media"
        (is (= 1 (-> wp/delete-media calls count)))
        (is (= "8d893df0-1afc-4dd6-8e20-eb74a6e4e50b" (-> wp/delete-media
                                                          ((comp first calls))
                                                          ((comp first :args))))))))

  (testing "POST /api/update-procurement"
    (with-stub! [[email/notify-subscribers (constantly nil)]]
      (let [new-title "New Title for Proposal #2"
            new-desc  "The description of this proposal has changed."
            rfp       (-> (get-pns-from-db "d0002906-6432-42b5-b82b-35f0d710f827")
                          (#(into {} %))
                          (assoc :title new-title)
                          (assoc :description new-desc))
            orig      (get-pns-from-db (:id rfp))
            {:keys [status body headers]}
            (auth-req :post "/api/update-procurement"
              (assoc :body rfp))]
        (testing "should return status 200"
          (is (= 200 status)))

        (testing "should alter record of rfp in the database"
          (let [{:keys [title description]} (get-pns-from-db "d0002906-6432-42b5-b82b-35f0d710f827")]
            (is (= new-title title))
            (is (= new-desc  description))))

        (testing "should notify subscribers of the updated rfp"
          (is (= 1 (count-calls email/notify-subscribers)))
          (is (= [:update orig (convert-pns-from-map rfp)]
                 (args-from-call email/notify-subscribers))))))

    (with-stub! [[email/notify-subscribers (constantly nil)]]
      (let [new-title "New Title for Invitation #2"
            new-desc  "The description of this invitation has changed."
            ifb       (-> (get-pns-from-db "2fa4e278-f022-4361-b69a-0063a387933a")
                          (#(into {} %))
                          (assoc :title new-title)
                          (assoc :description new-desc))
            orig      (get-pns-from-db (:id ifb))
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
