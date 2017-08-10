-- :name create-addendum! :! :1
-- :doc Creates a new addendum to an existing rfp/ifb
insert into procurement_addenda
(id, file_link, rfp_id, ifb_id, addendum_number)
values (:id, :file_link, :rfp_id, :ifb_id, :number);

-- :name delete-addendum! :! :1
-- :doc Deletes an addendum from the database
delete from procurement_addenda
where id = :id;

-- :name get-rfp-addenda :? :*
-- :doc Retrieve addenda with matching :rfp_no
select * from procurement_addenda
where rfp_id = :rfp_id;

-- :name get-ifb-addenda :? :*
-- :doc Retrieve addenda with matching ifb_no
select * from procurement_addenda
where ifb_id = :ifb_id;

-- :name get-all-addenda :? :*
-- :doc Retrieve all addenda
select * from procurement_addenda;
