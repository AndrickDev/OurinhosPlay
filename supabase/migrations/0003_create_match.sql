-- Sprint 3: bloqueios de quadra, disponibilidade e criação de jogo (anti-choque).
-- Rodar no Supabase -> SQL Editor -> New query -> Run. Pode rodar mais de uma vez.

-- ---------------------------------------------------------------- venue_blocks
-- Horários que o admin tira do ar (manutenção, evento oficial). Gestão pelo painel: sprint 6.
create table if not exists public.venue_blocks (
  id uuid primary key default gen_random_uuid(),
  venue_id uuid not null references public.venues(id) on delete cascade,
  starts_at timestamptz not null,
  ends_at timestamptz not null,
  reason text,
  period tstzrange generated always as (tstzrange(starts_at, ends_at, '[)')) stored,
  created_at timestamptz not null default now(),
  check (ends_at > starts_at)
);

create index if not exists venue_blocks_venue_idx on public.venue_blocks (venue_id, starts_at);

alter table public.venue_blocks enable row level security;

drop policy if exists "venue_blocks_select_authenticated" on public.venue_blocks;
create policy "venue_blocks_select_authenticated"
  on public.venue_blocks for select to authenticated using (true);

revoke insert, update, delete on public.venue_blocks from authenticated, anon;

-- ---------------------------------------------------------------- disponibilidade
-- Diz se a quadra está livre no intervalo e, se não, até quando e qual o próximo
-- horário livre que comporta a mesma duração. Só orienta a UX: quem decide é create_match.
create or replace function public.venue_availability(
  p_venue_id uuid,
  p_starts_at timestamptz,
  p_duration_min int
)
returns table (
  is_free boolean,
  reason text,        -- 'match' | 'block' | null
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

    select max(upper(m.period)), 'match' into v_until, v_reason
      from public.matches m
     where m.venue_id = p_venue_id
       and m.status in ('open', 'full')
       and m.period && tstzrange(v_start, v_start + v_dur, '[)');

    if v_until is null then
      select max(upper(b.period)), 'block' into v_until, v_reason
        from public.venue_blocks b
       where b.venue_id = p_venue_id
         and b.period && tstzrange(v_start, v_start + v_dur, '[)');
    end if;

    if v_until is null then
      -- Livre em v_start. Se v_start == pedido, está tudo livre; senão é a sugestão.
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

  -- Agenda muito cheia: informa o choque sem sugestão.
  return query select false, v_first_reason, v_first_until, null::timestamptz;
end;
$$;

grant execute on function public.venue_availability(uuid, timestamptz, int) to authenticated;

-- ---------------------------------------------------------------- create_match
create or replace function public.create_match(
  p_venue_id uuid,
  p_sport text,
  p_title text,
  p_description text,
  p_starts_at timestamptz,
  p_duration_min int,
  p_capacity int
)
returns public.matches
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_venue public.venues;
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
  if p_capacity is null or p_capacity < 2 or p_capacity > 40 then
    raise exception 'Vagas devem ficar entre 2 e 40';
  end if;

  select * into v_venue from public.venues where id = p_venue_id;
  if not found then
    raise exception 'Quadra não encontrada';
  end if;
  if not (p_sport = any (v_venue.sports)) then
    raise exception 'Essa quadra não é cadastrada para esse esporte';
  end if;

  v_ends := p_starts_at + make_interval(mins => p_duration_min);

  if exists (
    select 1 from public.venue_blocks b
     where b.venue_id = p_venue_id
       and b.period && tstzrange(p_starts_at, v_ends, '[)')
  ) then
    raise exception 'Horário bloqueado pela administração da quadra'
      using errcode = 'P0002';
  end if;

  begin
    insert into public.matches
      (venue_id, creator_id, sport, title, description, starts_at, ends_at, capacity)
    values
      (p_venue_id, v_uid, p_sport, btrim(p_title), nullif(btrim(coalesce(p_description, '')), ''),
       p_starts_at, v_ends, p_capacity)
    returning * into v_row;
  exception when exclusion_violation then
    raise exception 'Já existe um jogo nessa quadra nesse horário'
      using errcode = 'P0001';
  end;

  insert into public.participants (match_id, user_id) values (v_row.id, v_uid);

  -- Capacidade mínima já é 2, então o criador sozinho nunca lota o jogo.
  return v_row;
end;
$$;

revoke all on function public.create_match(uuid, text, text, text, timestamptz, int, int)
  from public, anon;
grant execute on function public.create_match(uuid, text, text, text, timestamptz, int, int)
  to authenticated;
