(ns webtools.test.tools
  (:require [webtools.handler :refer [app]]
            [webtools.test.constants :as c-t]))

(defn authorize
  "Add wt-token and wt-email cookies to a request for authentication."
  [{:keys [cookies] :as request}]
  (let [auth-cookies {"wt-token" {:value c-t/auth-token
                                  :domain "localhost"
                                  :path "/webtools"}
                      "wt-email" {:value "tyler.collins@cnmipss.org"
                                  :domain "localhost"
                                  :path "/webtools"}}]
    (assoc request :cookies (merge {} cookies auth-cookies))))

(defmacro auth-req
  "Perform an authorized request by applying wt-token and wt-email headers
  matching an authenticated user from the test-seed DB."
  {:style/indent 2}
  ([method url] `(auth-req ~method ~url (identity)))
  ([method url & body]
   `((app) (-> (mock/request ~method ~url)
               ~@body
               authorize))))

(defmacro unauth-req
  ""
  {:style/indent 2}
  ([method url] `(unauth-req ~method ~url (identity)))
  ([method url & body]
   `((app) (-> (mock/request ~method ~url)
               ~@body))))

(defn equal-props?
  "Return true if EVERY key in ks is associated with the same value in m and n"
  [m n & ks]
  (every? #(= (% m) (% n)) ks))

(defn not-equal-props?
  "Return true if NONE of the keys in ks is associated with an identical value 
  in m and n"
  [m n & ks]
  (not-any? #(= (% m) (% n)) ks))

