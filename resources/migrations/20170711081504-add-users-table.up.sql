CREATE TABLE users
       (id UUID PRIMARY KEY,
        email varchar(140) unique,
        admin BOOLEAN not null default false,
        roles varchar(140) default '',
        token text default '');
