-- :name create-subscription! :! :1
-- :doc Creates a new subscription to an existing rfp/ifb
insert into procurement_subscriptions
(id, rfp_id, ifb_id, subscription_number, company_name, contact_person, email, telephone)
values (:id, :rfp_id, :ifb_id, :subscription_number, :company_name, :contact_person, :email, :telephone);

-- :name delete-subscription! :! :1
-- :doc Deletes a subscription from the database
delete from procurement_subscriptions
where id = :id;

-- :name get-subscriptions :? :*
-- :doc Retrieve subscription with matching :rfp_id or ifb_id
select * from procurement_subscriptions
where rfp_id = :rfp_id or ifb_id = :ifb_id;

-- :name get-all-subscriptions :? :*
-- :doc Retrieve all subscriptions
select * from procurement_subscriptions;

