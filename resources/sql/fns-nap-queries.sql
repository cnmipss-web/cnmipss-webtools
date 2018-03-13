-- :name create-fns-nap! :! :n
-- :doc creates a new record of matched fns-nap records
INSERT INTO fns_nap
(date_created, fns_file_link, nap_file_link, matched_file_link)
VALUES ( SELECT current_date(), :fns_file_link, :nap_file_link, :matched_file_link)

-- :name drop-oldest-fns-nap! :! :n
-- :doc deletes the oldest record from fns_nap
DELETE FROM fns_nap 
WHERE data_created IS NOT NULL 
ORDER BY date_created DESC 
LIMIT 1

-- :name get-all-fns-nap :? :*
-- :doc retrieve the list of stored fns-nap registrations
SELECT * FROM fns_nap
