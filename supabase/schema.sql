-- PredictaApp — esquema Supabase (Postgres)
-- Uso personal (single-user). Pegar tal cual en: Supabase → SQL Editor → Run.
--
-- IMPORTANTE sobre los nombres de columna:
-- postgrest-kt serializa con los nombres de propiedad Kotlin en camelCase
-- (dateMillis, categoryId, usagePct, ...). Postgres baja a minúsculas todo
-- identificador SIN comillas, así que las columnas camelCase van ENTRE COMILLAS
-- dobles para que el upsert encuentre la columna exacta. No las saques.

-- ───────────────────────── categories ───────────────────────
-- Categorías de gasto/ingreso. El nombre es ÚNICO: no pueden existir dos
-- categorías con el mismo nombre. type = EXPENSE | INCOME.
-- Va PRIMERO porque expenses la referencia por FK.
create table if not exists public.categories (
    id          bigint  primary key,
    name        text    not null unique,
    emoji       text    not null,
    color       text    not null default '#636E72',
    type        text    not null,                    -- EXPENSE | INCOME
    "isCustom"  boolean not null default false,
    "sortOrder" integer not null default 0
);

-- ───────────────────────── expenses ─────────────────────────
-- categoryId: FK a categories(id). Reemplaza el viejo `category` text suelto.
-- ON UPDATE CASCADE: renombrar/reasignar ids se propaga. ON DELETE RESTRICT: no se
-- borra una categoría con gastos sin antes reasignarlos (la app los mueve a "Otros").
create table if not exists public.expenses (
    id           bigint  primary key,
    merchant     text    not null,
    "categoryId" bigint  not null references public.categories(id) on update cascade on delete restrict,
    amount       integer not null,
    source       text    not null default 'MANUAL',  -- WHATSAPP_IMAGE | WHATSAPP_TEXT | WHATSAPP_AUDIO | MANUAL
    "dateMillis" bigint  not null
);

-- ──────────────────────── subscriptions ─────────────────────
create table if not exists public.subscriptions (
    id            text    primary key,
    service       text    not null,
    initial       text    not null,
    "usagePct"    integer not null default 0,
    monthly       integer not null,
    zombie        boolean not null default false,
    "lastUsedDate" text                            -- nullable (YYYY-MM-DD)
);

-- ──────────────────────── fixed_expenses ────────────────────
create table if not exists public.fixed_expenses (
    id             bigint  primary key,
    name           text    not null,
    amount         integer not null,
    "dueDayOfMonth" integer not null,
    active         boolean not null default true,
    "paidMonthKey" text    not null default ''      -- "YYYY-MM"
);

-- ───────────────────────── notifications ────────────────────
create table if not exists public.notifications (
    id          text    primary key,
    type        text    not null,                   -- WARNING | ANOMALY | PATTERN | ...
    icon        text    not null,
    "typeLabel" text    not null,
    title       text    not null,
    body        text    not null,
    time        text    not null,
    "dateGroup" text    not null,                   -- TODAY | YESTERDAY | OLDER
    unread      boolean not null default false,
    actions     jsonb   not null default '[]'::jsonb
);

-- ───────────────────────── profiles ────────────────────────
-- Single-user: una sola fila, id fijo "me". Respalda el perfil (que vive en
-- DataStore) para sobrevivir reinstalaciones.
create table if not exists public.profiles (
    id             text    primary key,
    name           text    not null,
    email          text    not null default '',
    income         integer not null default 0,
    "paydayDay"    integer not null default 1,
    "fixedMonthly" integer not null default 0
);

-- ───────────────────────── seguridad ────────────────────────
-- Single-user: RLS queda DESACTIVADO (default al crear por SQL). Con la anon key
-- la app lee/escribe directo. Tradeoff: la anon key viaja en el APK, así que
-- cualquiera que la extraiga puede tocar estas tablas. Aceptable para un proyecto
-- personal y privado. Si algún día publicás la app, activá RLS + Supabase Auth.
