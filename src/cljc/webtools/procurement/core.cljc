(ns webtools.procurement.core
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [webtools.spec.procurement]))

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

;;Specs

(s/fdef map->PSAnnouncement
        :args :webtools.spec.procurement/record
        :ret (partial instance? webtools.procurement.core.PSAnnouncement)
        :fn (s/and #(= (-> % :ret :id) (-> % :args :id))))
