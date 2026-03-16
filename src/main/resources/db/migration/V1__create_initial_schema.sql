create table if not exists users (
    id uuid not null,
    username varchar(255) not null,
    email varchar(255) not null,
    password varchar(255) not null,
    role varchar(50) not null,
    active boolean not null default true,
    primary key (id),
    constraint uk_users_username unique (username),
    constraint uk_users_email unique (email)
);

create table if not exists institution_profile (
    id uuid not null,
    nama_instansi varchar(255) not null,
    kota_kabupaten varchar(255) not null,
    alamat_lengkap varchar(255) not null,
    nama_ketua varchar(255),
    nama_bendahara varchar(255),
    primary key (id)
);

create table if not exists zakat_quality (
    id uuid not null,
    name varchar(255) not null,
    zakat_type varchar(50) not null,
    active boolean not null default true,
    berat_per_jiwa_kg numeric(19, 2),
    nominal_per_jiwa bigint,
    primary key (id)
);

create table if not exists receipt_sequence (
    receipt_year integer not null,
    version bigint,
    last_issued bigint not null,
    primary key (receipt_year)
);

create table if not exists zakat_payment (
    id uuid not null,
    jumlah_jiwa integer,
    alamat text,
    payer_name varchar(255),
    payer_phone varchar(255),
    payment_method varchar(50),
    berat_beras_kg numeric(19, 2),
    jumlah_uang numeric(19, 2),
    jumlah_uang_zakat_mal numeric(19, 2),
    jumlah_uang_infaq_sedekah numeric(19, 2),
    jumlah_uang_fidiah numeric(19, 2),
    payment_at timestamp(6) with time zone not null,
    canceled boolean not null default false,
    canceled_at timestamp(6) with time zone,
    cancel_reason text,
    canceled_by varchar(255),
    receipt_number varchar(255),
    receipt_year integer,
    receipt_sequence bigint,
    zakat_quality_id uuid,
    created_at timestamp(6) with time zone not null,
    created_by varchar(255),
    updated_at timestamp(6) with time zone not null,
    updated_by varchar(255),
    primary key (id),
    constraint uk_zakat_payment_receipt_number unique (receipt_number),
    constraint fk_zakat_payment_zakat_quality foreign key (zakat_quality_id) references zakat_quality (id)
);

create index if not exists idx_zakat_payment_payment_at on zakat_payment (payment_at);
create index if not exists idx_zakat_payment_zakat_quality_id on zakat_payment (zakat_quality_id);

create table if not exists muzakki_person (
    id uuid not null,
    nama text,
    payment_id uuid,
    sequence_no integer,
    primary key (id),
    constraint uk_muzakki_payment_sequence unique (payment_id, sequence_no),
    constraint fk_muzakki_person_payment foreign key (payment_id) references zakat_payment (id) on delete cascade
);

create index if not exists idx_muzakki_person_payment_id on muzakki_person (payment_id);
