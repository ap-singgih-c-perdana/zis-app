alter table if exists institution_profile
    add column if not exists nomor_telepon varchar(255);

alter table if exists institution_profile
    add column if not exists email varchar(255);
