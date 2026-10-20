-- Opções por jogo (interruptores por esporte: tem bola, tem rede...). Ex.: {"bola": true, "rede": false}.
-- Opcional: jogo sem opções continua igual. Rodar no SQL Editor do Supabase.

alter table public.matches add column if not exists options jsonb not null default '{}'::jsonb;

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
  m.options
from public.matches m
join public.venues v on v.id = m.venue_id
left join public.courts c on c.id = m.court_id;

grant select on public.matches_feed to authenticated;

-- Só o criador define as opções do próprio jogo.
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
  update public.matches set options = p_options
   where id = p_match_id and creator_id = auth.uid();
  if not found then
    raise exception 'Jogo não encontrado';
  end if;
end;
$$;

revoke all on function public.set_match_options(uuid, jsonb) from public, anon;
grant execute on function public.set_match_options(uuid, jsonb) to authenticated;
