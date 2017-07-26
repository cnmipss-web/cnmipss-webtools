(ns certification-db.handler-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [clojure.walk :as walk]
            [ring.mock.request :as mock]
            [certification-db.handler :refer :all]
            [certification-db.util :refer :all]
            [certification-db.json :refer :all]
            [certification-db.config :refer [env]]
            [certification-db.db.core :as db]
            [certification-db.test.constants :as c-t]
            [certification-db.test.fixtures :as fixtures]
            [conman.core :refer [bind-connection] :as conman]
            [mount.core :as mount]))


(defn- authorize
  [request]
  (let [auth-cookies {"wt-token" {:value c-t/auth-token
                                  :domain "localhost"
                                  :path "/webtools"}
                      "wt-email" {:value "tyler.collins@cnmipss.org"
                                  :domain "localhost"
                                  :path "/webtools"}}]
    (assoc request :cookies auth-cookies)))

(defmacro auth-req
  ([method url] `(auth-req ~method ~url (identity)))
  ([method url & body]
   `((app) (-> (mock/request ~method ~url)
               ~@body
               authorize
               ))))

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

(deftest test-api-routes
  
  (testing "GET /api/all-certs should return a JSON list of certification records"
    (let [{:keys [status body headers error]} ((app) (mock/request :get "/api/all-certs"))
          json-body (json->edn body)]
      (is (= 200 status))
      (is (nil? error))
      (is (= "application/json" (get headers "Content-Type")))
      (is (= 4 (count json-body)))
      (is (= "BI-003-2006" (-> json-body first :cert_no)))))
  
  (testing "GET /api/all-jvas should return a JSON list of jva records"
    (let [{:keys [status body headers error]} ((app) (mock/request :get "/api/all-jvas"))
          json-body (json->edn body)]
      (is (= 200 status))
      (is (nil? error))
      (is (= "application/json" (get headers "Content-Type")))
      (is (= 3 (count json-body)))
      (is (= "PSS-2015-311" (-> json-body first :announce_no)))))
  
  (testing "POST /api/verify-token"
    (println "\nWARNING: POST /api/verify-token is untested"))
  
  (testing "GET /logout should remove auth cookies and redirect to #/login"
    (let [{:keys [status body headers error cookies]}
          (auth-req :get "/logout")
          wt-token (-> headers (get "Set-Cookie") first)
          wt-email (-> headers (get "Set-Cookie") second)]
      (is (= 302 status))
      (is (some? (re-find #"wt-token=;Max-Age=1" wt-token)))
      (is (some? (re-find #"wt-email=;Max-Age=1" wt-email)))
      (is (some? (re-find #"#/login" (get headers "Location")))))))


(deftest test-api-routes-with-auth
  
  (testing "GET /api/user"
    (let [{:keys [status body headers error]}
          (auth-req :get "/api/user")]
      (is (= 200 status))
      (is (nil? error))
      (is (=  java.lang.String (type body)))
      (let [json-body (json->edn body)
            {user :user} json-body]
        (is (= clojure.lang.PersistentArrayMap (type json-body)))
        (is (= "tyler.collins@cnmipss.org" (:email user)))
        (is (:admin user))
        (is (= c-t/auth-token (:token user))))))

  (testing "GET /api/all-users"
    (let [{:keys [status body headers error]}
          (auth-req :get "/api/all-users")]
      (is (= 200 status))
      (is (nil? error))
      (is (= java.lang.String (type body)))
      (let [json-body (json->edn body)]
        (is (= clojure.lang.LazySeq (type json-body)))
        (is (= 5 (count json-body)))
        (is (every? #(#{:email :roles :admin} (first (first %))) json-body)))))

  (testing "POST /api/update-user"
    (let [user-data {:email "tony.stark@cnmipss.org" :roles "Procurement,Manage Users" :admin true}
          {:keys [status body headers error]}
          (auth-req :post "/api/update-user"
                    (mock/body (edn->json user-data))
                    (mock/header "Content-Type" "application/json"))]
      (is (= 200 status))
      (is (nil? error))
      (is (= java.lang.String (type body)))
      (let [json-body (json->edn body)
            user (first (filter #(= (:email user-data) (:email %)) json-body))]
        (is (= clojure.lang.LazySeq (type json-body)))
        (is (= 5 (count json-body)))
        (is (every? #(#{:email :roles :admin} (first (first %))) json-body))
        (is (= "Procurement,Manage Users" (:roles user)))
        (is (:admin user)))))

  (testing "POST /api/create-user"
    (let [user-data {:email "bruce.banner@cnmipss.org" :roles "Manage DB,Manage Users" :admin false}
          {:keys [status body headers error]}
          (auth-req :post "/api/create-user"
                    (mock/body (edn->json user-data))
                    (mock/header "Content-Type" "application/json"))]
      (is (= 200 status))
      (is (nil? error))
      (is (= java.lang.String (type body)))
      (let [json-body (json->edn body)
            user (first (filter #(= (:email user-data) (:email %)) json-body))]
        (is (= clojure.lang.LazySeq (type json-body)))
        (is (= 6 (count json-body)))
        (is (every? #(#{:email :roles :admin} (first (first %))) json-body))
        (is (= "Manage DB,Manage Users" (:roles user)))
        (is (not (:admin user))))))

  (testing "POST /api/delete-user"
    (let [user-data {:email "tony.stark@cnmipss.org"}
          {:keys [status body headers error]}
          (auth-req :post "/api/delete-user"
                    (mock/body (edn->json user-data))
                    (mock/header "Content-Type" "application/json"))]
      (is (= 200 status))
      (is (nil? error))
      (is (= java.lang.String (type body)))
      (let [json-body (json->edn body)
            user (first (filter #(= (:email user-data) (:email %)) json-body))]
        (is (= clojure.lang.LazySeq (type json-body)))
        (is (= 5 (count json-body)))
        (is (nil? user)))))

  (testing "POST /api/update-jva"
    (let [jva-data {:announce_no "PSS-2015-311"
                    :position "Early Head Start Family Partnership Advocate (Re-"
                    :status true
                    :open_date "July 14, 2017"
                    :close_date "September 1, 2017"
                    :salary "PAY LEVEL: 20 STEP(S): 02-12; $15,105.87 - $24,584.23 Per Annum"
                    :location "Early Head Start/Head Start Program, Saipan"
                    :file_link "http://localhost.test/wp-content/uploads/2017/07/PSS-2015-311-Family-Partnership-Advocate_EHS_HDST_ReAnnouncement.pdf"}
          {:keys [status body headers error]}
          (auth-req :post "/api/update-jva"
                    (mock/body (edn->json jva-data))
                    (mock/header "Content-Type" "application/json"))]
      (is (= 200 status))
      (is (nil? error))
      (is (= java.lang.String (type body)))
      (let [json-body (json->edn body)
            jva (first (filter #(= (:announce_no jva-data) (:announce_no %)) json-body))]
        (is (= 3 (count json-body)))
        (is (= "September 01, 2017" (:close_date jva))))))

  (testing "POST /api/delete-jva"
    (let [jva-data {:announce_no "PSS-2015-311"
                    :position "Early Head Start Family Partnership Advocate (Re-"
                    :status true
                    :open_date "July 14, 2017"
                    :close_date "September 1, 2017"
                    :salary "PAY LEVEL: 20 STEP(S): 02-12; $15,105.87 - $24,584.23 Per Annum"
                    :location "Early Head Start/Head Start Program, Saipan"
                    :file_link "http://localhost.test/wp-content/uploads/2017/07/PSS-2015-311-Family-Partnership-Advocate_EHS_HDST_ReAnnouncement.pdf"}
          {:keys [status body headers error]}
          (auth-req :post "/api/delete-jva"
                    (mock/body (edn->json jva-data))
                    (mock/header "Content-Type" "application/json"))]
      (is (= 200 status))
      (is (nil? error))
      (is (= java.lang.String (type body)))
      (let [json-body (json->edn body)
            jva (first (filter #(= (:announce_no jva-data) (:announce_no %)) json-body))]
        (is (= 2 (count json-body)))
        (is (nil? jva))))))

(deftest test-upload-routes
  (testing "POST /upload/certification-csv"
    (println "\nWARNING: POST /upload/certification-csv is untested"))

  (testing "POST /upload/jva-pdf"
    (println "\nWARNING: POST /upload/jva-pdf is untested")))

