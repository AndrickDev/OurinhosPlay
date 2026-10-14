-- Sprint 4: entrar/sair de partida ("Bora lá") com controle de lotação.
-- Rodar no Supabase -> SQL Editor -> New query -> Run. Pode rodar mais de uma vez.

-- A tela de detalhe precisa de descrição e organizador. Colunas novas só no fim da view.
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
  m.creator_id
from public.matches m
join public.venues v on v.id = m.venue_id;

grant select on public.matches_feed to authenticated;

-- ---------------------------------------------------------------- join_match
create or replace function public.join_match(p_match_id uuid)
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

  insert into public.participants (match_id, user_id) values (p_match_id, v_uid);

  if v_taken + 1 >= v_match.capacity then
    update public.matches set status = 'full' where id = p_match_id;
  end if;
end;
$$;

revoke all on function public.join_match(uuid) from public, anon;
grant execute on function public.join_match(uuid) to authenticated;

-- ---------------------------------------------------------------- leave_match
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
  if v_match.creator_id = v_uid then
    raise exception 'O organizador não pode sair do próprio jogo';
  end if;
  if v_match.status in ('cancelled', 'finished') or v_match.ends_at <= now() then
    raise exception 'Esse jogo já terminou';
  end if;

  delete from public.participants where match_id = p_match_id and user_id = v_uid;
  if not found then
    raise exception 'Você não está nesse jogo';
  end if;

  -- Abriu uma vaga: um jogo lotado volta a aparecer como aberto.
  if v_match.status = 'full' then
    update public.matches set status = 'open' where id = p_match_id;
  end if;
end;
$$;

revoke all on function public.leave_match(uuid) from public, anon;
grant execute on function public.leave_match(uuid) to authenticated;
