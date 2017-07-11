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

-- :name get-all-certs :? :*
-- :doc retrieve the full table of certifications
select * from certifications

-- :name delete-cert! :! :n
-- :doc delete a user given the cert_no
DELETE FROM certifications
WHERE cert_no = :cert_no
