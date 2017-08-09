create table procurement_addenda (
       id UUID primary key,     
       rfp_id UUID references rfps,
       ifb_id UUID references ifbs,
       file_link text not null
);
