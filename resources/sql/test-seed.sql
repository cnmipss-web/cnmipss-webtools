-- :name create-tables :! :*
-- :doc create tables in database
drop table certifications;
CREATE TABLE certifications (
    cert_no text NOT NULL primary key,
    last_name text,
    first_name text,
    mi text,
    cert_type text,
    start_date text,
    expiry_date text
);
drop table jvas;
CREATE TABLE jvas (
    id uuid NOT NULL primary key,
    announce_no character varying(20) NOT NULL unique,
    "position" character varying(140) NOT NULL,
    status boolean DEFAULT true,
    open_date date NOT NULL,
    close_date date,
    salary character varying(140) NOT NULL,
    location character varying(140) NOT NULL,
    file_link text DEFAULT ''::text
);
drop table users;
CREATE TABLE users (
    id uuid NOT NULL primary key,
    email character varying(140) unique,
    admin boolean DEFAULT false NOT NULL,
    roles character varying(140) DEFAULT ''::character varying,
    token text DEFAULT ''::text
);

-- :name seed-users :! :n
-- :doc create user data in database
insert into users (id, email, admin, roles, token)
values ('9809afc1-6b00-4946-b50f-9eaa05a7ce08','tyler.collins@cnmipss.org',true,null,'ya29.GluTBHe_gy2R2PBdSedi3oZKT64AltZN7EfIQKReuLOWcdMjySQnh5VeSCLC8-_aG1wdhaBrT4baVSvWnrDoiK5z3_nJkdKpfAhiXI1c2cenTSJyd8sx-dpqBm0B'); 

insert into users (id, email, admin, roles, token)
values ('0009afc1-6b00-4946-b50f-9eaa05a7ce08','john.doe@cnmipss.org',false,'Certification',null);

insert into users (id, email, admin, roles, token)
values ('0109afc1-6b00-4946-b50f-9eaa05a7ce08','inigo.montoya@cnmipss.org',false,'HRO',null);

insert into users (id, email, admin, roles, token)
values ('0209afc1-6b00-4946-b50f-9eaa05a7ce08','tony.stark@cnmipss.org',false,'Procurement',null);

insert into users (id, email, admin, roles, token)
values ('0309afc1-6b00-4946-b50f-9eaa05a7ce08','bono.u2@cnmipss.org',false,'Manage Users,Manage DB',null);


-- :name clear-users :! :n
-- :doc clear users from the database
delete from users where true;

-- :name seed-certs :! :n
-- :doc create certification data in database
insert into certifications (cert_no, last_name, first_name, mi, cert_type, start_date, expiry_date)
values ('BI-003-2006','Jones','Victor','D.','Basic I','July 18, 2006','July 17, 2008');

insert into certifications (cert_no, last_name, first_name, mi, cert_type, start_date, expiry_date)
values ('BI-005-2006','Shryack','Angela','N.','Basic I','July 24, 2006','July 23, 2008');

insert into certifications (cert_no, last_name, first_name, mi, cert_type, start_date, expiry_date)
values ('BI-004-2006','Angel','Amanda','Lynn','Basic I','July 26, 2006','July 25, 2008');

insert into certifications (cert_no, last_name, first_name, mi, cert_type, start_date, expiry_date)
values ('BI-007-2006','Pagapular','Kathy','Lalaine','Basic I','July 26, 2006','July 25, 2008');

-- :name clear-certs :! :n
-- :doc clear certifications from the database
delete from certifications where true;

-- :name seed-jvas :! :n
-- :doc create jva data in database
insert into jvas (id, announce_no, "position", status, open_date, close_date, salary, location, file_link)
values ('8d893df0-1afc-4dd6-8e20-eb74a6e4e50b','PSS-2015-311','Early Head Start Family Partnership Advocate (Re-',true,'2017-07-14',null,'PAY LEVEL: 20 STEP(S): 02-12; $15,105.87 - $24,584.23 Per Annum','Early Head Start/Head Start Program, Saipan','http://localhost.test/wp-content/uploads/2017/07/PSS-2015-311-Family-Partnership-Advocate_EHS_HDST_ReAnnouncement.pdf');

insert into jvas (id, announce_no, "position", status, open_date, close_date, salary, location, file_link)
values ('865447c8-c58a-4763-ab83-8cda69cf7334','PSS-2016-219','School Attendance Review Committee Coordinator',false,'2016-12-22','2017-01-05','Pay Level Ungraded STEPS(S) Ungraded; $45,000.00 - $50,000.00 PER ANNUM','OFFICE OF THE COMMISSIONER OF EDUCATION','http://localhost.test/wp-content/uploads/2017/07/PSS-2016-219_School-Attendance-Review-Committee-Coordinator_SARC_BOE.pdf');

insert into jvas (id, announce_no, "position", status, open_date, close_date, salary, location, file_link)
values ('ce65a6c0-d6d3-4eba-bbaf-a939073eca1d','PSS-2017-071','Head Start Teacher Aide (Re-Announcement)',true,'2017-07-17',null,'PAY LEVEL/GRADE: I STEP(S): 08; $15,237.64 Per Annum','Head Start/Early Head Start Program','http://localhost.test/wp-content/uploads/2017/07/PSS-2017-071-Teacher-Aide_Head-Start-Program_ReAnnouncement.pdf');

-- :name clear-jvas :! :n
-- :doc clear jvas from the database
delete from jvas where true;
