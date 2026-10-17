-- Funções por vaga (goleiro, levantador...), vaga reservada com liberação automática,
-- benefício no rateio e "abrir vagas" numa reserva rápida.
-- Rodar no Supabase -> SQL Editor -> New query -> colar tudo -> Run. Pode rodar mais de uma vez.
-- Rode DEPOIS do 0014.

-- ---------------------------------------------------------------- tabelas
create table if not exists public.match_roles (
  match_id uuid not null references public.matches(id) on delete cascade,
  role text not null check (length(role) between 2 and 24),
  slots int not null check (slots between 1 and 40),
  -- Reservada: ninguém ocupa essa vaga por outra função até `release_hours` antes do jogo.
  reserved boolean not null default false,
  release_hours int not null default 3 check (release_hours between 0 and 72),
  -- Quanto do rateio a pessoa nessa função deixa de pagar: 0, 50 ou 100.
  perk_pct int not null default 0 check (perk_pct in (0, 50, 100)),
  primary key (match_id, role)
);

alter table public.match_roles enable row level security;
drop policy if exists match_roles_read on public.match_roles;
create policy match_roles_read on public.match_roles for select to authenticated using (true);
grant select on public.match_roles to authenticated;

alter table public.participants add column if not exists role text
  check (role is null or length(role) between 2 and 24);

-- ---------------------------------------------------------------- situação das funções
create or replace function public.match_roles_status(p_match_id uuid)
returns table (
  role text,
  slots int,
  taken int,
  reserved boolean,
  locked boolean,
  release_at timestamptz,
  perk_pct int
)
language sql
stable
security definer
set search_path = public
as $$
  select
    r.role,
    r.slots,
    (select count(*) from public.participants p where p.match_id = r.match_id and p.role = r.role)::int,
    r.reserved,
    (r.reserved and now() < m.starts_at - make_interval(hours => r.release_hours)),
    m.starts_at - make_interval(hours => r.release_hours),
    r.perk_pct
  from public.match_roles r
  join public.matches m on m.id = r.match_id
  where r.match_id = p_match_id
  order by r.role;
$$;

revoke all on function public.match_roles_status(uuid) from public, anon;
grant execute on function public.match_roles_status(uuid) to authenticated;

-- ---------------------------------------------------------------- feed: "falta goleiro"
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
  m.short_code,
  coalesce((
    select array_agg(r.role order by r.role)
      from public.match_roles r
     where r.match_id = m.id
       and r.slots > (select count(*) from public.participants p where p.match_id = r.match_id and p.role = r.role)
  ), '{}') as roles_open
from public.matches m
join public.venues v on v.id = m.venue_id
left join public.courts c on c.id = m.court_id;

grant select on public.matches_feed to authenticated;

-- ---------------------------------------------------------------- create_match (com funções)
drop function if exists public.create_match(uuid, text, text, text, timestamptz, int, int, boolean, uuid, int, text, boolean, text, text);

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
  p_looking_for text default null,
  -- [{"role":"goleiro","slots":2,"reserved":true,"release_hours":3,"perk_pct":100}]
  p_roles jsonb default null
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
  v_role jsonb;
  v_sum int := 0;
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

  if p_roles is not null and jsonb_typeof(p_roles) = 'array' then
    for v_role in select * from jsonb_array_elements(p_roles) loop
      v_sum := v_sum + coalesce((v_role ->> 'slots')::int, 0);
    end loop;
    if v_sum > p_capacity then
      raise exception 'As vagas por função passam do total de vagas';
    end if;
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

  if p_roles is not null and jsonb_typeof(p_roles) = 'array' then
    for v_role in select * from jsonb_array_elements(p_roles) loop
      insert into public.match_roles (match_id, role, slots, reserved, release_hours, perk_pct)
      values (
        v_row.id,
        btrim(v_role ->> 'role'),
        (v_role ->> 'slots')::int,
        coalesce((v_role ->> 'reserved')::boolean, false),
        coalesce((v_role ->> 'release_hours')::int, 3),
        coalesce((v_role ->> 'perk_pct')::int, 0)
      );
    end loop;
  end if;

  if p_creator_joins then
    insert into public.participants (match_id, user_id) values (v_row.id, v_uid);
    if p_capacity <= 1 then
      update public.matches set status = 'full' where id = v_row.id returning * into v_row;
    end if;
  end if;

  return v_row;
end;
$$;

revoke all on function public.create_match(uuid, text, text, text, timestamptz, int, int, boolean, uuid, int, text, boolean, text, text, jsonb)
  from public, anon;
grant execute on function public.create_match(uuid, text, text, text, timestamptz, int, int, boolean, uuid, int, text, boolean, text, text, jsonb)
  to authenticated;

-- ---------------------------------------------------------------- join_match (com função)
drop function if exists public.join_match(uuid);

create or replace function public.join_match(p_match_id uuid, p_role text default null)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_match public.matches;
  v_taken int;
  v_role public.match_roles;
  v_role_taken int;
  v_locked int;
begin
  if v_uid is null then
    raise exception 'Não autenticado';
  end if;
  if not exists (select 1 from public.profiles where id = v_uid and display_name is not null) then
    raise exception 'Defina seu nome antes de entrar em um jogo';
  end if;

  -- Trava a partida: duas pessoas pegando a última vaga ficam em fila, não em corrida.
  select * into v_match from public.matches where id = p_match_id for update;
  if not found then
    raise exception 'Jogo não encontrado';
  end if;
  if v_match.status = 'cancelled' then
    raise exception 'Esse jogo foi cancelado';
  end if;
  if v_match.status = 'finished' or v_match.ends_at <= now() then
    raise exception 'Esse jogo já terminou';
  end if;
  if exists (select 1 from public.participants where match_id = p_match_id and user_id = v_uid) then
    raise exception 'Você já está nesse jogo';
  end if;

  select count(*) into v_taken from public.participants where match_id = p_match_id;
  if v_taken >= v_match.capacity then
    raise exception 'Jogo lotado';
  end if;

  if p_role is not null then
    select * into v_role from public.match_roles where match_id = p_match_id and role = p_role;
    if not found then
      raise exception 'Esse jogo não tem a função %', p_role;
    end if;
    select count(*) into v_role_taken from public.participants where match_id = p_match_id and role = p_role;
    if v_role_taken >= v_role.slots then
      raise exception 'As vagas de % já foram preenchidas', p_role;
    end if;
  end if;

  -- Vagas reservadas ainda vazias (de outras funções) não podem ser ocupadas por quem entra.
  select coalesce(sum(greatest(r.slots - (
           select count(*) from public.participants p where p.match_id = r.match_id and p.role = r.role
         ), 0)), 0)::int
    into v_locked
    from public.match_roles r
   where r.match_id = p_match_id
     and r.reserved
     and r.role is distinct from p_role
     and now() < v_match.starts_at - make_interval(hours => r.release_hours);

  if v_taken + v_locked >= v_match.capacity then
    raise exception 'As vagas que sobraram são reservadas para uma função. Escolha a função.';
  end if;

  insert into public.participants (match_id, user_id, role) values (p_match_id, v_uid, p_role);

  if v_taken + 1 >= v_match.capacity then
    update public.matches set status = 'full' where id = p_match_id;
  end if;
end;
$$;

revoke all on function public.join_match(uuid, text) from public, anon;
grant execute on function public.join_match(uuid, text) to authenticated;

-- ---------------------------------------------------------------- abrir vagas numa reserva
create or replace function public.open_reservation(p_match_id uuid, p_capacity int)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_match public.matches;
  v_taken int;
begin
  if v_uid is null then
    raise exception 'Não autenticado';
  end if;
  select * into v_match from public.matches where id = p_match_id for update;
  if not found then
    raise exception 'Jogo não encontrado';
  end if;
  if v_match.creator_id <> v_uid then
    raise exception 'Só quem reservou pode abrir as vagas';
  end if;
  if v_match.status in ('cancelled', 'finished') or v_match.ends_at <= now() then
    raise exception 'Esse jogo já terminou';
  end if;
  if p_capacity is null or p_capacity < 2 or p_capacity > 40 then
    raise exception 'Vagas devem ficar entre 2 e 40';
  end if;
  select count(*) into v_taken from public.participants where match_id = p_match_id;
  update public.matches
     set is_private = false,
         capacity = p_capacity,
         status = case when v_taken >= p_capacity then 'full' else 'open' end
   where id = p_match_id;
end;
$$;

revoke all on function public.open_reservation(uuid, int) from public, anon;
grant execute on function public.open_reservation(uuid, int) to authenticated;
