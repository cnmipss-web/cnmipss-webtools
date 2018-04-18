(ns webtools.exceptions.certification)

(defn single-cert-collision [new-cert orig-cert]
  (ex-info
   (str "Certificate Collision: " (:cert_no orig-cert))
   {:orig-cert orig-cert
    :new-cert new-cert}))

(defn list-cert-collisions [collisions]
  (ex-info "Certification Collisions"
           {:err-type :cert-collision
            :errors collisions}))

