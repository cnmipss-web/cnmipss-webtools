-- :name create-cert! :! :n
-- :doc creates a new user record
INSERT INTO certifications
(cert_no, last_name, first_name, mi, cert_type, start_date, expiry_date)
VALUES (:cert_no, :last_name, :first_name, :mi, :cert_type, :start_date, :expiry_date)

-- :name update-cert! :! :n
-- :doc update an existing user record
UPDATE certifications
SET last_name = :last_name,
    first_name = :first_name,
    mi = :mi,
    cert_type = :cert_type,
    start_date = :start_date,
    expiry_date = :expiry_date
WHERE cert_no = :cert_no

-- :name get-cert :? :1
-- :doc retrieve a user given the cert_no
SELECT * FROM certifications
WHERE cert_no = :cert_no
order by cert_no

-- :name get-all-certs :? :*
-- :doc retrieve the full table of certifications
select * from certifications

-- :name delete-cert! :! :n
-- :doc delete a user given the cert_no
DELETE FROM certifications
WHERE cert_no = :cert_no

-- :name save-collision! :! :n
-- :doc stores a record of a certification collision
INSERT INTO cert_collisions
(id, cert_no, name1, start_date1, expiry_date1, cert_type1, name2, start_date2, expiry_date2, cert_type2)
VALUES (:id, :cert_no, :name1, :start_date1, :expiry_date1, :cert_type1, :name2, :start_date2, :expiry_date2, :cert_type2);

-- :name clear-collisions! :! :n
-- :doc clear the list of saved collisions
DELETE FROM cert_collisions
WHERE true;

-- :name get-collision-list :? :*
-- :doc return the most recent list of certification collisions
SELECT * FROM cert_collisions;
