(ns webtools.test.tools
  (:require [webtools.handler :refer [app]]
            [webtools.test.constants :as c-t]))

(defn authorize
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
            authorize))))

(defn equal-props?
  [props a b]
  (every? #(= (% a) (% b)) props))

(defn not-equal-props?
  [props a b]
  (not-any? #(= (% a) (% b)) props))
