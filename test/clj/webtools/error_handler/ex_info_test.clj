(ns webtools.error-handler.ex-info-test
  (:require [clojure.test :refer :all]
            [clojure.string :as cstr]
            [webtools.test.fixtures :as fixtures]
            [webtools.exceptions.certification :as cex]
            [webtools.exceptions.procurement :as pex]
            [webtools.error-handler.ex-info :as info-error]))

(use-fixtures :once fixtures/instrument)

(deftest test-cert-collision
  (let [error (cex/list-cert-collisions
               [(cex/single-cert-collision {} {})
                (cex/single-cert-collision {} {})])
        code  (info-error/code error)
        msg   (info-error/msg error)]
    (is (= "cert-collision" code))
    (is (cstr/includes? msg "A database collision has occurred with certification"))))

(deftest test-wordpress-upload
  (let [error (pex/wordpress-upload-failed "filename.pdf")
        code  (info-error/code error)
        msg   (info-error/msg error)]
    (is (= "wordpress-upload" code))
    (is (= "Error uploading filename.pdf to the public website.  Please contact the webmaster." msg))))
