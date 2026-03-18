alter table zakat_payment
    add column if not exists received_by_name varchar(255);
