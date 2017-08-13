create table procurement_subscriptions (
       id UUID primary key,
       rfp_id UUID references rfps,
       ifb_id UUID references ifbs,
       subscription_number int default 0,
       company_name varchar(140) not null,
       contact_person varchar(140) not null,
       email varchar(140) unique not null,
       telephone bigint,
       constraint subscribe_once unique (email, rfp_id, ifb_id)
);
