create table procurement_rfps_and_ifbs (
       id UUID primary key,
       type varchar(3),
       number varchar(8),
       open_date date not null,
       close_date timestamp not null,
       title varchar(140) default '',
       description text default '',
       file_link text default '',
       unique (id, type)
);

insert into procurement_rfps_and_ifbs
(id, type, number, open_date, close_date, title, description, file_link)
select id, 'rfp' as type, rfp_no, open_date, close_date, title, description, file_link  from rfps
where true;

insert into procurement_rfps_and_ifbs
(id, type, number, open_date, close_date, title, description, file_link)
select id, 'ifb' as type, ifb_no, open_date, close_date, title, description, file_link  from ifbs
where true;

alter table procurement_addenda
add column proc_id UUID references procurement_rfps_and_ifbs;

update procurement_addenda
set proc_id = procurement_addenda.rfp_id
where rfp_id is not null;

update procurement_addenda
set proc_id = procurement_addenda.ifb_id
where ifb_id is not null;

alter table procurement_addenda
drop column rfp_id;

alter table procurement_addenda
drop column ifb_id;

alter table procurement_subscriptions
add column proc_id UUID references procurement_rfps_and_ifbs;

alter table procurement_subscriptions
add unique(email, proc_id);

update procurement_subscriptions
set proc_id = rfp_id
where rfp_id is not null;

update procurement_subscriptions
set proc_id = ifb_id
where ifb_id is not null;

alter table procurement_subscriptions
drop column rfp_id;

alter table procurement_subscriptions
drop column ifb_id;

drop table rfps;
drop table ifbs;
