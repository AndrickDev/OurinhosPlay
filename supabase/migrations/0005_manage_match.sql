-- Sprint 4.5: gerenciar jogo (editar, cancelar, excluir), jogo de 1 vaga e
-- organizador que só organiza. Rodar no Supabase -> SQL Editor. Pode rodar mais de uma vez.

-- 1 vaga é permitida (útil para testar e para "preciso de 1 pessoa").
alter table public.matches drop constraint if exists matches_capacity_check;
alter table public.matches add constraint matches_capacity_check check (capacity between 1 and 40);

-- ---------------------------------------------------------------- create_match
-- Nova assinatura: p_creator_joins (o organizador entra na lista ou só organiza).
drop function if exists public.create_match(uuid, text, text, text, timestamptz, int, int);

create or replace function public.create_match(
  p_venue_id uuid,
  p_sport text,
  p_title text,
  p_description text,
  p_starts_at timestamptz,
  p_duration_min int,
  p_capacity int,
  p_creator_joins boolean default true
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
  if p_capacity is null or p_capacity < 1 or p_capacity > 40 then
    raise exception 'Vagas devem ficar entre 1 e 40';
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
    raise exception 'Horário bloqueado pela administração da quadra' using errcode = 'P0002';
  end if;

  begin
    insert into public.matches
      (venue_id, creator_id, sport, title, description, starts_at, ends_at, capacity)
    values
      (p_venue_id, v_uid, p_sport, btrim(p_title), nullif(btrim(coalesce(p_description, '')), ''),
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

revoke all on function public.create_match(uuid, text, text, text, timestamptz, int, int, boolean)
  from public, anon;
grant execute on function public.create_match(uuid, text, text, text, timestamptz, int, int, boolean)
  to authenticated;

-- ---------------------------------------------------------------- leave_match
-- O organizador também pode sair da lista (o jogo continua dele).
create or replace function public.leave_match(p_match_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_match public.matches;
begin
  if v_uid is null then
    raise exception 'Não autenticado';
  end if;

  select * into v_match from public.matches where id = p_match_id for update;
  if not found then
    raise exception 'Jogo não encontrado';
  end if;
  if v_match.status in ('cancelled', 'finished') or v_match.ends_at <= now() then
    raise exception 'Esse jogo já terminou';
  end if;

  delete from public.participants where match_id = p_match_id and user_id = v_uid;
  if not found then
    raise exception 'Você não está nesse jogo';
  end if;

  if v_match.status = 'full' then
    update public.matches set status = 'open' where id = p_match_id;
  end if;
end;
$$;

revoke all on function public.leave_match(uuid) from public, anon;
grant execute on function public.leave_match(uuid) to authenticated;

-- ---------------------------------------------------------------- permissões de gestão
-- Quem pode mexer no jogo: o organizador ou um admin.
create or replace function public.can_manage_match(p_match_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.matches m
     where m.id = p_match_id
       and (
         m.creator_id = auth.uid()
         or exists (select 1 from public.profiles p where p.id = auth.uid() and p.role = 'admin')
       )
  );
$$;

revoke all on function public.can_manage_match(uuid) from public, anon;
grant execute on function public.can_manage_match(uuid) to authenticated;

-- ---------------------------------------------------------------- update_match
-- Edita o que não mexe em quadra/horário (esses exigem refazer o anti-choque): crie outro jogo.
create or replace function public.update_match(
  p_match_id uuid,
  p_title text,
  p_description text,
  p_capacity int
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_match public.matches;
  v_taken int;
begin
  if auth.uid() is null then
    raise exception 'Não autenticado';
  end if;
  select * into v_match from public.matches where id = p_match_id for update;
  if not found then
    raise exception 'Jogo não encontrado';
  end if;
  if not public.can_manage_match(p_match_id) then
    raise exception 'Só o organizador pode editar esse jogo';
  end if;
  if v_match.status in ('cancelled', 'finished') then
    raise exception 'Esse jogo não pode mais ser editado';
  end if;
  if p_title is null or length(btrim(p_title)) not between 2 and 60 then
    raise exception 'O título precisa ter de 2 a 60 caracteres';
  end if;

  select count(*) into v_taken from public.participants where match_id = p_match_id;
  if p_capacity is null or p_capacity < 1 or p_capacity > 40 then
    raise exception 'Vagas devem ficar entre 1 e 40';
  end if;
  if p_capacity < v_taken then
    raise exception 'Já tem % confirmados; as vagas não podem ser menos que isso', v_taken;
  end if;

  update public.matches
     set title = btrim(p_title),
         description = nullif(btrim(coalesce(p_description, '')), ''),
         capacity = p_capacity,
         status = case when v_taken >= p_capacity then 'full' else 'open' end
   where id = p_match_id;
end;
$$;

revoke all on function public.update_match(uuid, text, text, int) from public, anon;
grant execute on function public.update_match(uuid, text, text, int) to authenticated;

-- ---------------------------------------------------------------- cancel_match / delete_match
create or replace function public.cancel_match(p_match_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'Não autenticado';
  end if;
  if not public.can_manage_match(p_match_id) then
    raise exception 'Só o organizador pode cancelar esse jogo';
  end if;
  update public.matches set status = 'cancelled' where id = p_match_id;
end;
$$;

create or replace function public.delete_match(p_match_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'Não autenticado';
  end if;
  if not public.can_manage_match(p_match_id) then
    raise exception 'Só o organizador pode excluir esse jogo';
  end if;
  delete from public.matches where id = p_match_id;
end;
$$;

revoke all on function public.cancel_match(uuid) from public, anon;
revoke all on function public.delete_match(uuid) from public, anon;
grant execute on function public.cancel_match(uuid) to authenticated;
grant execute on function public.delete_match(uuid) to authenticated;

-- ---------------------------------------------------------------- virar admin (OPCIONAL)
-- Admin pode cancelar/editar/excluir jogo de qualquer pessoa. O organizador já gerencia o
-- próprio jogo sem isso. Para se tornar admin, rode SEPARADAMENTE, trocando o e-mail:
--
--   update public.profiles set role = 'admin'
--    where id = (select id from auth.users where email = 'SEU_EMAIL@gmail.com');
