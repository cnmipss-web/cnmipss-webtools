(ns webtools.procurement.core
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]))

(defprotocol process-procurement
  "Methods for manipulating procurement records"
  (save-to-db [a] "Save a record to the DB")
  (change-in-db [a] "Update a record in the DB")
  (changes-email [orig new sub] "Create hiccup markup for an email notifying subscribers about changes")
  (delete-from-db [a] "Delete a record from the DB")
  (for-json [a] "Prep a record to be converted to JSON"))

(defprotocol retrieve-procurement
  "Methods to retrive procurement records from DB"
  (make-uuid [id] "Convert an id to uuid class")
  (get-pns-from-db [id] "Retrieve an rfp or ifb based on its id"))

(defprotocol create-procurement
  "Method to convert simply map to PSAnnouncement with relevant type checks"
  (pns-from-map [pns]))

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
     type
     proc_id
     subscription_number
     company_name
     contact_person
     email
     telephone])

(s/def ::not-nil some?)
(s/def ::id (s/with-gen
              #(instance? #?(:clj  java.util.UUID
                             :cljs cljs.core/UUID) %)
              gen/uuid))

(s/def ::type keyword?)
(s/def ::number string?)
(s/def ::date (s/with-gen
                (partial instance? #?(:clj org.joda.time.DateTime
                                      :cljs js/Function))
                (fn [] (gen/fmap #(org.joda.time.DateTime. %) (s/gen pos-int?)))))
(s/def ::title string?)
(s/def ::desc string?)
(s/def ::link string?)
(s/def ::company_name string?)
(s/def ::contact_person string?)
(s/def ::email #(some? (re-find #"^(([^<>()\[\]\\.,;:\s@\"]+(\.[^<>()\[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$" %)))
(s/def ::telephone #(some? (re-find #"^(\+\d{1,2}\s)?(\(?\d{3}\)?)?[\s.-]?\d{3}[\s.-]?\d{4}$" %)))

(s/fdef ->PSAnnouncement
        :args (s/cat :id ::id
                     :type ::type
                     :number ::number
                     :open_date ::date
                     :close_date ::date
                     :title ::title
                     :description ::desc
                     :file_link ::link)
        :ret (partial instance? webtools.procurement.core.PSAnnouncement)
        :fn (s/and #(= (-> % :ret :id) (-> % :args :id))))
