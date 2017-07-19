create table jvas
       (id UUID primary key,
       announce_no varchar(16) not null,
       position varchar(140) not null,
       status boolean default true,
       open_date date not null,
       close_date date,
       salary varchar(140) not null,
       location varchar(140) not null,
       file_link text default '');
