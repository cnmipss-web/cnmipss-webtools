(ns webtools.handler-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [webtools.handler :refer :all]
            [webtools.test.fixtures :as fixtures]))


(use-fixtures :once fixtures/prep-db)

(use-fixtures :each fixtures/with-rollback)

(deftest test-home-routes 

  (testing "GET / returns 200"
    (let [response ((app) (mock/request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "GET /webtools redirects to /"
    (let [response ((app) (mock/request :get "/webtools"))]
      (is (= 302 (:status response)))))

  (testing "GET invalid-routes returns 404"
    (let [response ((app) (mock/request :get "/invalid"))]
      (is (= 404 (:status response))))))

(deftest test-oauth-routes

  (testing "GET /oauth/oauth-init redirects to googleapis"
    (let [response ((app) (mock/request :get "/oauth/oauth-init"))
          location (get-in response [:headers "Location"])]
      (is (some? (re-find #"googleapis\.com" location)))
      (is (= 302 (:status response)))))

  (testing "GET /oauth/oauth-callback"
    (println "\nWARNING: GET /oauth/oauth-callback is untested")))



