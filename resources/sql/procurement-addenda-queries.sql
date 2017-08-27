-- :name create-addendum! :! :1
-- :doc Creates a new addendum to an existing rfp/ifb
insert into procurement_addenda
(id, file_link, proc_id, addendum_number)
values (:id, :file_link, :proc_id, :number);

-- :name delete-addendum! :! :1
-- :doc Deletes an addendum from the database
delete from procurement_addenda
where id = :id;

-- :name get-addenda :? :*
-- :doc Retrieve addenda with matching :proc_id
select * from procurement_addenda
where proc_id = :proc_id;

-- :name get-all-addenda :? :*
-- :doc Retrieve all addenda
select * from procurement_addenda;
