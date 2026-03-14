insert into institution_profile (
    id, nama_instansi, kota_kabupaten, alamat_lengkap, nama_ketua, nama_bendahara
)
select
    '00000000-0000-0000-0000-000000000001'::uuid,
    'Masjid Al Adil',
    'Jakarta',
    'Jl. Adhyaksa',
    'H. Nur Pujianto, S.Kom',
    'Ust. Abu Hanifah'
where not exists (select 1 from institution_profile);

-- Seed default zakat_quality hanya jika tabel masih kosong
insert into zakat_quality (id, name, zakat_type, berat_per_jiwa_kg, nominal_per_jiwa, active)
select v.id, v.name, v.zakat_type, v.berat_per_jiwa_kg, v.nominal_per_jiwa, v.active
from (
    values
        ('48f28cc1-2247-4ea8-8187-8028f6ee91ad'::uuid, 'Standar 2.5 Kg', 'ZAKAT_FITRAH_BERAS', 2.5::numeric, null::bigint, true),
        ('f481fa0d-2416-4fee-b4f7-589ff1b664b6'::uuid, 'Standar 3.0 Kg', 'ZAKAT_FITRAH_BERAS', 3.0::numeric, null::bigint, true),
        ('8d3dd908-a6a2-400c-bdf5-820cd0fa3815'::uuid, 'SK Bupati (Standar)', 'ZAKAT_FITRAH_UANG', null::numeric, 45000::bigint, true),
        ('daace2c4-2a21-4901-8cc6-505f19379f87'::uuid, 'Beras Premium', 'ZAKAT_FITRAH_UANG', null::numeric, 55000::bigint, true)
) as v(id, name, zakat_type, berat_per_jiwa_kg, nominal_per_jiwa, active)
where not exists (select 1 from zakat_quality);

alter table zakat_payment
    add column if not exists canceled boolean;

update zakat_payment set canceled = false where canceled is null;

alter table zakat_payment
    alter column canceled set not null;

alter table zakat_payment
    alter column canceled set default false;