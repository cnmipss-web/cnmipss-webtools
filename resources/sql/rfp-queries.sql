-- :name create-rfp! :! :n
-- :doc creates a new record of a Request for Proposal
insert into rfps
(id, rfp_no, open_date, close_date, title, description, file_link)
values (:id, :rfp_no, :open_date, :close_date, :title, :description, :file_link)

-- :name delete-rfp! :! :n
-- :doc deletes an existing record of a Request for Proposal
delete from rfps
where rfp_no = :rfp_no or id = :id

-- :name update-rfp :! :1
-- :doc updates an existing record of a Request for Proposal
update rfps
set open_date = :open_date,
    close_date = :close_date,
    title = :title,
    description = :description,
    file_link = :file_link
where rfp_no = :rfp_no

-- :name get-all-rfps :? :*
-- :doc return all records of Request for Proposals
select rfp_no, open_date, close_date, title, description, file_link  from rfps

-- :name get-rfp :? :1
-- :doc returns a single record of a Request for Proposal
select rfp_no, open_date, close_date, title, description, file_link  from rfps
where id = :id or rfp_no = :rfp_no

-- :name get-open-rfps :? :n
-- :doc returns records of open Requests for Proposal
select rfp_no, open_date, close_date, title, description, file_link from rfps
where (select current_date) < close_date

