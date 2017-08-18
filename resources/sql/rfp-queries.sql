-- :name create-rfp! :! :n
-- :doc creates a new record of a Request for Proposal
insert into rfps
(id, rfp_no, open_date, close_date, title, description, file_link)
values (:id, :rfp_no, :open_date, :close_date, :title, :description, :file_link)

-- :name delete-rfp! :! :n
-- :doc deletes an existing record of a Request for Proposal
delete from rfps
where id = :id

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
select id, rfp_no, open_date, close_date, title, description, file_link  from rfps
where true
order by close_date desc

-- :name get-rfp :? :1
-- :doc returns a single record of a Request for Proposal
select id, rfp_no, open_date, close_date, title, description, file_link  from rfps
where id = :id

-- :name get-rfp-by-no :? :1
-- :doc returns a single record of a Request for Proposal
select id, rfp_no, open_date, close_date, title, description, file_link  from rfps
where rfp_no = :rfp_no

-- :name get-open-rfps :? :*
-- :doc returns records of open Requests for Proposal
select id, rfp_no, open_date, close_date, title, description, file_link from rfps
where (select date_part('day', current_date - close_date)) < 2
order by close_date desc
