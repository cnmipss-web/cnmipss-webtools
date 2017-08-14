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
  (testing "GET /api/all-certs")

  (testing "GET /api/all-jvas")

  (testing "GET /api/all-procurement")

  (testing "POST /api/subscribe-procurement"
    )

  (testing "POST /api/verify-token")

  (testing "GET /logout"))

(deftest test-api-routes-with-auth
  (testing "GET /api/refresh-session")

  (testing "GET /api/user")

  (testing "GET /api/all-users")

  (testing "POST /api/create-user")

  (testing "POST /api/delete-user")

  (testing "POST /api/delete-user")

  (testing "POST /api/update-jva")

  (testing "POST /api/delete-jva")

  (testing "POST /api/update-rfp")

  (testing "POST /api/delete-rfp")

  (testing "POST /api/update-ifb")

  (testing "POST /api/delete-ifb"))
