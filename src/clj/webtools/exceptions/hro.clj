(ns webtools.exceptions.hro)

(defn mismatched-jvas [jva1 jva2]
  (ex-info
   (str "JVAs do not have matching numbers: #"
        (:announce_no jva1)
        " vs. #"
        (:announce_no jva2))
   {:err-type :mismatched-jvas}))
