-- Sprint 1: profiles + trigger + RLS + nome com tag (#0000-#9999).
-- Rodar no Supabase -> SQL Editor -> New query -> Run. Pode rodar mais de uma vez.

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text,
  tag smallint check (tag between 0 and 9999),
  role text not null default 'user' check (role in ('user', 'admin')),
  created_at timestamptz not null default now()
);

-- Caso a tabela já exista de uma execução anterior.
alter table public.profiles add column if not exists tag smallint
  check (tag between 0 and 9999);

-- Nome + tag é único (sem diferenciar maiúsculas): "Andrick#0042".
create unique index if not exists profiles_name_tag_uniq
  on public.profiles (lower(display_name), tag)
  where display_name is not null and tag is not null;

-- Cria o perfil automaticamente quando um usuário se cadastra.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id) values (new.id)
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- Perfis para usuários que já existiam antes do trigger.
insert into public.profiles (id)
select id from auth.users
on conflict (id) do nothing;

-- Define o nome (uma única vez) e sorteia uma tag livre para ele.
create or replace function public.set_display_name(p_name text)
returns public.profiles
language plpgsql
security definer
set search_path = public
as $$
declare
  v_uid uuid := auth.uid();
  v_name text := btrim(p_name);
  v_row public.profiles;
  v_tag smallint;
  v_try int := 0;
begin
  if v_uid is null then
    raise exception 'Não autenticado';
  end if;
  if length(v_name) < 2 or length(v_name) > 30 then
    raise exception 'O nome precisa ter de 2 a 30 caracteres';
  end if;

  select * into v_row from public.profiles where id = v_uid for update;
  if not found then
    insert into public.profiles (id) values (v_uid) returning * into v_row;
  end if;
  if v_row.display_name is not null then
    raise exception 'Nome já definido';
  end if;

  loop
    v_try := v_try + 1;
    v_tag := floor(random() * 10000)::smallint;
    begin
      update public.profiles
         set display_name = v_name, tag = v_tag
       where id = v_uid
      returning * into v_row;
      return v_row;
    exception when unique_violation then
      if v_try >= 200 then
        raise exception 'Sem tags livres para esse nome, escolha outro';
      end if;
    end;
  end loop;
end;
$$;

revoke all on function public.set_display_name(text) from public, anon;
grant execute on function public.set_display_name(text) to authenticated;

alter table public.profiles enable row level security;

-- Nome aparece para outros jogadores: qualquer logado lê.
drop policy if exists "profiles_select_authenticated" on public.profiles;
create policy "profiles_select_authenticated"
  on public.profiles for select to authenticated using (true);

-- Cliente não escreve direto em profiles (nome só via set_display_name,
-- role só pelo painel/admin). Remove políticas antigas de update.
drop policy if exists "profiles_update_own" on public.profiles;
revoke insert, update, delete on public.profiles from authenticated, anon;
