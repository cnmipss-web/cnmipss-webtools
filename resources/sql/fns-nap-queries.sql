-- :name create-fns-nap! :! :n
-- :doc creates a new record of matched fns-nap records
INSERT INTO fns_nap
(id, date_created, fns_file_link, nap_file_link, matched_file_link)
VALUES (:id, CURRENT_TIMESTAMP, :fns_file_link, :nap_file_link, :matched_file_link)

-- :name delete-fns-nap! :! :n
-- :doc delets a record of matched fns-nap records
DELETE FROM fns_nap
WHERE fns_file_link=:fns_file_link
AND nap_file_link=:nap_file_link
AND matched_file_link=:matched_file_link

-- :name get-oldest-fns-nap :? :1
-- :doc retrieve the oldest record from fns_nap
SELECT * FROM fns_nap
WHERE date_created=(SELECT MIN(date_created) FROM fns_nap)

-- :name drop-oldest-fns-nap! :! :n
-- :doc deletes the oldest record from fns_nap
DELETE FROM fns_nap
WHERE date_created=( SELECT MIN(date_created) FROM fns_nap )


-- :name get-all-fns-nap :? :*
-- :doc retrieve the list of stored fns-nap registrations
SELECT * FROM fns_nap
ORDER BY date_created DESC
