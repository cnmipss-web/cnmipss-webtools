(ns webtools.procurement.core)

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
     target
     file_link
     addendum_number])

(defrecord Subscription
    [id
     type
     target
     subscription_number
     company_name
     contact_person
     email
     telephone])
