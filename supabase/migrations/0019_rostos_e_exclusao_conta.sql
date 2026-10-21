-- 0019: rostos (iniciais) no cartão do feed + exclusão da própria conta (LGPD).
-- Rodar via `npx supabase db push`.

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
  c.price_per_hour_cents as court_price_cents,
  m.is_event,
  m.promoted,
  m.route_start,
  m.route_end,
  m.cost_cents,
  case
    when m.creator_id = auth.uid()
      or exists (select 1 from public.participants p where p.match_id = m.id and p.user_id = auth.uid())
    then m.pix_key
  end as pix_key,
  m.is_private,
  m.level,
  m.looking_for,
  m.short_code,
  coalesce((
    select array_agg(r.role order by r.role)
      from public.match_roles r
     where r.match_id = m.id
       and r.slots > (select count(*) from public.participants p where p.match_id = r.match_id and p.role = r.role)
  ), '{}') as roles_open,
  m.options,
  coalesce((
    select array_agg(coalesce(nullif(upper(left(btrim(pr.display_name), 1)), ''), '?') order by p.joined_at, p.user_id)
      from (select * from public.participants p0 where p0.match_id = m.id order by p0.joined_at, p0.user_id limit 5) p
      join public.profiles pr on pr.id = p.user_id
  ), '{}') as faces
from public.matches m
join public.venues v on v.id = m.venue_id
left join public.courts c on c.id = m.court_id;

grant select on public.matches_feed to authenticated;

-- Exclui a conta de quem chama. Apaga auth.users; o cascade remove perfil, participações,
-- amigos e jogos criados por essa pessoa. Admin não se exclui por aqui (evita ficar sem admin).
-- Bloqueia se ela criou jogo futuro com outras pessoas (o cascade apagaria o jogo delas).
create or replace function public.delete_my_account()
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception 'Não autenticado';
  end if;
  if exists (select 1 from public.profiles where id = auth.uid() and role = 'admin') then
    raise exception 'Administradores não podem excluir a conta por aqui';
  end if;
  if exists (
    select 1 from public.matches m
     where m.creator_id = auth.uid() and m.status <> 'cancelled' and m.ends_at > now()
       and exists (select 1 from public.participants p where p.match_id = m.id and p.user_id <> auth.uid())
  ) then
    raise exception 'Você tem jogo futuro com outras pessoas. Cancele o jogo antes de excluir a conta.';
  end if;
  delete from auth.users where id = auth.uid();
end;
$$;

revoke all on function public.delete_my_account() from public, anon;
grant execute on function public.delete_my_account() to authenticated;
