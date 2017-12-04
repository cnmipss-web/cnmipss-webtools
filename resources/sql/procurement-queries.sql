-- :name create-pnsa! :! :n
-- :doc Creates a new record of a Procurement RFP or IFB Announcement
insert into procurement_rfps_and_ifbs
(id, type, number, open_date, close_date, title, description, file_link, spec_link)
values (:id, :type, :number, :open_date, :close_date, :title, :description, :file_link, :spec_link);

-- :name delete-pnsa! :! :n
-- :doc Deletes a record of a Procurement RFP or IFB Announcement from the DB
delete from procurement_rfps_and_ifbs
where id = :id;

-- :name update-pnsa! :! :1
-- :doc Updates an existing record of a Procurement RFP or IFB Announcement in the DB
update procurement_rfps_and_ifbs
set number = :number,
    open_date = :open_date,
    close_date = :close_date,
    title = :title,
    description = :description,
    file_link = :file_link
where id = :id;

-- :name get-all-pnsa :? :*
-- :doc Return all records of RFPs and IFBs
select * from procurement_rfps_and_ifbs
where true
order by close_date desc;

-- :name get-open-pnsa :? :*
-- :doc Return all records of RFPs and IFBs that have not closed
select * from procurement_rfps_and_ifbs
where 
      select current_date < close_date
order by close_date desc;

-- :name get-single-pnsa :? :1
-- :doc Returns a single RFP/IFB by id
select * from procurement_rfps_and_ifbs
where id = :id;

-- :name get-pnsa-by-no :? :1
-- :doc Returns a single RFP/IFB by number
select * from procurement_rfps_and_ifbs
where number = :number;
