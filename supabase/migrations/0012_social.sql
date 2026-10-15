-- Social: "Cheguei" (visto), chat rápido temporário com frases prontas, 1 foto por pessoa
-- e amigos. Tudo some depois do jogo (6 horas após o fim).
-- Rodar no Supabase -> SQL Editor -> New query -> colar tudo -> Run. Pode rodar mais de uma vez.

-- ---------------------------------------------------------------- cheguei
alter table public.participants add column if not exists arrived_at timestamptz;

create or replace function public.check_in(p_match_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_match public.matches;
begin
  if auth.uid() is null then
    raise exception 'Não autenticado';
  end if;
  select * into v_match from public.matches where id = p_match_id;
  if not found then
    raise exception 'Jogo não encontrado';
  end if;
  if v_match.status = 'cancelled' then
    raise exception 'Esse jogo foi cancelado';
  end if;
  if not exists (select 1 from public.participants where match_id = p_match_id and user_id = auth.uid()) then
    raise exception 'Só quem está na lista pode avisar que chegou';
  end if;
  if now() < v_match.starts_at - interval '60 minutes' then
    raise exception 'Ainda é cedo: o "Cheguei" abre 1 hora antes do jogo';
  end if;
  if now() > v_match.ends_at then
    raise exception 'Esse jogo já terminou';
  end if;
  update public.participants
     set arrived_at = coalesce(arrived_at, now())
   where match_id = p_match_id and user_id = auth.uid();
end;
$$;

revoke all on function public.check_in(uuid) from public, anon;
grant execute on function public.check_in(uuid) to authenticated;

-- ---------------------------------------------------------------- chat rápido
create table if not exists public.match_messages (
  id uuid primary key default gen_random_uuid(),
  match_id uuid not null references public.matches(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  phrase text not null check (length(phrase) between 1 and 30),
  created_at timestamptz not null default now()
);
create index if not exists match_messages_match_idx on public.match_messages (match_id, created_at);

create table if not exists public.match_photos (
  match_id uuid not null references public.matches(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  data text not null check (length(data) <= 400000),   -- JPEG pequeno em base64
  created_at timestamptz not null default now(),
  primary key (match_id, user_id)
);

alter table public.match_messages enable row level security;
alter table public.match_photos enable row level security;

-- Só quem está na lista lê, e só enquanto a conversa não expirou (fim do jogo + 6 h).
create or replace function public.can_read_chat(p_match_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
      from public.matches m
      join public.participants p on p.match_id = m.id and p.user_id = auth.uid()
     where m.id = p_match_id
       and m.ends_at + interval '6 hours' > now()
  );
$$;

revoke all on function public.can_read_chat(uuid) from public, anon;
grant execute on function public.can_read_chat(uuid) to authenticated;

drop policy if exists "match_messages_select" on public.match_messages;
create policy "match_messages_select" on public.match_messages
  for select to authenticated using (public.can_read_chat(match_id));

drop policy if exists "match_photos_select" on public.match_photos;
create policy "match_photos_select" on public.match_photos
  for select to authenticated using (public.can_read_chat(match_id));

revoke insert, update, delete on public.match_messages from authenticated, anon;
revoke insert, update, delete on public.match_photos from authenticated, anon;
grant select on public.match_messages to authenticated;
grant select on public.match_photos to authenticated;

-- Limpeza: roda junto de cada envio (barato) e some com tudo que expirou.
create or replace function public.purge_expired_chat()
returns void
language sql
security definer
set search_path = public
as $$
  delete from public.match_messages mm
   using public.matches m
   where m.id = mm.match_id and m.ends_at + interval '6 hours' < now();
  delete from public.match_photos mp
   using public.matches m
   where m.id = mp.match_id and m.ends_at + interval '6 hours' < now();
$$;

create or replace function public.send_phrase(p_match_id uuid, p_phrase text)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_n int;
begin
  if auth.uid() is null then
    raise exception 'Não autenticado';
  end if;
  if not public.can_read_chat(p_match_id) then
    raise exception 'O chat desse jogo não está disponível';
  end if;
  select count(*) into v_n from public.match_messages where match_id = p_match_id and user_id = auth.uid();
  if v_n >= 30 then
    raise exception 'Muitas mensagens nesse jogo';
  end if;
  insert into public.match_messages (match_id, user_id, phrase) values (p_match_id, auth.uid(), p_phrase);
  perform public.purge_expired_chat();
end;
$$;

create or replace function public.send_photo(p_match_id uuid, p_data text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'Não autenticado';
  end if;
  if not public.can_read_chat(p_match_id) then
    raise exception 'O chat desse jogo não está disponível';
  end if;
  if p_data is null or length(p_data) > 400000 then
    raise exception 'Foto grande demais';
  end if;
  insert into public.match_photos (match_id, user_id, data) values (p_match_id, auth.uid(), p_data)
  on conflict (match_id, user_id) do update set data = excluded.data, created_at = now();
  perform public.purge_expired_chat();
end;
$$;

revoke all on function public.send_phrase(uuid, text) from public, anon;
revoke all on function public.send_photo(uuid, text) from public, anon;
revoke all on function public.purge_expired_chat() from public, anon, authenticated;
grant execute on function public.send_phrase(uuid, text) to authenticated;
grant execute on function public.send_photo(uuid, text) to authenticated;

-- ---------------------------------------------------------------- amigos
create table if not exists public.friendships (
  user_id uuid not null references public.profiles(id) on delete cascade,
  friend_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, friend_id),
  check (user_id <> friend_id)
);

alter table public.friendships enable row level security;

drop policy if exists "friendships_select_own" on public.friendships;
create policy "friendships_select_own" on public.friendships
  for select to authenticated using (user_id = auth.uid());

revoke insert, update, delete on public.friendships from authenticated, anon;
grant select on public.friendships to authenticated;

-- Adiciona pelo nome e número (ex.: Joao#0042). Devolve o id do amigo.
create or replace function public.add_friend(p_name text, p_tag int)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_friend uuid;
begin
  if auth.uid() is null then
    raise exception 'Não autenticado';
  end if;
  select id into v_friend from public.profiles
   where lower(display_name) = lower(btrim(p_name)) and tag = p_tag;
  if v_friend is null then
    raise exception 'Não achei ninguém com esse nome e número';
  end if;
  if v_friend = auth.uid() then
    raise exception 'Esse é você mesmo';
  end if;
  insert into public.friendships (user_id, friend_id) values (auth.uid(), v_friend)
  on conflict do nothing;
  return v_friend;
end;
$$;

create or replace function public.remove_friend(p_friend_id uuid)
returns void
language sql
security definer
set search_path = public
as $$
  delete from public.friendships where user_id = auth.uid() and friend_id = p_friend_id;
$$;

create or replace function public.my_friends()
returns table (id uuid, display_name text, tag smallint)
language sql
stable
security definer
set search_path = public
as $$
  select p.id, p.display_name, p.tag
    from public.friendships f
    join public.profiles p on p.id = f.friend_id
   where f.user_id = auth.uid()
   order by lower(p.display_name);
$$;

revoke all on function public.add_friend(text, int) from public, anon;
revoke all on function public.remove_friend(uuid) from public, anon;
revoke all on function public.my_friends() from public, anon;
grant execute on function public.add_friend(text, int) to authenticated;
grant execute on function public.remove_friend(uuid) to authenticated;
grant execute on function public.my_friends() to authenticated;
