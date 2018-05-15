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

-- :name get-subscription :? :1
-- :doc Retrieve a single subscription by id
select procurement_rfps_and_ifbs.title,
       procurement_rfps_and_ifbs.number,
       procurement_rfps_and_ifbs.type,
       procurement_rfps_and_ifbs.description,
       procurement_subscriptions.contact_person,
       procurement_subscriptions.email,
       procurement_subscriptions.active  
from procurement_subscriptions
inner join
procurement_rfps_and_ifbs
on procurement_rfps_and_ifbs.id = procurement_subscriptions.proc_id
where procurement_subscriptions.id = :id;

-- :name get-users-subscription :? :*
select * from procurement_subscriptions
where proc_id = :proc_id
      and email = :email;

-- :name get-all-subscriptions :? :*
-- :doc Retrieve all subscriptions
select * from procurement_subscriptions;

-- :name deactivate-subscription :! :1
-- :doc deactivate a subscription to updates regarding a procurement announcement
update procurement_subscriptions
set active = false
where id = :id;

-- :name reactivate-subscription :! :1
--:doc reactivate a subscription to updates regarding a procurement announcements
update procurement_subscriptions
set active = true
where id = :id;
