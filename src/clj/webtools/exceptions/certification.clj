(ns webtools.exceptions.certification)

(defn single-cert-collision [{:keys [cert1 cert2]}]
  (ex-info
   (str "Certificate Collision: " (:cert_no cert1))
   {:orig-cert cert1
    :new-cert cert2}))

(defn list-cert-collisions [collisions]
  (ex-info "Certification Collisions"
           {:err-type :cert-collision
            :errors collisions}))

