-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(id, email, admin, roles)
VALUES (:id, :email, :admin, :roles)

-- :name set-user-token! :! :1
-- :doc set a user's auth token from google
update users
set token = :token
where email = :email

-- :name get-user-info :? :1
-- :doc retrieve a user given the email.
SELECT * FROM users
WHERE email = :email
order by email

-- :name get-all-users :? :*
-- :doc retrieve all users
select email, roles, admin from users
where true

-- :name get-proc-users :? :*
-- :doc Retrieve users with roles containing 'Procurement'
select email from users
where roles like '%Procurement%';

-- :name get-user-token :? :1
-- :doc retrieve a user's auth token given the email
select token from users
where email = :email

-- :name is-user-admin? :? :1
-- :doc return user admin status
select admin from users
where email = :email

-- :name get-user-roles :? :1
-- :doc retrieve user's authorized roles
select roles from users
where email = :email

-- :name set-user-roles! :! :1
-- :doc set a user's authorized roles
update users
set roles = :roles
where email = :email

-- :name set-user-admin! :! :1
-- :doc set a user's admin status
update users
set admin = :admin
where email = :email

-- :name delete-user! :! :n
-- :doc delete a user given the id
DELETE FROM users
WHERE email = :email
