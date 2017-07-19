-- :name create-jva! :! :n
-- :doc creates a new jva record
INSERT INTO jvas
(id, announce_no, position, status, open_date, close_date, salary, location, file_link)
VALUES (:id, :announce_no, :position, :status, :open_date, :close_date, :salary, :location, :file_link)

-- :name get-all-jvas :? :*
-- :doc retrieve all jvas
select announce_no, position, status, open_date, close_date, salary, location, file_link from jvas
where true
