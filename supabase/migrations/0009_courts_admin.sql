-- Sprint 6: várias quadras por local (courts), pago x grátis com preço, painel admin.
-- Rodar no Supabase -> SQL Editor -> New query -> colar tudo -> Run. Pode rodar mais de uma vez.
-- Rode DEPOIS do 0006 e 0007.

-- ---------------------------------------------------------------- courts
create table if not exists public.courts (
  id uuid primary key default gen_random_uuid(),
  venue_id uuid not null references public.venues(id) on delete cascade,
  name text not null check (length(btrim(name)) between 1 and 60),
  sports text[] not null default '{}',
  is_free boolean not null default true,
  price_per_hour_cents int check (price_per_hour_cents is null or price_per_hour_cents >= 0),
  audience text,                 -- quem pode usar: "Aberto ao público", "Sócios", "Alunos"...
  created_at timestamptz not null default now()
);

create unique index if not exists courts_venue_name_uniq on public.courts (venue_id, lower(name));

alter table public.courts enable row level security;

drop policy if exists "courts_select_authenticated" on public.courts;
create policy "courts_select_authenticated"
  on public.courts for select to authenticated using (true);

revoke insert, update, delete on public.courts from authenticated, anon;
grant select on public.courts to authenticated;

-- jogos e bloqueios podem apontar para uma quadra específica (null = local inteiro / sem quadras)
alter table public.matches add column if not exists court_id uuid references public.courts(id) on delete restrict;
alter table public.venue_blocks add column if not exists court_id uuid references public.courts(id) on delete cascade;

-- anti-choque passa a valer por quadra (ou pelo local, quando o jogo não tem quadra)
alter table public.matches drop constraint if exists matches_no_overlap;
alter table public.matches
  add constraint matches_no_overlap
  exclude using gist ((coalesce(court_id, venue_id)) with =, period with &&)
  where (status in ('open', 'full'));

-- Uma "Quadra principal" para cada local que ainda não tem quadra. O admin ajusta depois.
insert into public.courts (venue_id, name, sports, is_free, price_per_hour_cents, audience)
select v.id, 'Quadra principal', v.sports, (v.kind = 'public'),
       case when lower(v.name) like 'orla beach tennis%' then 7000 else null end,
       case when v.kind = 'public' then 'Aberto ao público' else null end
  from public.venues v
 where not exists (select 1 from public.courts c where c.venue_id = v.id);

-- Jogos antigos passam a apontar para a quadra principal do local.
update public.matches m
   set court_id = (select c.id from public.courts c where c.venue_id = m.venue_id order by c.created_at limit 1)
 where m.court_id is null
   and exists (select 1 from public.courts c where c.venue_id = m.venue_id);

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
  c.price_per_hour_cents as court_price_cents
from public.matches m
join public.venues v on v.id = m.venue_id
left join public.courts c on c.id = m.court_id;

grant select on public.matches_feed to authenticated;

-- ---------------------------------------------------------------- is_admin
create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'admin');
$$;

revoke all on function public.is_admin() from public, anon;
grant execute on function public.is_admin() to authenticated;

-- ---------------------------------------------------------------- disponibilidade (por quadra)
drop function if exists public.venue_availability(uuid, timestamptz, int);

create or replace function public.venue_availability(
  p_venue_id uuid,
  p_starts_at timestamptz,
  p_duration_min int,
  p_court_id uuid default null
)
returns table (
  is_free boolean,
  reason text,
  busy_until timestamptz,
  next_free_at timestamptz
)
language plpgsql
stable
security invoker
set search_path = public
as $$
declare
  v_dur interval := make_interval(mins => p_duration_min);
  v_key uuid := coalesce(p_court_id, p_venue_id);
  v_start timestamptz := p_starts_at;
  v_first_reason text;
  v_first_until timestamptz;
  v_until timestamptz;
  v_reason text;
  v_i int := 0;
begin
  if p_duration_min is null or p_duration_min < 15 or p_duration_min > 480 then
    raise exception 'Duração inválida';
  end if;

  loop
    v_i := v_i + 1;
    v_until := null;
    v_reason := null;

    select max(upper(m.period)), 'match' into v_until, v_reason
      from public.matches m
     where coalesce(m.court_id, m.venue_id) = v_key
       and m.status in ('open', 'full')
       and m.period && tstzrange(v_start, v_start + v_dur, '[)');

    if v_until is null then
      select max(upper(b.period)), 'block' into v_until, v_reason
        from public.venue_blocks b
       where b.venue_id = p_venue_id
         and (b.court_id is null or b.court_id = p_court_id)
         and b.period && tstzrange(v_start, v_start + v_dur, '[)');
    end if;

    if v_until is null then
      if v_i = 1 then
        return query select true, null::text, null::timestamptz, null::timestamptz;
      else
        return query select false, v_first_reason, v_first_until, v_start;
      end if;
      return;
    end if;

    if v_i = 1 then
      v_first_reason := v_reason;
      v_first_until := v_until;
    end if;

    v_start := v_until;
    exit when v_i >= 30;
  end loop;

  return query select false, v_first_reason, v_first_until, null::timestamptz;
end;
$$;

grant execute on function public.venue_availability(uuid, timestamptz, int, uuid) to authenticated;

-- ---------------------------------------------------------------- create_match (com quadra)
drop function if exists public.create_match(uuid, text, text, text, timestamptz, int, int, boolean);

create or replace function public.create_match(
  p_venue_id uuid,
  p_sport text,
  p_title text,
  p_description text,
  p_starts_at timestamptz,
  p_duration_min int,
  p_capacity int,
  p_creator_joins boolean default true,
  p_court_id uuid default null
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
      (venue_id, court_id, creator_id, sport, title, description, starts_at, ends_at, capacity)
    values
      (p_venue_id, p_court_id, v_uid, p_sport, btrim(p_title), nullif(btrim(coalesce(p_description, '')), ''),
       p_starts_at, v_ends, p_capacity)
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

revoke all on function public.create_match(uuid, text, text, text, timestamptz, int, int, boolean, uuid)
  from public, anon;
grant execute on function public.create_match(uuid, text, text, text, timestamptz, int, int, boolean, uuid)
  to authenticated;

-- ---------------------------------------------------------------- admin: quadras
create or replace function public.admin_save_court(
  p_venue_id uuid,
  p_name text,
  p_sports text[],
  p_is_free boolean,
  p_price_cents int,
  p_audience text default null,
  p_court_id uuid default null
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
    raise exception 'Só administradores podem gerenciar quadras';
  end if;
  if p_name is null or length(btrim(p_name)) not between 1 and 60 then
    raise exception 'Dê um nome à quadra (até 60 letras)';
  end if;
  if not exists (select 1 from public.venues where id = p_venue_id) then
    raise exception 'Local não encontrado';
  end if;
  if not p_is_free and (p_price_cents is null or p_price_cents < 0) then
    raise exception 'Informe o preço por hora';
  end if;

  begin
    if p_court_id is null then
      insert into public.courts (venue_id, name, sports, is_free, price_per_hour_cents, audience)
      values (p_venue_id, btrim(p_name), coalesce(p_sports, '{}'), p_is_free,
              case when p_is_free then null else p_price_cents end, nullif(btrim(coalesce(p_audience, '')), ''))
      returning id into v_id;
    else
      update public.courts
         set name = btrim(p_name), sports = coalesce(p_sports, '{}'), is_free = p_is_free,
             price_per_hour_cents = case when p_is_free then null else p_price_cents end,
             audience = nullif(btrim(coalesce(p_audience, '')), '')
       where id = p_court_id and venue_id = p_venue_id
       returning id into v_id;
      if v_id is null then
        raise exception 'Quadra não encontrada';
      end if;
    end if;
  exception when unique_violation then
    raise exception 'Já existe uma quadra com esse nome nesse local';
  end;
  return v_id;
end;
$$;

create or replace function public.admin_delete_court(p_court_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.is_admin() then
    raise exception 'Só administradores podem gerenciar quadras';
  end if;
  begin
    delete from public.courts where id = p_court_id;
  exception when foreign_key_violation then
    raise exception 'Essa quadra já tem jogos; cancele ou exclua os jogos antes';
  end;
end;
$$;

-- ---------------------------------------------------------------- admin: bloqueios
create or replace function public.admin_add_block(
  p_venue_id uuid,
  p_starts_at timestamptz,
  p_ends_at timestamptz,
  p_reason text default null,
  p_court_id uuid default null
)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_id uuid;
  v_n int;
begin
  if not public.is_admin() then
    raise exception 'Só administradores podem bloquear horários';
  end if;
  if p_ends_at <= p_starts_at then
    raise exception 'O fim precisa ser depois do início';
  end if;
  if p_court_id is not null and not exists (
    select 1 from public.courts where id = p_court_id and venue_id = p_venue_id
  ) then
    raise exception 'Quadra não encontrada nesse local';
  end if;

  select count(*) into v_n
    from public.matches m
   where m.venue_id = p_venue_id
     and (p_court_id is null or m.court_id = p_court_id)
     and m.status in ('open', 'full')
     and m.period && tstzrange(p_starts_at, p_ends_at, '[)');
  if v_n > 0 then
    raise exception 'Há % jogo(s) nesse horário; cancele antes de bloquear', v_n;
  end if;

  insert into public.venue_blocks (venue_id, court_id, starts_at, ends_at, reason)
  values (p_venue_id, p_court_id, p_starts_at, p_ends_at, nullif(btrim(coalesce(p_reason, '')), ''))
  returning id into v_id;
  return v_id;
end;
$$;

create or replace function public.admin_remove_block(p_block_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.is_admin() then
    raise exception 'Só administradores podem bloquear horários';
  end if;
  delete from public.venue_blocks where id = p_block_id;
end;
$$;

revoke all on function public.admin_save_court(uuid, text, text[], boolean, int, text, uuid) from public, anon;
revoke all on function public.admin_delete_court(uuid) from public, anon;
revoke all on function public.admin_add_block(uuid, timestamptz, timestamptz, text, uuid) from public, anon;
revoke all on function public.admin_remove_block(uuid) from public, anon;
grant execute on function public.admin_save_court(uuid, text, text[], boolean, int, text, uuid) to authenticated;
grant execute on function public.admin_delete_court(uuid) to authenticated;
grant execute on function public.admin_add_block(uuid, timestamptz, timestamptz, text, uuid) to authenticated;
grant execute on function public.admin_remove_block(uuid) to authenticated;
