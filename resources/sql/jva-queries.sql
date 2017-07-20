-- :name create-jva! :! :n
-- :doc creates a new jva record
INSERT INTO jvas
(id, announce_no, position, status, open_date, close_date, salary, location, file_link)
VALUES (:id, :announce_no, :position, :status, :open_date, :close_date, :salary, :location, :file_link)

-- :name get-all-jvas :? :*
-- :doc retrieve all jvas
select announce_no, position, status, open_date, close_date, salary, location, file_link from jvas
where true

-- :name update-jva! :! :1
-- :doc edit a single jva
update jvas
set position = :position,
    status = :status,
    open_date = :open_date,
    close_date = :close_date,
    salary = :salary,
    location = :location,
    file_link = :file_link
where announce_no = :announce_no
