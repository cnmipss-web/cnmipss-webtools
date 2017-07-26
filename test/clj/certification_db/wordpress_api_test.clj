(ns certification-db.wordpress-api-test
  (:require [certification-db.wordpress-api :as wp]
            [certification-db.test.fixtures :as fixtures]
            [certification-db.constants :refer [wp-token-route wp-media-route]]
            [certification-db.test.constants :as c-t]
            [certification-db.json :refer :all]
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
