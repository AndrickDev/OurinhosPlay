-- Duração livre: de 30 minutos a 6 horas, em passos de 15 minutos (antes só 60, 90 ou 120).
-- Rodar no Supabase -> SQL Editor -> New query -> colar tudo -> Run. Pode rodar mais de uma vez.
-- Rode DEPOIS do 0015.


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
  if p_duration_min is null or p_duration_min < 30 or p_duration_min > 360 or p_duration_min % 15 <> 0 then
    raise exception 'A duração precisa ficar entre 30 minutos e 6 horas, de 15 em 15 minutos';
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
