-- :name create-subscription! :! :1
-- :doc Creates a new subscription to an existing rfp/ifb
insert into procurement_subscriptions
(id, proc_id, subscription_number, company_name, contact_person, email, telephone)
values (:id, :proc_id, :subscription_number, :company_name, :contact_person, :email, :telephone);

-- :name delete-subscription! :! :1
-- :doc Deletes a subscription from the database
delete from procurement_subscriptions
where id = :id;

-- :name get-subscriptions :? :*
-- :doc Retrieve subscription with matching :rfp_id or ifb_id
select * from procurement_subscriptions
where proc_id = :proc_id;

-- :name get-all-subscriptions :? :*
-- :doc Retrieve all subscriptions
select * from procurement_subscriptions;

