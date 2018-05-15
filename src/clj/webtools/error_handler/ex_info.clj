(ns webtools.error-handler.ex-info
  (:require [clojure.string :as cstr]))

(defmulti code
  (fn [error] (:err-type (ex-data error))))

(defmulti msg
  (fn [error] (:err-type (ex-data error))))

;; Implementations for :cert-collision
(defmethod code :cert-collision [error]
  "cert-collision")

(defmethod msg :cert-collision [error]
  (let [{:keys [err-type errors]} (ex-data error)]
    (cstr/join
     "\n"
     (map
      (fn cert-collision-error-msg [error]
        (let [{:keys [orig-cert new-cert]} (ex-data error)]
          (str "A database collision has occurred with certification "
               (:cert_no orig-cert)
               " for " (:first_name orig-cert) " " (:last_name orig-cert)
               " and " (:cert_no new-cert) " for " (:first_name new-cert) " " (:last_name new-cert)
               ".  Please correct the error.")))
      errors))))

;; Implementations for :wordpress-upload
(defmethod code :wordpress-upload [error]
  "wordpress-upload")

(defmethod msg :wordpress-upload [error]
  (let [{:keys [filename]} (ex-data error)]
    (str "Error uploading " filename " to the public website.  Please contact the webmaster.")))

;; Implementations for :default
(defmethod code :default [error]
  "unknown-info")

(defmethod msg :default [error]
  "Unknown INFO error.  Please contact the developer.")
