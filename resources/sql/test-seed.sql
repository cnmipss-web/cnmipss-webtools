-- :name clear-all-tables! :! :*
-- :doc create tables in database
delete from certifications where true;
delete from jvas where true;
delete from users where true;
delete from rfps where true;
delete from ifbs where true;

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

-- :name seed-rfps :! :n
-- :doc seed the database with dummy rfps
insert into rfps (id, rfp_no, open_date, close_date, title, description, file_link)
values ('1174a9a8-b45a-422a-bb46-574f814c2550', '15-010', '2015-01-20', '2015-06-06 12:00:00.0', 'Test Request for Proposal #1', 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent congue non magna eu rutrum. Maecenas ligula mi, interdum nec massa eget, consectetur semper lacus. Quisque viverra ac enim sit amet euismod. Nulla blandit placerat nisl. Sed sed risus sit amet metus scelerisque ultricies. Mauris dignissim lacus sit amet leo dictum pharetra. Quisque varius tellus nisi, non malesuada felis scelerisque sed. Nam aliquam eleifend turpis. Cras tristique pretium viverra. Nullam maximus, sapien vel laoreet porta, tellus felis ullamcorper eros, in tristique enim eros non eros. Aenean rutrum elementum massa a laoreet.

Vestibulum a ullamcorper odio, at eleifend tellus. Aliquam ut enim arcu. Donec consequat ex eu lectus dictum imperdiet vel ut quam. Cras quis euismod neque, sit amet finibus arcu. Mauris placerat urna augue, eget vestibulum enim cursus et. Donec dapibus risus at augue sagittis sagittis. Donec faucibus, tortor sit amet sagittis iaculis, sem quam fringilla nulla, in lobortis elit arcu sit amet arcu. Aenean et purus in tortor rhoncus pretium. Donec mollis neque mauris. Vestibulum ac erat mi. Quisque ac gravida nibh. Curabitur facilisis velit nec faucibus consectetur. Donec molestie egestas velit, vel eleifend metus rhoncus id. Maecenas est diam, volutpat non urna nec, luctus blandit neque. Donec ut tincidunt nisl.', 'http://dummy-link/file');

insert into rfps (id, rfp_no, open_date, close_date, title, description, file_link)
values ('d0002906-6432-42b5-b82b-35f0d710f827', '16-099', '2016-11-20', '2016-02-14 09:00:00.0', 'Test Request for Proposal #2', 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent congue non magna eu rutrum. Maecenas ligula mi, interdum nec massa eget, consectetur semper lacus. Quisque viverra ac enim sit amet euismod. Nulla blandit placerat nisl. Sed sed risus sit amet metus scelerisque ultricies. Mauris dignissim lacus sit amet leo dictum pharetra. Quisque varius tellus nisi, non malesuada felis scelerisque sed. Nam aliquam eleifend turpis. Cras tristique pretium viverra. Nullam maximus, sapien vel laoreet porta, tellus felis ullamcorper eros, in tristique enim eros non eros. Aenean rutrum elementum massa a laoreet.

Vestibulum a ullamcorper odio, at eleifend tellus. Aliquam ut enim arcu. Donec consequat ex eu lectus dictum imperdiet vel ut quam. Cras quis euismod neque, sit amet finibus arcu. Mauris placerat urna augue, eget vestibulum enim cursus et. Donec dapibus risus at augue sagittis sagittis. Donec faucibus, tortor sit amet sagittis iaculis, sem quam fringilla nulla, in lobortis elit arcu sit amet arcu. Aenean et purus in tortor rhoncus pretium. Donec mollis neque mauris. Vestibulum ac erat mi. Quisque ac gravida nibh. Curabitur facilisis velit nec faucibus consectetur. Donec molestie egestas velit, vel eleifend metus rhoncus id. Maecenas est diam, volutpat non urna nec, luctus blandit neque. Donec ut tincidunt nisl.', 'http://dummy-link/file');

insert into rfps (id, rfp_no, open_date, close_date, title, description, file_link)
values ('d2b4e97c-5d7c-4ccd-8fae-a27a27c863e3', '17-015', '2017-03-12', '2017-12-15 16:30:00.0', 'Test Request for Proposal #3', 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent congue non magna eu rutrum. Maecenas ligula mi, interdum nec massa eget, consectetur semper lacus. Quisque viverra ac enim sit amet euismod. Nulla blandit placerat nisl. Sed sed risus sit amet metus scelerisque ultricies. Mauris dignissim lacus sit amet leo dictum pharetra. Quisque varius tellus nisi, non malesuada felis scelerisque sed. Nam aliquam eleifend turpis. Cras tristique pretium viverra. Nullam maximus, sapien vel laoreet porta, tellus felis ullamcorper eros, in tristique enim eros non eros. Aenean rutrum elementum massa a laoreet.

Vestibulum a ullamcorper odio, at eleifend tellus. Aliquam ut enim arcu. Donec consequat ex eu lectus dictum imperdiet vel ut quam. Cras quis euismod neque, sit amet finibus arcu. Mauris placerat urna augue, eget vestibulum enim cursus et. Donec dapibus risus at augue sagittis sagittis. Donec faucibus, tortor sit amet sagittis iaculis, sem quam fringilla nulla, in lobortis elit arcu sit amet arcu. Aenean et purus in tortor rhoncus pretium. Donec mollis neque mauris. Vestibulum ac erat mi. Quisque ac gravida nibh. Curabitur facilisis velit nec faucibus consectetur. Donec molestie egestas velit, vel eleifend metus rhoncus id. Maecenas est diam, volutpat non urna nec, luctus blandit neque. Donec ut tincidunt nisl.', 'http://dummy-link/file');

-- :name clear-rfps :! :n
-- :doc clear the db of all rfp records
delete from rfps where true;

-- :name seed-ifbs :! :n
-- :doc seed the database with dummy ifbs
insert into ifbs (id, ifb_no, open_date, close_date, title, description, file_link)
values ('cf82deed-c84f-446c-a3f0-0d826428ddbd', '15-007', '2015-01-11', '2015-03-06 09:30:00.0', 'Test Invitation for Bid #1', 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent congue non magna eu rutrum. Maecenas ligula mi, interdum nec massa eget, consectetur semper lacus. Quisque viverra ac enim sit amet euismod. Nulla blandit placerat nisl. Sed sed risus sit amet metus scelerisque ultricies. Mauris dignissim lacus sit amet leo dictum pharetra. Quisque varius tellus nisi, non malesuada felis scelerisque sed. Nam aliquam eleifend turpis. Cras tristique pretium viverra. Nullam maximus, sapien vel laoreet porta, tellus felis ullamcorper eros, in tristique enim eros non eros. Aenean rutrum elementum massa a laoreet.

Vestibulum a ullamcorper odio, at eleifend tellus. Aliquam ut enim arcu. Donec consequat ex eu lectus dictum imperdiet vel ut quam. Cras quis euismod neque, sit amet finibus arcu. Mauris placerat urna augue, eget vestibulum enim cursus et. Donec dapibus risus at augue sagittis sagittis. Donec faucibus, tortor sit amet sagittis iaculis, sem quam fringilla nulla, in lobortis elit arcu sit amet arcu. Aenean et purus in tortor rhoncus pretium. Donec mollis neque mauris. Vestibulum ac erat mi. Quisque ac gravida nibh. Curabitur facilisis velit nec faucibus consectetur. Donec molestie egestas velit, vel eleifend metus rhoncus id. Maecenas est diam, volutpat non urna nec, luctus blandit neque. Donec ut tincidunt nisl.', 'http://dummy-link/file');

insert into ifbs (id, ifb_no, open_date, close_date, title, description, file_link)
values ('2fa4e278-f022-4361-b69a-0063a387933a', '16-229', '2016-12-31', '2016-05-05 08:00:00.0', 'Test Invitation for Bid #2', 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent congue non magna eu rutrum. Maecenas ligula mi, interdum nec massa eget, consectetur semper lacus. Quisque viverra ac enim sit amet euismod. Nulla blandit placerat nisl. Sed sed risus sit amet metus scelerisque ultricies. Mauris dignissim lacus sit amet leo dictum pharetra. Quisque varius tellus nisi, non malesuada felis scelerisque sed. Nam aliquam eleifend turpis. Cras tristique pretium viverra. Nullam maximus, sapien vel laoreet porta, tellus felis ullamcorper eros, in tristique enim eros non eros. Aenean rutrum elementum massa a laoreet.

Vestibulum a ullamcorper odio, at eleifend tellus. Aliquam ut enim arcu. Donec consequat ex eu lectus dictum imperdiet vel ut quam. Cras quis euismod neque, sit amet finibus arcu. Mauris placerat urna augue, eget vestibulum enim cursus et. Donec dapibus risus at augue sagittis sagittis. Donec faucibus, tortor sit amet sagittis iaculis, sem quam fringilla nulla, in lobortis elit arcu sit amet arcu. Aenean et purus in tortor rhoncus pretium. Donec mollis neque mauris. Vestibulum ac erat mi. Quisque ac gravida nibh. Curabitur facilisis velit nec faucibus consectetur. Donec molestie egestas velit, vel eleifend metus rhoncus id. Maecenas est diam, volutpat non urna nec, luctus blandit neque. Donec ut tincidunt nisl.', 'http://dummy-link/file');

insert into ifbs (id, ifb_no, open_date, close_date, title, description, file_link)
values ('5c052995-12c5-4fcc-b57e-bcbf7323f174', '17-105', '2017-07-12', '2017-12-15 10:00:00.0', 'Test Invitation for Bid #3', 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent congue non magna eu rutrum. Maecenas ligula mi, interdum nec massa eget, consectetur semper lacus. Quisque viverra ac enim sit amet euismod. Nulla blandit placerat nisl. Sed sed risus sit amet metus scelerisque ultricies. Mauris dignissim lacus sit amet leo dictum pharetra. Quisque varius tellus nisi, non malesuada felis scelerisque sed. Nam aliquam eleifend turpis. Cras tristique pretium viverra. Nullam maximus, sapien vel laoreet porta, tellus felis ullamcorper eros, in tristique enim eros non eros. Aenean rutrum elementum massa a laoreet.

Vestibulum a ullamcorper odio, at eleifend tellus. Aliquam ut enim arcu. Donec consequat ex eu lectus dictum imperdiet vel ut quam. Cras quis euismod neque, sit amet finibus arcu. Mauris placerat urna augue, eget vestibulum enim cursus et. Donec dapibus risus at augue sagittis sagittis. Donec faucibus, tortor sit amet sagittis iaculis, sem quam fringilla nulla, in lobortis elit arcu sit amet arcu. Aenean et purus in tortor rhoncus pretium. Donec mollis neque mauris. Vestibulum ac erat mi. Quisque ac gravida nibh. Curabitur facilisis velit nec faucibus consectetur. Donec molestie egestas velit, vel eleifend metus rhoncus id. Maecenas est diam, volutpat non urna nec, luctus blandit neque. Donec ut tincidunt nisl.', 'http://dummy-link/file');

-- :name clear-ifbs :! :n
-- :doc clear the db of all ifb records
delete from ifbs where true;

