insert into institution_profile (
    id, nama_instansi, kota_kabupaten, alamat_lengkap, nama_ketua, nama_bendahara
) values (
             '00000000-0000-0000-0000-000000000001',
             'Masjid Al Adil',
             'Jakarta',
             'Jl. Adhyaksa',
             'H. Nur Pujianto, S.Kom',
             'Ust. Abu Hanifah'
         );

-- ZAKAT FITRAH (BERAS)
insert into zakat_quality (id, name, zakat_type, berat_per_jiwa_kg, nominal_per_jiwa, active)
values
    ('48f28cc1-2247-4ea8-8187-8028f6ee91ad', 'Standar 2.5 Kg', 'ZAKAT_FITRAH_BERAS', 2.5, null, true),
    ('f481fa0d-2416-4fee-b4f7-589ff1b664b6', 'Standar 3.0 Kg', 'ZAKAT_FITRAH_BERAS', 3.0, null, true);

-- ZAKAT FITRAH (UANG)
insert into zakat_quality (id, name, zakat_type, berat_per_jiwa_kg, nominal_per_jiwa, active)
values
    ('8d3dd908-a6a2-400c-bdf5-820cd0fa3815', 'SK Bupati (Standar)', 'ZAKAT_FITRAH_UANG', null, 45000, true),
    ('daace2c4-2a21-4901-8cc6-505f19379f87', 'Beras Premium',       'ZAKAT_FITRAH_UANG', null, 55000, true);

alter table zakat_payment
    add column if not exists canceled boolean;

update zakat_payment set canceled = false where canceled is null;

alter table zakat_payment
    alter column canceled set not null;

alter table zakat_payment
    alter column canceled set default false;

ALTER TABLE zakat_payment RENAME COLUMN created_at TO payment_at;