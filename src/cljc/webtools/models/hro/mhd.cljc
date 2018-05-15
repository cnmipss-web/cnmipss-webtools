(ns webtools.models.hro.mhd)

(defrecord MHDTicket
    [title
     description
     ticket_queue_id
     priority_id
     status_id
     ticket_type_id
     assigned_to_id
     ticket_form_id
     user_id
     cc
     custom_fields])

(defmulti ticket-fetch
  "Multimethod for fetching MHD tickets using MHD API.  Arguments be one of the 
  following values:

    k    -- :list, :single
    args -- paramenters to append to query"
  (fn [k & args] k))

(defprotocol handle-tickets
  (create-ticket  [ticket] "Create a MHD Ticket using the MHD API")
  (update-ticket  [ticket] "Update a MHD Ticket using the MHD API")
  (destroy-ticket [ticket] "Destroy a MHD Ticket using the MHD API")
  (close-ticket   [ticket] "Close a MHD Ticket using the MHD API"))
