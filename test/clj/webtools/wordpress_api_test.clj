(ns webtools.wordpress-api-test
  (:require [bond.james :refer [calls with-stub! with-stub-ns]]
            [clj-http.client :as http]
            [clojure.test :refer :all]
            [webtools.config :refer [env]]
            [webtools.constants :refer [wp-media-route wp-token-route]]
            [webtools.json :refer :all]
            [webtools.test.fixtures :as fixtures]
            [webtools.wordpress-api :as wp]))

(use-fixtures :once fixtures/prep-db)

(deftest test-wp-auth-token
  (testing "returns a string from wp-token-route containing JWT from wp"
    (with-stub! [[http/post (constantly {:body (data->json {:token "JWT from wp"})})]]
      (let [token (wp/wp-auth-token)]
        (is (= "Bearer JWT from wp" token))
        (is (= 1 (-> http/post calls count)))
        (is (= (str (:wp-host env) wp-token-route)
               (-> http/post calls first :args first)))))))

(deftest test-create-media
  (testing "POSTs a file to wordpress server, then GETs information about that file"
    (with-stub! [[http/post (constantly {:body (data->json {})})]
                 [http/get (constantly {:body (data->json {:source_url "https://dummyl.ink"})})]]
      (let [slug (java.util.UUID/randomUUID)
            file_link (wp/create-media "test.pdf" "test/clj/webtools/test/jva-sample.pdf"
                                       :date "today"
                                       :slug slug
                                       :title "Title"
                                       :author "SomeDude"
                                       :alt_text "alt-text"
                                       :caption "Testing123")]

        (testing "should return file_link"
          (is (= "https://dummyl.ink" file_link)))

        (testing "should call http/post with correct url"
          (let [{:keys [wp-host]} env
                url (str wp-host wp-media-route "?caption=Testing123&date=today&slug="
                         slug "&alt_text=alt-text&title=Title&author=SomeDude&")]
            (is (= 1 (-> http/get calls count)))
            (is (= 3 (-> http/post calls count)))
            (is (= url (-> http/post calls second :args first)))))))))

(deftest test-delete-media
  (testing "DELETEs a file from the wordpress server"
    (with-stub-ns [[clj-http.client (constantly {:body (data->json [{:id "1234"}])})]]
      (let [slug "f9869a17-7a64-40a9-ba5b-83e53f855268"
            url (str (:wp-host env) wp-media-route "/1234")]
        (wp/delete-media slug)

        (testing "should http/get id for media url"
          (is (= 1 (-> http/get calls count))))

        (testing "should call http/delete with correct url"
          (is (= 1 (-> http/delete calls count)))
          (is (= url (-> http/delete calls first :args first))))))))
