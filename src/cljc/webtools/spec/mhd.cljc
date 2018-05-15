(ns webtools.spec.mhd
  (:require [clojure.spec.alpha :as spec]))

(spec/def ::title string?)
(spec/def ::description string?)
(spec/def ::ticket_queue_id int?)
(spec/def ::priority_id #{10 20 30 40})
(spec/def ::status_id #{10 20 30 40 50 60})
(spec/def ::ticket_type_id int?)
(spec/def ::assigned_to_id int?)
(spec/def ::ticket_form_id (spec/nilable int?))
(spec/def ::custom_fields (spec/nilable map?))
(spec/def ::user_id int?)
(spec/def ::cc string?)

(spec/def ::ticket (spec/keys :req-un [::title
                                       ::description
                                       ::ticket_queue_id
                                       ::priority_id
                                       ::status_id
                                       ::ticket_type_id
                                       ::assigned_to_id
                                       ::ticket_form_id
                                       ::custom_fields
                                       ::user_id
                                       ::cc]))
