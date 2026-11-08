-- DADOS DE TESTE (não é migration). Cria 5 jogadores falsos e um jogo de futebol
-- "[Teste] Jogo rolando" que começou há 5 min, com você (primeiro admin) + os 5 na lista,
-- 1 goleiro, e o jogador "Teste Bruno" levando a bola; e "[Teste] Só organizo" (amanhã, você
-- criou mas não está na lista). Serve para ver sorteio de times, Agenda,
-- cronômetro, placar e "Eu levo" sem precisar de outras pessoas.
-- Rodar: npx supabase db query --remote --yes -f supabase/testes/jogo_rolando.sql
-- Apagar tudo: supabase/testes/limpar_testes.sql
do $$
declare
  v_me    uuid;
  v_court public.courts;
  v_match uuid;
  v_names text[] := array['Teste Ana', 'Teste Bruno', 'Teste Caio', 'Teste Duda', 'Teste Enzo'];
  v_ids   uuid[] := '{}';
  v_id    uuid;
  i int;
begin
  select id into v_me from public.profiles where role = 'admin' order by created_at limit 1;
  if v_me is null then raise exception 'Nenhum admin'; end if;

  delete from public.matches where title like '[Teste] %';

  for i in 1..array_length(v_names, 1) loop
    select id into v_id from auth.users where email = 'teste' || i || '@teste.ourinhosplay.invalid';
    if v_id is null then
      v_id := gen_random_uuid();
      insert into auth.users (id, instance_id, aud, role, email, raw_app_meta_data, raw_user_meta_data, created_at, updated_at)
      values (v_id, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated',
              'teste' || i || '@teste.ourinhosplay.invalid', '{}', '{}', now(), now());
    end if;
    update public.profiles set display_name = v_names[i], tag = 9000 + i where id = v_id;
    v_ids := v_ids || v_id;
  end loop;

  -- quadra de futebol livre agora (anti-choque)
  select c.* into v_court from public.courts c join public.venues v on v.id = c.venue_id
   where 'futebol' = any(v.sports)
     and not exists (select 1 from public.matches m
                      where coalesce(m.court_id, m.venue_id) = c.id
                        and m.status <> 'cancelled'
                        and tstzrange(m.starts_at, m.ends_at) && tstzrange(now() - interval '10 minutes', now() + interval '90 minutes'))
   order by c.created_at limit 1;
  if v_court.id is null then raise exception 'Sem quadra de futebol livre agora'; end if;

  insert into public.matches (venue_id, court_id, creator_id, sport, title, starts_at, ends_at, capacity, options)
  values (v_court.venue_id, v_court.id, v_me, 'futebol', '[Teste] Jogo rolando',
          date_trunc('minute', now()) - interval '5 minutes', date_trunc('minute', now()) + interval '85 minutes', 10,
          '{"bola": true, "colete": true}')
  returning id into v_match;

  insert into public.match_roles (match_id, role, slots, reserved, release_hours, perk_pct)
  values (v_match, 'goleiro', 2, true, 2, 0);

  insert into public.participants (match_id, user_id) values (v_match, v_me);
  insert into public.participants (match_id, user_id, role) values (v_match, v_ids[1], 'goleiro');
  for i in 2..array_length(v_ids, 1) loop
    insert into public.participants (match_id, user_id) values (v_match, v_ids[i]);
  end loop;

  insert into public.match_gear (match_id, item, user_id) values (v_match, 'bola', v_ids[2]);

  -- Jogo amanhã que você criou mas NÃO está na lista (Agenda deve mostrar "Você organiza").
  insert into public.matches (venue_id, court_id, creator_id, sport, title, starts_at, ends_at, capacity)
  values (v_court.venue_id, v_court.id, v_me, 'futebol', '[Teste] Só organizo',
          date_trunc('hour', now()) + interval '1 day', date_trunc('hour', now()) + interval '1 day 90 minutes', 10)
  returning id into v_match;
  insert into public.participants (match_id, user_id) values (v_match, v_ids[3]);
end $$;

select m.id, m.title, m.starts_at, m.ends_at, (select count(*) from public.participants p where p.match_id = m.id) as na_lista
  from public.matches m where m.title like '[Teste] %' order by m.starts_at;
