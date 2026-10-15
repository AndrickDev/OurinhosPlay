-- Organização do jogo: rateio (Pix), jogo só por código, nível, "procurando", código curto
-- para convite e tênis de mesa.
-- Rodar no Supabase -> SQL Editor -> New query -> colar tudo -> Run. Pode rodar mais de uma vez.
-- Rode DEPOIS do 0013.

-- ---------------------------------------------------------------- esportes
alter table public.matches drop constraint if exists matches_sport_check;
alter table public.matches
  add constraint matches_sport_check
  check (sport in (
    'futebol', 'futevolei', 'volei', 'basquete', 'beach_tennis',
    'futsal', 'tenis', 'padel', 'natacao', 'biribol', 'corrida', 'ciclismo', 'caminhada',
    'tenis_de_mesa'
  ));

-- ---------------------------------------------------------------- colunas
alter table public.matches add column if not exists cost_cents int check (cost_cents is null or cost_cents >= 0);
alter table public.matches add column if not exists pix_key text check (pix_key is null or length(pix_key) <= 80);
alter table public.matches add column if not exists is_private boolean not null default false;
alter table public.matches add column if not exists level text not null default 'todos'
  check (level in ('todos', 'iniciante', 'intermediario', 'avancado'));
alter table public.matches add column if not exists looking_for text
  check (looking_for is null or length(looking_for) <= 60);

-- Código curto para convidar ("A1B2C3"): derivado do id, estável.
alter table public.matches add column if not exists short_code text
  generated always as (upper(substr(md5(id::text), 1, 6))) stored;
create index if not exists matches_short_code_idx on public.matches (short_code);

-- ---------------------------------------------------------------- feed
-- A chave Pix só aparece para quem organiza ou está na lista.
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
  m.route_end,
  m.cost_cents,
  case
    when m.creator_id = auth.uid()
      or exists (select 1 from public.participants p where p.match_id = m.id and p.user_id = auth.uid())
    then m.pix_key
  end as pix_key,
  m.is_private,
  m.level,
  m.looking_for,
  m.short_code
from public.matches m
join public.venues v on v.id = m.venue_id
left join public.courts c on c.id = m.court_id;

grant select on public.matches_feed to authenticated;

-- ---------------------------------------------------------------- achar jogo pelo código
create or replace function public.find_match_by_code(p_code text)
returns uuid
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_id uuid;
begin
  if auth.uid() is null then
    raise exception 'Não autenticado';
  end if;
  select id into v_id
    from public.matches
   where short_code = upper(btrim(p_code))
     and status in ('open', 'full')
     and ends_at > now()
   order by starts_at
   limit 1;
  if v_id is null then
    raise exception 'Não achei nenhum jogo com esse código';
  end if;
  return v_id;
end;
$$;

revoke all on function public.find_match_by_code(text) from public, anon;
grant execute on function public.find_match_by_code(text) to authenticated;

-- ---------------------------------------------------------------- create_match
drop function if exists public.create_match(uuid, text, text, text, timestamptz, int, int, boolean, uuid);

create or replace function public.create_match(
  p_venue_id uuid,
  p_sport text,
  p_title text,
  p_description text,
  p_starts_at timestamptz,
  p_duration_min int,
  p_capacity int,
  p_creator_joins boolean default true,
  p_court_id uuid default null,
  p_cost_cents int default null,
  p_pix_key text default null,
  p_is_private boolean default false,
  p_level text default 'todos',
  p_looking_for text default null
)
returns public.matches
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_venue public.venues;
  v_court public.courts;
  v_ends timestamptz;
  v_row public.matches;
begin
  if v_uid is null then
    raise exception 'Não autenticado';
  end if;
  if not exists (select 1 from public.profiles where id = v_uid and display_name is not null) then
    raise exception 'Defina seu nome antes de criar um jogo';
  end if;
  if p_title is null or length(btrim(p_title)) not between 2 and 60 then
    raise exception 'O título precisa ter de 2 a 60 caracteres';
  end if;
  if p_starts_at <= now() then
    raise exception 'O horário precisa ser no futuro';
  end if;
  if p_duration_min not in (60, 90, 120) then
    raise exception 'Duração inválida';
  end if;
  if p_capacity is null or p_capacity < 1 or p_capacity > 40 then
    raise exception 'Vagas devem ficar entre 1 e 40';
  end if;
  if p_cost_cents is not null and (p_cost_cents < 0 or p_cost_cents > 5000000) then
    raise exception 'Valor do rateio inválido';
  end if;
  if p_level not in ('todos', 'iniciante', 'intermediario', 'avancado') then
    raise exception 'Nível inválido';
  end if;

  select * into v_venue from public.venues where id = p_venue_id;
  if not found then
    raise exception 'Local não encontrado';
  end if;

  if p_court_id is not null then
    select * into v_court from public.courts where id = p_court_id and venue_id = p_venue_id;
    if not found then
      raise exception 'Quadra não encontrada nesse local';
    end if;
    if not (p_sport = any (v_court.sports)) then
      raise exception 'Essa quadra não é cadastrada para esse esporte';
    end if;
  else
    if exists (select 1 from public.courts c where c.venue_id = p_venue_id) then
      raise exception 'Escolha a quadra';
    end if;
    if not (p_sport = any (v_venue.sports)) then
      raise exception 'Esse local não é cadastrado para esse esporte';
    end if;
  end if;

  v_ends := p_starts_at + make_interval(mins => p_duration_min);

  if exists (
    select 1 from public.venue_blocks b
     where b.venue_id = p_venue_id
       and (b.court_id is null or b.court_id = p_court_id)
       and b.period && tstzrange(p_starts_at, v_ends, '[)')
  ) then
    raise exception 'Horário bloqueado pela administração da quadra' using errcode = 'P0002';
  end if;

  begin
    insert into public.matches
      (venue_id, court_id, creator_id, sport, title, description, starts_at, ends_at, capacity,
       cost_cents, pix_key, is_private, level, looking_for)
    values
      (p_venue_id, p_court_id, v_uid, p_sport, btrim(p_title), nullif(btrim(coalesce(p_description, '')), ''),
       p_starts_at, v_ends, p_capacity,
       p_cost_cents, nullif(btrim(coalesce(p_pix_key, '')), ''), coalesce(p_is_private, false), p_level,
       nullif(btrim(coalesce(p_looking_for, '')), ''))
    returning * into v_row;
  exception when exclusion_violation then
    raise exception 'Já existe um jogo nessa quadra nesse horário' using errcode = 'P0001';
  end;

  if p_creator_joins then
    insert into public.participants (match_id, user_id) values (v_row.id, v_uid);
    if p_capacity <= 1 then
      update public.matches set status = 'full' where id = v_row.id returning * into v_row;
    end if;
  end if;

  return v_row;
end;
$$;

revoke all on function public.create_match(uuid, text, text, text, timestamptz, int, int, boolean, uuid, int, text, boolean, text, text)
  from public, anon;
grant execute on function public.create_match(uuid, text, text, text, timestamptz, int, int, boolean, uuid, int, text, boolean, text, text)
  to authenticated;
