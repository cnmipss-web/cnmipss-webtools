(ns certification-db.test.handler
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [certification-db.handler :refer :all]
            [certification-db.util :refer :all]
            [certification-db.db.core :as db]))

(deftest test-app
  (testing "main route"
    (let [response ((app) (mock/request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "not-found route"
    (let [response ((app) (mock/request :get "/invalid"))]
      (is (= 404 (:status response))))))

(deftest test-oauth
  ())

(deftest test-api-user
  (testing "GET /api/user"
    (testing "it should respond with JSON"
      (let [response ((app) (mock/request :get "/api/user?email=tyler.collins@cnmipss.org"))
            body  (json->edn (response :body))]
        (is (= (type body) clojure.lang.PersistentArrayMap))))
    (testing "it should retrieve user info from the database given a valid user email"
      (let [response ((app) (mock/request :get "/api/user?email=tyler.collins@cnmipss.org"))
            body (json->edn (response :body))
            {:keys [status user]} body]
        (is (= 200 status))
        (is (= "tyler.collins@cnmipss.org" (get user 1)))
        (is (= "Tyler Collins" (get user 2)))
        (is (= "admin" (get user 3)))))
    (testing "it should return 404 for emails not found in DB"
      (let [response ((app) (mock/request :get "/api/user?email=wrong@notreal.lol"))
            {:keys [status]} response]
        (is (= 404 status))))
    (testing "it should return 400 for invalid emails in query"
      (let [response ((app) (mock/request :get "/api/user"))
            {:keys [status]} response]
        (is (= 400 status))))))

(deftest test-api-verifytoken
  (testing "POST /api/verify-token"
    (testing "it should respond status 200 if token/email combo match DB"
      (let [correct-token (db/get-user-token "tyler.collins@cnmipss.org")
            response ((app) (-> (mock/request :post "/api/verify-token")
                                (mock/body (edn->json {:email "tyler.collins@cnmipss.org" :token correct-token}))
                                (mock/header "Content-Type" "application/json")))
            {:keys [status]} response]
        (is (= 200 status))))
    (testing "it should respond status 403 if token/email combo do not match DB"
      (let [response ((app) (-> (mock/request :post "/api/verify-token")
                                (mock/body (edn->json {:email "tyler.collins@cnmipss.org" :token "token"}))
                                (mock/header "Content-Type" "application/json")))
            {:keys [status]} response]
        (is (= 403 status))))))
