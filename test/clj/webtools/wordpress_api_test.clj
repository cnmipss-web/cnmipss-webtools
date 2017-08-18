(ns webtools.wordpress-api-test
  (:require [webtools.wordpress-api :refer [wp-auth-token] :as wp]
            [webtools.config :refer [env]]
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
        (is (= (str (:wp-host env) wp-token-route)
               (-> http/post calls first :args first)))))))

(deftest test-create-media
  (testing "posts a file to wordpress server"
    (with-stub! [[http/post (constantly {:body (edn->json {:source_url "https://dummyl.ink"})})]]
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
          (let [url (str "http://localhost//wp-json/wp/v2/media?caption=Testing123&date=today&slug="
                         slug "&alt_text=alt-text&title=Title&author=SomeDude&")]
            (is (= 2 (-> http/post calls count)))
            (is (= url (-> http/post calls second :args first)))))))))

(deftest test-delete-media
  (testing "deletes a file from the wordpress server"
    (with-stub! [[http/delete (constantly nil)]
                 [http/get (constantly {:body (edn->json [{:id "1234"}])})]]
      (let [slug "f9869a17-7a64-40a9-ba5b-83e53f855268"
              url (str (:wp-host env) wp-media-route "/1234")]
        (wp/delete-media slug)
        (testing "should http/get id for media url"
          (is (= 1 (-> http/get calls count))))
        
        (testing "should call http/delete with correct url"
          (is (= 1 (-> http/delete calls count)))
          (is (= url (-> http/delete calls first :args first))))))))
