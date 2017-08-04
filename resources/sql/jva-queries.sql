-- :name create-jva! :! :n
-- :doc creates a new jva record
INSERT INTO jvas
(id, announce_no, position, status, open_date, close_date, salary, location, file_link)
VALUES (:id, :announce_no, :position, :status, :open_date, :close_date, :salary, :location, :file_link)

-- :name get-all-jvas :? :*
-- :doc retrieve all jvas
select announce_no, position, status, open_date, close_date, salary, location, file_link from jvas
order by status desc, announce_no desc

-- :name get-open-jvas :? :*
-- :doc retrieve all jvas
select announce_no, position, status, open_date, close_date, salary, location, file_link from jvas
where (select current_date) < close_date or close_date is null
order by status desc, announce_no desc

-- :name get-jva :? :1
-- :doc retrieve a single jva by its announce_no
select * from jvas
where announce_no = :announce_no

-- :name jva-id :? :1
-- :doc get jva-id from announce_no
select id from jvas
where announce_no = :announce_no

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

-- :name delete-jva! :! :1
-- :doc delete a single jva
delete from jvas
where announce_no = :announce_no
