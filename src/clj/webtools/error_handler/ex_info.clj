(ns webtools.error-handler.ex-info)

(defprotocol ReportError
  (code [x] "Return an error-code based on the type of error in terms of our data model.")
  (msg [x]  "A human friendly error message based on the type of error in terms of our data model"))

(defn- cert-collision-error-msg [error]
  (let [{:keys [orig-cert new-cert]} (ex-data error)]
    (str "A database collision has occurred between certification "
         (:cert_no orig-cert)
         " for " (:first_name orig-cert) " " (:last_name orig-cert)
         " and " (:cert_no new-cert) " for " (:first_name new-cert) " " (:last_name new-cert)
         ".  Please correct the error.")))

(defrecord cert-collision-error [error]
  ReportError
  (code [{:keys [error]}] "cert-collision")
  (msg  [{:keys [error]}]
    (let [{:keys [err-type errors]} (ex-data error)]
      (clojure.string/join
       "\n"
       (map
        cert-collision-error-msg
        errors)))))

(defrecord unknown-error [error]
  ReportError
  (code [x] "unknown-info")
  (msg  [x] "Unknown INFO error.  Please contact the developer."))

(defn cert-collision? [error]
  (= :cert-collision (:err-type (ex-data error))))

(defn err-type
  "Determine the type of error in terms of our data model"
  [error]
  (cond
    (cert-collision? error) (->cert-collision-error error)
    :else (->unknown-error error)))


(extend-protocol ReportError
  clojure.lang.ExceptionInfo
  (code [err]
    (code (err-type err)))
  (msg  [err]
    (msg  (err-type err))))
