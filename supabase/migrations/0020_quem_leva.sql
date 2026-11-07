-- "Eu levo": quem da lista assume levar cada equipamento do jogo (bola, colete, rede...).
-- Um responsável por item. Só quem está na lista vê e assume. Opcional: jogo sem isso continua igual.

create table if not exists public.match_gear (
  match_id uuid not null references public.matches(id) on delete cascade,
  item text not null check (item in ('bola','colete','rede','raquete','bolinha','agua')),
  user_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (match_id, item)
);

alter table public.match_gear enable row level security;

create or replace function public.is_match_member(p_match_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.participants p
     where p.match_id = p_match_id and p.user_id = auth.uid()
  );
$$;

revoke all on function public.is_match_member(uuid) from public, anon;
grant execute on function public.is_match_member(uuid) to authenticated;

drop policy if exists "match_gear_select" on public.match_gear;
create policy "match_gear_select" on public.match_gear
  for select to authenticated using (public.is_match_member(match_id));

revoke insert, update, delete on public.match_gear from authenticated, anon;
grant select on public.match_gear to authenticated;

-- p_take = true assume o item (se ninguém assumiu); false solta (só quem assumiu).
create or replace function public.claim_gear(p_match_id uuid, p_item text, p_take boolean)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_owner uuid;
begin
  if auth.uid() is null then
    raise exception 'Não autenticado';
  end if;
  if not public.is_match_member(p_match_id) then
    raise exception 'Entre no jogo para levar algo';
  end if;
  if not exists (select 1 from public.matches m where m.id = p_match_id and m.ends_at > now() and m.status not in ('cancelled', 'finished')) then
    raise exception 'Esse jogo já acabou';
  end if;

  select user_id into v_owner from public.match_gear where match_id = p_match_id and item = p_item;

  if p_take then
    if v_owner is not null and v_owner <> auth.uid() then
      raise exception 'Alguém já vai levar';
    end if;
    insert into public.match_gear (match_id, item, user_id) values (p_match_id, p_item, auth.uid())
    on conflict (match_id, item) do nothing;
    -- corrida: outra pessoa assumiu entre a leitura e o insert
    if not found and not exists (select 1 from public.match_gear where match_id = p_match_id and item = p_item and user_id = auth.uid()) then
      raise exception 'Alguém já vai levar';
    end if;
  else
    delete from public.match_gear where match_id = p_match_id and item = p_item and user_id = auth.uid();
  end if;
end;
$$;

revoke all on function public.claim_gear(uuid, text, boolean) from public, anon;
grant execute on function public.claim_gear(uuid, text, boolean) to authenticated;

-- Quem sai do jogo deixa de levar.
create or replace function public.release_gear_on_leave()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  delete from public.match_gear where match_id = old.match_id and user_id = old.user_id;
  return old;
end;
$$;

drop trigger if exists participants_release_gear on public.participants;
create trigger participants_release_gear
  after delete on public.participants
  for each row execute function public.release_gear_on_leave();
