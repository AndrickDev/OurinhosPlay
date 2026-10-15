-- Painel admin: ver pessoas (nome + e-mail) e remover conta de vez.
-- Rodar no Supabase -> SQL Editor -> New query -> colar tudo -> Run. Pode rodar mais de uma vez.

create or replace function public.admin_list_users()
returns table (
  id uuid,
  display_name text,
  tag smallint,
  email text,
  role text,
  created_at timestamptz
)
language plpgsql
stable
security definer
set search_path = public, auth
as $$
begin
  if not public.is_admin() then
    raise exception 'Só administradores podem ver as pessoas';
  end if;
  return query
    select p.id, p.display_name, p.tag, u.email::text, p.role, p.created_at
      from public.profiles p
      join auth.users u on u.id = p.id
     order by p.created_at desc;
end;
$$;

-- Remove a conta de vez: login, perfil, participações e jogos criados pela pessoa (cascade).
create or replace function public.admin_delete_user(p_user_id uuid)
returns void
language plpgsql
security definer
set search_path = public, auth
as $$
begin
  if not public.is_admin() then
    raise exception 'Só administradores podem remover contas';
  end if;
  if p_user_id = auth.uid() then
    raise exception 'Você não pode remover a própria conta por aqui';
  end if;
  if exists (select 1 from public.profiles where id = p_user_id and role = 'admin') then
    raise exception 'Não é possível remover outro administrador';
  end if;
  delete from auth.users where id = p_user_id;
end;
$$;

revoke all on function public.admin_list_users() from public, anon;
revoke all on function public.admin_delete_user(uuid) from public, anon;
grant execute on function public.admin_list_users() to authenticated;
grant execute on function public.admin_delete_user(uuid) to authenticated;
