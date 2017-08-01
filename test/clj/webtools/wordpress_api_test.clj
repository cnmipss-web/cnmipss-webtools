(ns webtools.wordpress-api-test
  (:require [webtools.wordpress-api :as wp]
            [webtools.test.fixtures :as fixtures]
            [webtools.constants :refer [wp-token-route wp-media-route]]
            [webtools.test.constants :as c-t]
            [webtools.json :refer :all]
            [clojure.test :refer :all]
            [clj-http.client :as http]
            [bond.james :refer [calls with-spy with-stub!]]))

(use-fixtures :once fixtures/prep-db)

(deftest test-wp-auth-token
  (testing "returns a string from wp-token-route containing JWT from wp"
    (with-stub! [[http/post (constantly {:body (edn->json {:token "JWT from wp"})})]]
      (let [token (wp/wp-auth-token)]
        (is (= "Bearer JWT from wp" token))
        (is (= 1 (-> http/post calls count)))
        (is (= wp-token-route (-> http/post calls first :args first)))))))
