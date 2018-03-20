(ns webtools.models.procurement.core
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [webtools.spec.procurement]
            [webtools.spec.subscription]))

(defprotocol procurement-to-db
  "Methods for manipulating procurement records"
  (save-to-db     [pns] "Save a record to the DB")
  (change-in-db   [pns] "Update a record in the DB")
  (delete-from-db [pns] "Delete a record from the DB"))

(defprotocol procurement-from-db
  "Methods to retrive procurement records from DB"
  (make-uuid        [id] "Convert a string id to uuid class")
  (get-pns-from-db  [id] "Retrieve an rfp or ifb based on its id")
  (get-subs-from-db [proc_id] "Retrieve all subscriptions to with supplide proc_id"))

(defprotocol create-procurement
  "Method to convert simple maps to procurement records, with validation and type conversion"
  (convert-pns-from-map [map] "Convert HashMap or ArrayMap to webtools.procurement.corePSAnnouncement 
with validation and type conversions.")
  (convert-sub-from-map [map]
    "Convert HashMap or ArrayMap to webtools.models.procurement.core.Subscription 
with validation and type conversions.")
  (convert-add-from-map [map]
    "Convert HashMap or ArrayMap to webtools.models.procurement.core.Addendum 
with validation and type conversions."))

(defprotocol communicate-procurement
  "Methods for generating email notifications regarding procurement announcements"
  (changes-email  [orig new sub])
  (uppercase-type [pns]          "Convert the type keyword to an upper-case string")
  (title-string   [pns]          "Combine the type keyword, announcement number and title into a single string."))

(defrecord PSAnnouncement
    [id
     type
     number
     open_date
     close_date
     title
     description
     file_link
     spec_link])

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
                      (s/gen :webtools.spec.procurement/file_link)
                      (s/gen :webtools.spec.procurement/spec_link))))))

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

(s/fdef make-uuid
        :args (s/cat :id (s/alt :string :webtools.spec.core/uuid-str
                                :uuid :webtools.spec.core/uuid))
        :ret :webtools.spec.core/uuid)

(s/fdef get-pns-from-db
        :args (s/cat :id (s/alt :string :webtools.spec.core/uuid-str
                                :uuid :webtools.spec.core/uuid))
        :ret  (s/or :pns ::procurement
                    :nil nil?))

(s/fdef convert-pns-from-map
        :args (s/cat :map map?)                     
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
                        identity
                        (map
                         (fn [[key val]]
                           (= val (-> record :args key)))
                         record)))))

(s/fdef map->Subscription
        :args (s/cat :map (s/alt :sub :webtools.spec.subscription/record
                                 :nil nil?))
        :ret  (s/or :sub ::subscription
                    :nil nil?)
        :fn   (s/and (fn [record]
                       (every?
                        identity
                        (map
                         (fn [[key val]]
                           (= val (-> record :args key)))
                         record)))))



