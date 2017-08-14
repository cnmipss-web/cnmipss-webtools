-- :name create-ifb! :! :n
-- :doc creates a new record of a Request for Proposal
insert into ifbs
(id, ifb_no, open_date, close_date, title, description, file_link)
values (:id, :ifb_no, :open_date, :close_date, :title, :description, :file_link)

-- :name delete-ifb! :! :n
-- :doc deletes an existing record of a Request for Proposal
delete from ifbs
where ifb_no = :ifb_no

-- :name update-ifb :! :1
-- :doc updates an existing record of a Request for Proposal
update ifbs
set open_date = :open_date,
    close_date = :close_date,
    title = :title,
    description = :description,
    file_link = :file_link
where ifb_no = :ifb_no

-- :name get-all-ifbs :? :*
-- :doc return all records of Request for Proposals
select id, ifb_no, open_date, close_date, title, description, file_link  from ifbs
order by close_date desc

-- :name get-ifb :? :1
-- :doc returns a single record of a Request for Proposal
select id, ifb_no, open_date, close_date, title, description, file_link  from ifbs
where id = :id

-- :name get-ifb-by-no :? :1
-- :doc returns a single record of a Request for Proposal
select id, ifb_no, open_date, close_date, title, description, file_link  from ifbs
where ifb_no = :ifb_no

-- :name get-open-ifbs :? :n
-- :doc returns records of open Requests for Proposal
select id, ifb_no, open_date, close_date, title, description, file_link from ifbs
where (select date_part('day', current_date - close_date)) < 2
order by close_date desc
