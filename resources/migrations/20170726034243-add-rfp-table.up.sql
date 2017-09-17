CREATE TABLE rfps (
       id UUID PRIMARY KEY,
       rfp_no varchar(140) unique,
       open_date date not null,
       close_date date not null,
       title varchar(140) default '',
       description text default '',
       file_link text default ''
);
