CREATE TABLE fns_nap
       (id UUID PRIMARY KEY,
       date_created timestamp not null,
       fns_file_link text not null,
       nap_file_link text not null,
       matched_file_link text not null);
