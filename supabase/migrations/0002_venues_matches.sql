-- Sprint 2: quadras (venues), partidas (matches), participantes e anti-choque.
-- Rodar no Supabase -> SQL Editor -> New query -> Run. Pode rodar mais de uma vez.

create extension if not exists btree_gist;

-- ---------------------------------------------------------------- venues
create table if not exists public.venues (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  kind text not null default 'public' check (kind in ('public', 'private')),
  address text not null,
  lat double precision,
  lng double precision,
  sports text[] not null default '{}',
  manager_id uuid references public.profiles(id) on delete set null,
  created_at timestamptz not null default now()
);

create unique index if not exists venues_name_uniq on public.venues (lower(name));

-- ---------------------------------------------------------------- matches
create table if not exists public.matches (
  id uuid primary key default gen_random_uuid(),
  venue_id uuid not null references public.venues(id) on delete restrict,
  creator_id uuid not null references public.profiles(id) on delete cascade,
  sport text not null check (sport in ('futebol', 'volei', 'basquete', 'beach_tennis')),
  title text not null check (length(btrim(title)) between 2 and 60),
  description text,
  starts_at timestamptz not null,
  ends_at timestamptz not null,
  capacity int not null check (capacity between 2 and 40),
  status text not null default 'open' check (status in ('open', 'full', 'cancelled', 'finished')),
  period tstzrange generated always as (tstzrange(starts_at, ends_at, '[)')) stored,
  created_at timestamptz not null default now(),
  check (ends_at > starts_at)
);

create index if not exists matches_feed_idx on public.matches (status, starts_at);

-- Anti-choque: dois jogos ativos não ocupam a mesma quadra ao mesmo tempo.
do $$
begin
  alter table public.matches
    add constraint matches_no_overlap
    exclude using gist (venue_id with =, period with &&)
    where (status in ('open', 'full'));
exception when duplicate_object then null;
end $$;

-- ---------------------------------------------------------------- participants
create table if not exists public.participants (
  match_id uuid not null references public.matches(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  joined_at timestamptz not null default now(),
  primary key (match_id, user_id)
);

-- ---------------------------------------------------------------- RLS
alter table public.venues enable row level security;
alter table public.matches enable row level security;
alter table public.participants enable row level security;

drop policy if exists "venues_select_authenticated" on public.venues;
create policy "venues_select_authenticated"
  on public.venues for select to authenticated using (true);

drop policy if exists "matches_select_authenticated" on public.matches;
create policy "matches_select_authenticated"
  on public.matches for select to authenticated using (true);

drop policy if exists "participants_select_authenticated" on public.participants;
create policy "participants_select_authenticated"
  on public.participants for select to authenticated using (true);

-- Escrita: nada direto pelo cliente. Criar/entrar/sair virão por RPC (sprints 3 e 4);
-- quadras são cadastradas pelo painel (sprint 6) ou por este seed.
revoke insert, update, delete on public.venues, public.matches, public.participants
  from authenticated, anon;

-- ---------------------------------------------------------------- feed
-- View com vaga ocupada e nome da quadra. security_invoker respeita a RLS de quem lê.
create or replace view public.matches_feed
with (security_invoker = true) as
select
  m.id,
  m.venue_id,
  v.name  as venue_name,
  m.sport,
  m.title,
  m.starts_at,
  m.ends_at,
  m.capacity,
  (select count(*) from public.participants p where p.match_id = m.id)::int as taken,
  m.status
from public.matches m
join public.venues v on v.id = m.venue_id;

grant select on public.matches_feed to authenticated;

-- ---------------------------------------------------------------- seed
-- ATENÇÃO: nomes/endereços abaixo são PLACEHOLDERS. Troque pelas quadras reais de
-- Ourinhos (update/insert aqui ou no Table Editor) antes de usar com outras pessoas.
insert into public.venues (name, kind, address, sports) values
  ('Quadra Poliesportiva (exemplo 1)', 'public',  'Ourinhos - SP', array['futebol', 'basquete']),
  ('Quadra de Areia (exemplo 2)',      'public',  'Ourinhos - SP', array['volei', 'beach_tennis']),
  ('Arena Society (exemplo 3)',        'private', 'Ourinhos - SP', array['futebol'])
on conflict do nothing;
