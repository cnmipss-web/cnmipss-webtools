(ns webtools.procurement.core
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [webtools.spec.procurement]
            [webtools.spec.subscription]))

(defprotocol process-procurement
  "Methods for manipulating procurement records"
  (save-to-db [a] "Save a record to the DB")
  (change-in-db [a] "Update a record in the DB")
  (delete-from-db [a] "Delete a record from the DB")
  (for-json [a] "Prep a record to be converted to JSON")
  (proc-type [a] "Returns a records type (:rfp or :ifb)"))

(defprotocol retrieve-procurement
  "Methods to retrive procurement records from DB"
  (make-uuid [id] "Convert an id to uuid class")
  (get-pns-from-db [id] "Retrieve an rfp or ifb based on its id"))

(defprotocol create-procurement
  "Method to convert simply map to PSAnnouncement with relevant type checks"
  (pns-from-map [pns]))

(defprotocol communicate-procurement
  "Methods for generating email notifications regarding procurement announcements"
  (changes-email [orig new sub]))

(defrecord PSAnnouncement
    [id
     type
     number
     open_date
     close_date
     title
     description
     file_link])

(defrecord Addendum
    [id
     type
     proc_id
     file_link
     addend_number])

(defrecord Subscription
    [id
     proc_id
     subscription_number
     company_name
     contact_person
     email
     telephone])

(s/def ::procurement
  (s/with-gen
    :webtools.spec.procurement/record
    (fn [] (gen/fmap (partial apply ->PSAnnouncement)
                     (gen/tuple
                      (s/gen :webtools.spec.procurement/id)
                      (s/gen :webtools.spec.procurement/type)
                      (s/gen :webtools.spec.procurement/number)
                      (s/gen :webtools.spec.procurement/open_date)
                      (s/gen :webtools.spec.procurement/close_date)
                      (s/gen :webtools.spec.procurement/title)
                      (s/gen :webtools.spec.procurement/description)
                      (s/gen :webtools.spec.procurement/file_link))))))

(s/def ::subscription
  (s/with-gen
    :webtools.spec.subscription/record
    (fn [] (gen/fmap (partial ->Subscription)
                     (gen/tuple
                      (s/gen :webtools.spec.subscription/id)
                      (s/gen :webtools.spec.subscription/proc_id)
                      (s/gen :webtools.spec.subscription/subscription_number)
                      (s/gen :webtools.spec.subscription/company_name)
                      (s/gen :webtools.spec.subscription/contact_person)
                      (s/gen :webtools.spec.subscription/email)
                      (s/gen :webtools.spec.subscription/telephone))))))

(s/fdef proc-type
        :args (s/cat :a ::procurement)
        :ret #{:rfp :ifb})

(s/fdef make-uuid
        :args (s/cat :id (s/alt :string :webtools.spec.core/uuid-str
                                :uuid :webtools.spec.core/uuid))
        :ret :webtools.spec.core/uuid)

(s/fdef get-pns-from-db
        :args (s/cat :id (s/alt :string :webtools.spec.core/uuid-str
                                :uuid :webtools.spec.core/uuid))
        :ret  (s/or :pns ::procurement
                    :nil nil?))

(s/fdef pns-from-map
        :args (s/cat :map :webtools.spec.procurement/record)
        :ret ::procurement)

(s/fdef changes-email
        :args (s/cat :orig ::procurement
                     :new ::procurement
                     :sub :webtools.spec.subscription/record)
        :ret vector?)

(s/fdef map->PSAnnouncement
        :args (s/cat :map (s/alt :pns :webtools.spec.procurement/record
                                 :nil nil?))
        :ret  (s/or :pns ::procurement
                    :sub nil?)
        :fn   (s/and (fn [record]
                       (every?
                        (map
                         (fn [[key val]]
                           (= val (-> record :args key))))))))

(s/fdef map->Subscription
        :args (s/cat :map (s/alt :sub :webtools.spec.subscription/record
                                 :nil nil?))
        :ret  (s/or :sub ::subscription
                    :nil nil?)
        :fn   (s/and (fn [record]
                       (every?
                        (map
                         (fn [[key val]]
                           (= val (-> record :args key))))))))



