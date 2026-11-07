-- set_match_options passa a aceitar só {"chave": true|false}, até 30 chaves curtas.
-- Motivo: o app lê options como Map<String, Boolean>; um valor de outro tipo quebrava o feed de todos.
-- Rodar: npx supabase db query --remote --yes -f supabase/migrations/0021_opcoes_so_booleanas.sql

create or replace function public.set_match_options(p_match_id uuid, p_options jsonb)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if p_options is null or jsonb_typeof(p_options) <> 'object' then
    raise exception 'Opções inválidas';
  end if;
  if (select count(*) from jsonb_object_keys(p_options)) > 30
     or exists (
       select 1 from jsonb_each(p_options) e
        where jsonb_typeof(e.value) <> 'boolean' or length(e.key) not between 1 and 40
     ) then
    raise exception 'Opções inválidas';
  end if;
  update public.matches set options = p_options
   where id = p_match_id and creator_id = auth.uid();
  if not found then
    raise exception 'Jogo não encontrado';
  end if;
end;
$$;

revoke all on function public.set_match_options(uuid, jsonb) from public, anon;
grant execute on function public.set_match_options(uuid, jsonb) to authenticated;

-- Limpa o que já estiver gravado fora do formato (mantém só valores booleanos).
update public.matches m
   set options = coalesce((
     select jsonb_object_agg(e.key, e.value) from jsonb_each(m.options) e where jsonb_typeof(e.value) = 'boolean'
   ), '{}'::jsonb)
 where exists (select 1 from jsonb_each(m.options) e where jsonb_typeof(e.value) <> 'boolean');
