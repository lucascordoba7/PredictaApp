-- PredictaApp — migración a cuentas por email (sin password)
-- Correr UNA vez en: Supabase → SQL Editor → Run. Es transaccional: si algo falla, no queda a medias.
--
-- Qué hace:
--   1. profiles pasa a tener una fila por cuenta, con email único.
--   2. Las 5 tablas de datos ganan "userId" y todo lo existente se asigna a la cuenta 'me' (Lucas).
--   3. Las PK pasan a ser compuestas (id, "userId").
--
-- Por qué el punto 3: los ids de categories/expenses/fixed_expenses los genera el AUTOINCREMENT
-- de Room en cada dispositivo, arrancando de 1. Con dos cuentas, ambas producen id=1 y la
-- segunda pisaría a la primera. La PK compuesta hace que id sea único POR usuario.
-- subscriptions/notifications usan id text, pero se lo aplicamos igual por uniformidad.

begin;

-- ─────────────────── 1. profiles: una fila por cuenta ───────────────────
-- El id deja de ser el literal 'me' conceptualmente: pasa a ser un identificador opaco
-- por cuenta. La fila existente conserva 'me' a propósito — así el backfill de abajo no
-- tiene que reescribir nada y tus 56 gastos no se tocan.
create unique index if not exists profiles_email_key on public.profiles (lower(email));

-- ─────────────────── 2. userId en las tablas de datos ───────────────────
-- El default 'me' backfillea todo lo que ya existe hacia tu cuenta.
alter table public.categories     add column if not exists "userId" text not null default 'me';
alter table public.expenses       add column if not exists "userId" text not null default 'me';
alter table public.subscriptions  add column if not exists "userId" text not null default 'me';
alter table public.fixed_expenses add column if not exists "userId" text not null default 'me';
alter table public.notifications  add column if not exists "userId" text not null default 'me';

-- Backfill explícito por si la columna ya existía sin default.
update public.categories     set "userId" = 'me' where "userId" is null or "userId" = '';
update public.expenses       set "userId" = 'me' where "userId" is null or "userId" = '';
update public.subscriptions  set "userId" = 'me' where "userId" is null or "userId" = '';
update public.fixed_expenses set "userId" = 'me' where "userId" is null or "userId" = '';
update public.notifications  set "userId" = 'me' where "userId" is null or "userId" = '';

-- Sacamos el default: de acá en más, cada insert declara su dueño explícitamente.
alter table public.categories     alter column "userId" drop default;
alter table public.expenses       alter column "userId" drop default;
alter table public.subscriptions  alter column "userId" drop default;
alter table public.fixed_expenses alter column "userId" drop default;
alter table public.notifications  alter column "userId" drop default;

-- ─────────────────── 3. FKs viejas fuera (se recrean compuestas) ───────────────────
-- Drop por catálogo en vez de por nombre: no dependemos de cómo los bautizó Postgres.
do $$
declare c record;
begin
  for c in
    select conrelid::regclass as tbl, conname
    from pg_constraint
    where contype = 'f'
      and confrelid = 'public.categories'::regclass
  loop
    execute format('alter table %s drop constraint %I', c.tbl, c.conname);
  end loop;
end $$;

-- ─────────────────── 4. PKs compuestas ───────────────────
do $$
declare t text;
begin
  foreach t in array array['categories','expenses','subscriptions','fixed_expenses','notifications']
  loop
    execute format(
      'alter table public.%I drop constraint if exists %I',
      t, t || '_pkey'
    );
    execute format('alter table public.%I add primary key (id, "userId")', t);
  end loop;
end $$;

-- ─────────────────── 5. Unicidad de nombre de categoría: por usuario ───────────────────
-- Antes era global (`name text not null unique`): con dos cuentas, la segunda no podría
-- tener su propia "Comida". Ahora el par (userId, name) es lo único.
do $$
declare c record;
begin
  for c in
    select conname from pg_constraint
    where conrelid = 'public.categories'::regclass and contype = 'u'
  loop
    execute format('alter table public.categories drop constraint %I', c.conname);
  end loop;
end $$;
drop index if exists public.categories_name_key;
create unique index if not exists categories_user_name_key
  on public.categories ("userId", name);

-- ─────────────────── 6. FKs compuestas ───────────────────
-- MATCH SIMPLE (default): si "categoryId" es NULL la FK no se evalúa, que es lo que
-- queremos para subscriptions."categoryId" nullable.
alter table public.expenses
  add constraint expenses_category_fkey
  foreign key ("categoryId", "userId") references public.categories (id, "userId")
  on update cascade on delete restrict;

alter table public.subscriptions
  add constraint subscriptions_category_fkey
  foreign key ("categoryId", "userId") references public.categories (id, "userId")
  on update cascade;

commit;

-- ─────────────────── verificación ───────────────────
-- Debe devolver 18 / 56 / 6 / 3 / 27, todo con userId = 'me'.
select 'categories' t, "userId", count(*) from public.categories group by 2
union all select 'expenses',       "userId", count(*) from public.expenses       group by 2
union all select 'subscriptions',  "userId", count(*) from public.subscriptions  group by 2
union all select 'fixed_expenses', "userId", count(*) from public.fixed_expenses group by 2
union all select 'notifications',  "userId", count(*) from public.notifications  group by 2;
