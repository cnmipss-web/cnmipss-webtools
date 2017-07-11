-- :name create-user! :! :n
-- :doc creates a new user record
INSERT INTO users
(id, email, admin, token)
VALUES (:id, :email, :admin, :token)

-- :name set-user-token! :! :1
-- :doc set a user's auth token from google
update users
set token = :token
where email = :email

-- :name get-user-info :? :1
-- :doc retrieve a user given the email.
SELECT * FROM users
WHERE email = :email

-- :name get-user-token :? :1
-- :doc retrieve a user's auth token given the email
select token from users
where email = :email

-- :name is-user-admin? :? :1
-- :doc return user admin status
select "admin" from users
where email = :email

-- :name delete-user! :! :n
-- :doc delete a user given the id
DELETE FROM users
WHERE email = :email
