(ns certification-db.test.constants)

(def dummy-user {:id (java.util.UUID/randomUUID)
                 :email "email"
                 :admin false
                 :roles "roles"})

(def dummy-jva {:id (java.util.UUID/randomUUID)
                :announce_no "1234567890"
                :position "Fake Job"
                :status false
                :open_date (java.sql.Date. -63517978400000)
                :close_date nil
                :salary "Beware the Ides of March"
                :location "Senate Steps"
                :file_link "Et tu Brute?"})
(def dummy-cert {:cert_no "1234567890"
                 :last_name "Caesar"
                 :first_name "Gaius"
                 :mi "J"
                 :cert_type "Dictator for Life"
                 :start_date "Crossing the Rubicon"
                 :expiry_date "The Ides of March"})

(def user-seed-count 5)
(def cert-seed-count 4)
(def jva-seed-count 3)

(def auth-token "ya29.GluTBHe_gy2R2PBdSedi3oZKT64AltZN7EfIQKReuLOWcdMjySQnh5VeSCLC8-_aG1wdhaBrT4baVSvWnrDoiK5z3_nJkdKpfAhiXI1c2cenTSJyd8sx-dpqBm0B")
