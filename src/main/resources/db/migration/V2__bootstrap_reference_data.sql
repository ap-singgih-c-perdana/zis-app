alter table if exists public.zakat_payment
    add column if not exists created_at timestamp(6) with time zone;

alter table if exists public.zakat_payment
    add column if not exists created_by varchar(255);

alter table if exists public.zakat_payment
    add column if not exists updated_at timestamp(6) with time zone;

alter table if exists public.zakat_payment
    add column if not exists updated_by varchar(255);

update public.zakat_payment
set created_at = coalesce(created_at, payment_at, now()),
    created_by = coalesce(created_by, 'system'),
    updated_at = coalesce(updated_at, created_at, payment_at, now()),
    updated_by = coalesce(updated_by, created_by, 'system')
where created_at is null
   or created_by is null
   or updated_at is null
   or updated_by is null;

alter table if exists public.zakat_payment
    alter column created_at set not null;

alter table if exists public.zakat_payment
    alter column updated_at set not null;

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
where exists (select 1 from information_schema.tables where table_schema = 'public' and table_name = 'institution_profile')
  and not exists (select 1 from institution_profile);

insert into zakat_quality (id, name, zakat_type, berat_per_jiwa_kg, nominal_per_jiwa, active)
select v.id, v.name, v.zakat_type, v.berat_per_jiwa_kg, v.nominal_per_jiwa, v.active
from (
    values
        ('48f28cc1-2247-4ea8-8187-8028f6ee91ad'::uuid, 'Standar 2.5 Kg', 'ZAKAT_FITRAH_BERAS', 2.5::numeric, null::bigint, true),
        ('f481fa0d-2416-4fee-b4f7-589ff1b664b6'::uuid, 'Standar 3.0 Kg', 'ZAKAT_FITRAH_BERAS', 3.0::numeric, null::bigint, true),
        ('8d3dd908-a6a2-400c-bdf5-820cd0fa3815'::uuid, 'SK Bupati (Standar)', 'ZAKAT_FITRAH_UANG', null::numeric, 45000::bigint, true),
        ('daace2c4-2a21-4901-8cc6-505f19379f87'::uuid, 'Beras Premium', 'ZAKAT_FITRAH_UANG', null::numeric, 55000::bigint, true)
) as v(id, name, zakat_type, berat_per_jiwa_kg, nominal_per_jiwa, active)
where exists (select 1 from information_schema.tables where table_schema = 'public' and table_name = 'zakat_quality')
  and not exists (select 1 from zakat_quality);
