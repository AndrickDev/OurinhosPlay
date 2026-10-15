-- Novos esportes + eventos (corrida etc.) com trajeto A -> B e destaque no topo do feed.
-- Rodar no Supabase -> SQL Editor -> New query -> colar tudo -> Run. Pode rodar mais de uma vez.
-- Rode DEPOIS do 0009.

-- ---------------------------------------------------------------- esportes
alter table public.matches drop constraint if exists matches_sport_check;
alter table public.matches
  add constraint matches_sport_check
  check (sport in (
    'futebol', 'volei', 'basquete', 'beach_tennis',
    'futsal', 'tenis', 'padel', 'natacao', 'biribol', 'corrida', 'ciclismo', 'caminhada'
  ));

-- ---------------------------------------------------------------- eventos
alter table public.matches add column if not exists is_event boolean not null default false;
alter table public.matches add column if not exists promoted boolean not null default false;
alter table public.matches add column if not exists route_start text;
alter table public.matches add column if not exists route_end text;

-- Eventos aceitam mais gente (corrida de rua, por exemplo).
alter table public.matches drop constraint if exists matches_capacity_check;
alter table public.matches add constraint matches_capacity_check check (capacity between 1 and 500);

-- Evento não ocupa quadra: o anti-choque vale só para jogos.
alter table public.matches drop constraint if exists matches_no_overlap;
alter table public.matches
  add constraint matches_no_overlap
  exclude using gist ((coalesce(court_id, venue_id)) with =, period with &&)
  where (status in ('open', 'full') and not is_event);

-- ---------------------------------------------------------------- feed (colunas novas só no fim)
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
  m.status,
  m.description,
  m.creator_id,
  m.court_id,
  c.name as court_name,
  c.is_free as court_is_free,
  c.price_per_hour_cents as court_price_cents,
  m.is_event,
  m.promoted,
  m.route_start,
  m.route_end
from public.matches m
join public.venues v on v.id = m.venue_id
left join public.courts c on c.id = m.court_id;

grant select on public.matches_feed to authenticated;

-- ---------------------------------------------------------------- admin: criar evento
create or replace function public.admin_create_event(
  p_venue_id uuid,
  p_sport text,
  p_title text,
  p_description text,
  p_starts_at timestamptz,
  p_duration_min int,
  p_capacity int,
  p_route_start text default null,
  p_route_end text default null,
  p_promoted boolean default true
)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_id uuid;
begin
  if not public.is_admin() then
    raise exception 'Só administradores podem criar eventos';
  end if;
  if p_title is null or length(btrim(p_title)) not between 2 and 60 then
    raise exception 'O título precisa ter de 2 a 60 caracteres';
  end if;
  if p_starts_at <= now() then
    raise exception 'O horário precisa ser no futuro';
  end if;
  if p_duration_min is null or p_duration_min < 15 or p_duration_min > 720 then
    raise exception 'Duração inválida';
  end if;
  if p_capacity is null or p_capacity < 1 or p_capacity > 500 then
    raise exception 'Vagas devem ficar entre 1 e 500';
  end if;
  if not exists (select 1 from public.venues where id = p_venue_id) then
    raise exception 'Local não encontrado';
  end if;

  insert into public.matches
    (venue_id, creator_id, sport, title, description, starts_at, ends_at, capacity,
     is_event, promoted, route_start, route_end)
  values
    (p_venue_id, auth.uid(), p_sport, btrim(p_title), nullif(btrim(coalesce(p_description, '')), ''),
     p_starts_at, p_starts_at + make_interval(mins => p_duration_min), p_capacity,
     true, coalesce(p_promoted, true),
     nullif(btrim(coalesce(p_route_start, '')), ''), nullif(btrim(coalesce(p_route_end, '')), ''))
  returning id into v_id;
  return v_id;
end;
$$;

create or replace function public.admin_set_promoted(p_match_id uuid, p_promoted boolean)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.is_admin() then
    raise exception 'Só administradores podem destacar eventos';
  end if;
  update public.matches set promoted = p_promoted where id = p_match_id;
end;
$$;

revoke all on function public.admin_create_event(uuid, text, text, text, timestamptz, int, int, text, text, boolean)
  from public, anon;
revoke all on function public.admin_set_promoted(uuid, boolean) from public, anon;
grant execute on function public.admin_create_event(uuid, text, text, text, timestamptz, int, int, text, text, boolean)
  to authenticated;
grant execute on function public.admin_set_promoted(uuid, boolean) to authenticated;
