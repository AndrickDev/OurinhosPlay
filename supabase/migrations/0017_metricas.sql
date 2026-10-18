-- Métricas de tração (só admin). Contagens agregadas, sem dados pessoais.
create or replace function public.admin_metrics()
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
begin
  if not public.is_admin() then
    raise exception 'Apenas administradores';
  end if;
  return jsonb_build_object(
    'jogadores', (select count(*) from profiles),
    'jogadores_novos_30d', (select count(*) from profiles where created_at > now() - interval '30 days'),
    'jogos_total', (select count(*) from matches),
    'jogos_30d', (select count(*) from matches where created_at > now() - interval '30 days'),
    'jogos_cancelados', (select count(*) from matches where status = 'cancelled'),
    'ativos_30d', (select count(distinct user_id) from participants where joined_at > now() - interval '30 days'),
    'vagas_preenchidas_30d', (select count(*) from participants where joined_at > now() - interval '30 days'),
    'por_esporte', coalesce((select jsonb_object_agg(sport, n) from (select sport, count(*) n from matches group by sport) s), '{}'::jsonb),
    'por_local', coalesce((select jsonb_agg(jsonb_build_object('local', v.name, 'jogos', n) order by n desc)
                           from (select venue_id, count(*) n from matches group by venue_id order by n desc limit 10) m
                           join venues v on v.id = m.venue_id), '[]'::jsonb)
  );
end;
$$;

revoke all on function public.admin_metrics() from public, anon;
grant execute on function public.admin_metrics() to authenticated;
