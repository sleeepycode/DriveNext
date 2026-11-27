-- DriveNext Lab 2. Run in Supabase SQL Editor in the same project as Lab 1.
-- Intended for a fresh educational project. Does not drop existing user data.
begin;
create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    email text not null,
    first_name text not null default '',
    last_name text not null default '',
    middle_name text not null default '',
    birth_date date,
    gender text check (gender in ('male','female')),
    license_number text check (license_number is null or char_length(license_number) = 10),
    license_issued date,
    avatar_path text,
    license_path text,
    passport_path text,
    consent boolean not null default false,
    provider text not null default 'email' check (provider in ('email','google')),
    registration_complete boolean not null default false,
    created_at timestamptz not null default now(),
    constraint profile_avatar_owner check (avatar_path is null or avatar_path = id::text || '/avatar.jpg'),
    constraint profile_license_owner check (license_path is null or license_path = id::text || '/license.jpg'),
    constraint profile_passport_owner check (passport_path is null or passport_path = id::text || '/passport.jpg'),
    constraint profile_completed_fields check (
      not registration_complete or provider = 'google' or (
        length(trim(first_name)) > 0 and length(trim(last_name)) > 0 and birth_date is not null
        and gender is not null and license_number is not null and license_issued is not null
        and license_path is not null and passport_path is not null and consent
      )
    )
);
alter table public.profiles enable row level security;
revoke all on public.profiles from anon, authenticated;
grant usage on schema public to authenticated;
grant select on public.profiles to authenticated;
grant insert (id,email,first_name,last_name,middle_name,birth_date,gender,license_number,license_issued,avatar_path,license_path,passport_path,consent,registration_complete) on public.profiles to authenticated;
grant update (id,email,first_name,last_name,middle_name,birth_date,gender,license_number,license_issued,avatar_path,license_path,passport_path,consent,registration_complete) on public.profiles to authenticated;
drop policy if exists drivenext_profile_read on public.profiles;
create policy drivenext_profile_read on public.profiles for select to authenticated using ((select auth.uid()) = id);
drop policy if exists drivenext_profile_insert on public.profiles;
create policy drivenext_profile_insert on public.profiles for insert to authenticated with check ((select auth.uid()) = id and lower(email) = lower((select auth.jwt()->>'email')));
drop policy if exists drivenext_profile_update on public.profiles;
create policy drivenext_profile_update on public.profiles for update to authenticated using ((select auth.uid()) = id) with check ((select auth.uid()) = id and lower(email) = lower((select auth.jwt()->>'email')));

-- Google OAuth is an alternative login path specified in Lab 2. The server creates a minimal profile.
create or replace function public.drivenext_create_google_profile() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    if new.raw_app_meta_data->>'provider' = 'google' then
        insert into public.profiles(id,email,first_name,last_name,provider,registration_complete)
        values (new.id,coalesce(new.email,''),coalesce(new.raw_user_meta_data->>'given_name',''),coalesce(new.raw_user_meta_data->>'family_name',''),'google',true)
        on conflict (id) do nothing;
    end if;
    return new;
end;
$$;
revoke all on function public.drivenext_create_google_profile() from public, anon, authenticated;
drop trigger if exists drivenext_google_profile on auth.users;
create trigger drivenext_google_profile after insert on auth.users for each row execute function public.drivenext_create_google_profile();
insert into public.profiles(id,email,first_name,last_name,provider,registration_complete)
select id,coalesce(email,''),coalesce(raw_user_meta_data->>'given_name',''),coalesce(raw_user_meta_data->>'family_name',''),'google',true
from auth.users where raw_app_meta_data->>'provider'='google' on conflict (id) do nothing;

insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types)
values ('drivenext-documents','drivenext-documents',false,6291456,array['image/jpeg']::text[])
on conflict(id) do update set public=false,file_size_limit=excluded.file_size_limit,allowed_mime_types=excluded.allowed_mime_types;
-- The bucket is PRIVATE. Do not add a public SELECT policy or service_role to the APK.
drop policy if exists drivenext_docs_read on storage.objects;
create policy drivenext_docs_read on storage.objects for select to authenticated using (
  bucket_id='drivenext-documents' and (storage.foldername(name))[1]=(select auth.uid())::text
);
drop policy if exists drivenext_docs_insert on storage.objects;
create policy drivenext_docs_insert on storage.objects for insert to authenticated with check (
  bucket_id='drivenext-documents' and name in ((select auth.uid())::text || '/avatar.jpg',(select auth.uid())::text || '/license.jpg',(select auth.uid())::text || '/passport.jpg')
);
drop policy if exists drivenext_docs_update on storage.objects;
create policy drivenext_docs_update on storage.objects for update to authenticated using (
  bucket_id='drivenext-documents' and (storage.foldername(name))[1]=(select auth.uid())::text
) with check (
  bucket_id='drivenext-documents' and name in ((select auth.uid())::text || '/avatar.jpg',(select auth.uid())::text || '/license.jpg',(select auth.uid())::text || '/passport.jpg')
);
drop policy if exists drivenext_docs_delete on storage.objects;
create policy drivenext_docs_delete on storage.objects for delete to authenticated using (
  bucket_id='drivenext-documents' and (storage.foldername(name))[1]=(select auth.uid())::text
);
commit;
