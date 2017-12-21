(ns webtools.routes.upload-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [clojure.walk :as walk]
            [clojure.java.io :refer [file]]
            [clj-fuzzy.metrics :as measure]
            [cemerick.url :as curl]
            [ring.mock.request :as mock]
            [bond.james :refer [calls with-spy with-stub!]]
            [clj-time.format :as f]
            [webtools.handler :refer :all]
            [webtools.util :refer :all]
            [webtools.util.dates :as util-dates]
            [webtools.json :refer :all]
            [webtools.config :refer [env]]
            [webtools.email :as email]
            [webtools.db.core :as db]
            [webtools.constants :as const]
            [webtools.test.constants :as test-const]
            [webtools.test.fixtures :as fixtures]
            [webtools.test.tools :refer [auth-req equal-props? not-equal-props?]]
            [webtools.wordpress-api :as wp]
            [conman.core :refer [bind-connection] :as conman]
            [mount.core :as mount]))

(use-fixtures :once fixtures/prep-db fixtures/instrument)

(use-fixtures :each fixtures/with-rollback)

(deftest test-upload-certification
  (testing "POST /upload/certification-csv"
    (testing "should store certifications in the database"
      (let [csv-file (file "test/clj/webtools/test/certificates-clean.csv")
            {:keys [status body headers error] :as response}
            (auth-req :post "/upload/certification-csv"
                      (assoc :params {:file {:tempfile csv-file
                                             :file-name "certificates-clean.csv"
                                             :size (.length csv-file)}}))
            redirect-url (-> (get headers "Location") curl/url)]
        (is (= 302 status))
        (is (nil? error))
        (is (=  "app" (:anchor redirect-url)))
        (is (= {"success" "true" "role" "Certification"} (:query redirect-url)))
        (is (= '("wt-success=true;Path=/webtools;Max-Age=60") (get headers "Set-Cookie")))
        (is (= "" body))))

    (testing "should reject certification collisions that are not renewals"
      (let [csv-file (file "test/clj/webtools/test/certificates-collisions.csv")
            {:keys [status body headers error] :as response}
            (auth-req :post "/upload/certification-csv"
                      (assoc :params {:file {:tempfile csv-file
                                             :file-name "certificates-collisions.csv"
                                             :size (.length csv-file)}}))
            redirect-url (-> (get headers "Location") curl/url)
            msg (-> headers (get "Set-Cookie") first cemerick.url/url-decode)
            {:keys [last_name cert_no first_name cert_type]}
            (-> (re-seq #"(\{.*?\})" msg) second second read-string)
            existing-cert (db/get-cert {:cert_no "BI-003-2006"})]
        (is (= 302 status))
        (is (nil? error))
        (is (=  "app" (:anchor redirect-url)))
        (is (= "false" (get-in redirect-url [:query "success"])))
        (is (= "Certification" (get-in redirect-url [:query "role"])))
        (is (= "" body))
        (is (= "Victor" (:first_name existing-cert)))
        (is (= "Jones" (:last_name existing-cert)))
        (is (= "Basic I" cert_type))
        
        (testing "should respond with a cookie that identifies the rejected certification"
          (is (= "Terra" first_name))
          (is (= "Allen" last_name))
          (is (= "Basic I" cert_type))
          (is (= "BI-003-2006" cert_no)))))

    (testing "should mark certification collisions that are renewals and save them without error"
      (let [csv-file (file "test/clj/webtools/test/certificates-renewal.csv")
            {:keys [status body headers error] :as response}
            (auth-req :post "/upload/certification-csv"
                      (assoc :params {:file {:tempfile csv-file
                                             :file-name "certificates-renewal.csv"
                                             :size (.length csv-file)}}))
            redirect-url (-> (get headers "Location") curl/url)
            S-03-127 (db/get-cert {:cert_no "S-03-127"})
            S-03-127-renewal (db/get-cert {:cert_no "S-03-127-renewal-1"})
            S-04-095 (db/get-cert {:cert_no "S-04-095"})
            S-04-095-renewal (db/get-cert {:cert_no "S-04-095-renewal-1"})]
        (is (= 302 status))
        (is (nil? error))
        (is (=  "app" (:anchor redirect-url)))
        (is (= {"success" "true" "role" "Certification"} (:query redirect-url)))
        (is (= "" body))

        (is (equal-props? [:first_name :last_name :cert_type] S-03-127 S-03-127-renewal))
        (is (not-equal-props? [:start_date :expiry_date :cert_no] S-03-127 S-03-127-renewal))
        
        (is (equal-props? [:first_name :last_name :cert_type] S-04-095 S-04-095-renewal))
        (is (not-equal-props? [:start_date :expiry_date :cert_no] S-04-095 S-04-095-renewal))))))

(deftest test-upload-hro
  (testing "POST /upload/jva-pdf"
    (with-stub! [[wp/create-media (constantly "http://nil")]]
      (let [pdf (file "test/clj/webtools/test/jva-sample.pdf")
            {:keys [status body headers]}
            (auth-req :post "/upload/jva-pdf"
                      (assoc :params {:file {:tempfile pdf :filename "jva-sample.pdf" :size (.length pdf)}}))]
        (testing "should redirect after successful upload"
          (is (= 302 status)))

        (testing "should store jva info in DB"
          (let [jva (db/get-jva {:announce_no "PSS-2017-041"})]
            (is (= (util-dates/parse-date "August 4, 2017") (:open_date jva)))
            (is (= (util-dates/parse-date "August 18, 2017") (:close_date jva)))))

        (testing "should create wp media"
          (is (= 1 (-> wp/create-media calls count)))
          (is (= java.util.UUID (-> wp/create-media calls first :args last type))))))))

(deftest test-upload-p&s
  (testing "POST /upload/procurement-pdf"
    (with-stub! [[wp/create-media (constantly "http://nil")]]
      (testing "should handle uploaded rfps"
        (let [pdf (file "test/clj/webtools/test/rfp-sample.pdf")
              spec (file "test/clj/webtools/test/rfp-sample.pdf")
              {:keys [status body headers error params] :as response}
              (auth-req :post "/upload/procurement-pdf"
                        (assoc :params {:ann-file {:tempfile pdf :filename "rfp-sample.pdf" :size (.length pdf)}
                                        :spec-file {:tempfile spec :filename "rfp-specs.pdf" :size (.length spec)}}))]

          (testing "should redirect after successful upload"
            (is (= 302 status))
            (is (nil? error))
            (is (= '("wt-success=true;Path=/webtools;Max-Age=60") (get headers "Set-Cookie"))))

          (testing "should store rfp info in postgres database"
            (let [rfp (db/get-pnsa-by-no {:number "17-041"})]
              (is (some? rfp))
              (is (= "Training on the Foundations of Reading and Guided Reading" (:title rfp)))))

          (testing "should create wp media"
            (is (= 2 (-> wp/create-media calls count)))
            (is (= java.util.UUID (-> wp/create-media calls first :args last type))))))

      (testing "should handle uploaded ifbs"
        (let [pdf (file "test/clj/webtools/test/ifb-sample.pdf")
              spec (file "test/clj/webtools/test/rfp-sample.pdf")
              {:keys [status body headers error params] :as response}
              (auth-req :post "/upload/procurement-pdf"
                        (assoc :params {:ann-file {:tempfile pdf :filename "ifb-sample.pdf" :size (.length pdf)}
                                        :spec-file {:tempfile spec :filename "ifb-specs.pdf" :size (.length spec)}}))]

          (testing "should redirect after successful upload"
            (is (= 302 status))
            (is (nil? error))
            (is (= '("wt-success=true;Path=/webtools;Max-Age=60") (get headers "Set-Cookie"))))

          (testing "should store ifb info in postgres database"
            (let [ifb (db/get-pnsa-by-no {:number "17-051"})]
              (is (some? ifb))
              (is (= "Purchase of 1 (One) Riding Mower for the Public School System" (:title ifb)))))

          (testing "should create wp media"
            (is (= 4 (-> wp/create-media calls count))))))

      (testing "should respond with helpful error messages to malformed dates"
        (let [pdf (file "test/clj/webtools/test/rfp-malformed-dates.pdf")
              {:keys [status body headers error params] :as response}
              (auth-req :post "/upload/procurement-pdf"
                        (assoc :params {:ann-file {:tempfile pdf :filename "sample-rfp.pdf" :size (.length pdf)}
                                        :spec-file {:tempfile pdf :filename "sample-spec.pdf" :size (.length pdf)}}))]

          (testing "should redirect after failed upload"
            (is (= 302 status)))
          
          (testing "should supply an error code via url params"
            (let [location (-> response :headers (get "Location") curl/url)
                  error (get-in location [:query "error"])]
              (is (= "bad-date" error))))

          (testing "should supply an error message via cookie"
            (let [cookies (-> response :headers (get "Set-Cookie"))]
              (is (some (fn [cookie]
                          (some?
                           (re-seq
                            #"wt\-error\=One\+of\+the\+required\+dates\+is\+incorrectly\+formatted."
                            cookie)))
                        cookies))))))

      (testing "should handle multiline titles and dates"))))
  
(deftest test-upload-p&s-addendum
  ;; This is seperated from test-upload-p&s because the DB changes made by those tests cause this one to fail
  (testing "POST /upload/procurement-addendum"
    (with-stub! [[wp/create-media (constantly "http://cnmipss.org/file-link.pdf")]
                 [email/notify-subscribers (constantly nil)]]
      (testing "should store uploaded addenda"
        (let [pdf (file "test/clj/webtools/test/rfp-sample.pdf")
              {:keys [status body headers params] :as response}
              (auth-req :post "/upload/procurement-addendum"
                        (assoc :params {:file {:tempfile pdf :filename "sample-rfp.pdf" :size (.length pdf)}
                                        :id "d0002906-6432-42b5-b82b-35f0d710f827"
                                        :type :rfp
                                        :number "18-001"}))]
          (testing "should redirect after successful upload"
            (is (= 302 status))
            (is (= '("wt-success=true;Path=/webtools;Max-Age=60") (get headers "Set-Cookie"))))

          (testing "should store addendum info in postgres DB")

          (testing "should create wp media"))))))
