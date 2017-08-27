create table rfps (
       id UUID PRIMARY KEY,
       rfp_no varchar(140) unique,
       open_date date not null,
       close_date timestamp not null,
       title varchar(140) default '',
       description text default '',
       file_link text default ''
);

create table ifbs (
       id UUID PRIMARY KEY,
       ifb_no varchar(140) unique,
       open_date date not null,
       close_date timestamp not null,
       title varchar(140) default '',
       description text default '',
       file_link text default ''
);


insert into rfps
(id, rfp_no, open_date, close_date, title, description, file_link)
select
(id, number, open_date, close_date, title, description, file_link)
from procurement_rfps_and_ifbs
where type='rfp';

insert into ifbs
(id, rfp_no, open_date, close_date, title, description, file_link)
select
(id, number, open_date, close_date, title, description, file_link)
from procurement_rfps_and_ifbs
where type='ifb';

drop table procurement_rfps_and_ifbs;
