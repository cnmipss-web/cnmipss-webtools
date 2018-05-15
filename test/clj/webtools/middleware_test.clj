(ns webtools.middleware-test
  (:require [bond.james :refer [calls with-spy]]
            [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [webtools.layout :refer [error-page]]
            [webtools.middleware :as middleware]
            [webtools.test.constants :as c-t]
            [webtools.test.fixtures :as fixtures]))


(def handler identity)

(defn- authorize
  [request]
  (let [auth-cookies {"wt-token" {:value c-t/auth-token
                                  :domain "localhost"
                                  :path "/webtools"}
                      "wt-email" {:value "tyler.collins@cnmipss.org"
                                  :domain "localhost"
                                  :path "/webtools"}}]
    (assoc request :cookies auth-cookies)))

(use-fixtures :once fixtures/prep-db)

(deftest test-wrap-webtools-auth
  (testing "passes authenticated requests to handler"
    (let [request (-> (mock/request :get "/secure/route") authorize)]
      (with-spy [handler]
        ((middleware/wrap-webtools-auth handler) request)
        (is (= 1 (-> handler calls count)))
        (is (= request (-> handler calls first :args first))))))
  (testing "passes unauthenticated requests to the error-page"
    (let [request (mock/request :get "/secure/route")]
      (with-spy [handler error-page]
        ((middleware/wrap-webtools-auth handler) request)
        (is (= 0 (-> handler calls count)))
        (is (= 1 (-> error-page calls count)))
        (is (= {:status 403
                :title "Access Forbidden"
                :message "You do not have permission to access this resource."}
               (-> error-page calls first :args first)))))))
