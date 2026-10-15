-- DADOS DE TESTE (só para ver a tela cheia, rolagem e cores). Não é migration.
-- Antes: rodar 0006_locais_ourinhos.sql e ter definido seu nome no app.
-- Rodar no Supabase -> SQL Editor -> colar tudo -> Run. Pode rodar de novo: apaga os [Teste] antigos.
-- Para só apagar os testes, rode apenas a linha abaixo, sozinha:
--   delete from public.matches where title like '[Teste] %';

delete from public.matches where title like '[Teste] %';

do $$
declare
  v_creator uuid;
  v_venues  uuid[];
  v_n       int;
  v_venue   public.venues;
  v_sport   text;
  v_start   timestamptz;
  v_match   uuid;
  v_titles  text[] := array[
    'Pelada de quinta', 'Racha do bairro', 'Vôlei de areia', 'Basquete 3x3', 'Society dos amigos',
    'Futebol feminino', 'Treino aberto', 'Torneio relâmpago', 'Jogo da firma', 'Racha da madrugada',
    'Beach tennis duplas', 'Vôlei misto', 'Pelada da UNESP', 'Fut sete'
  ];
  i int;
begin
  select id into v_creator from public.profiles where display_name is not null order by created_at limit 1;
  if v_creator is null then
    raise exception 'Defina o nome no app antes de rodar o seed';
  end if;

  select array_agg(id order by name) into v_venues from public.venues;
  v_n := array_length(v_venues, 1);

  for i in 1..14 loop
    select * into v_venue from public.venues where id = v_venues[1 + (i % v_n)];
    v_sport := v_venue.sports[1 + (i % array_length(v_venue.sports, 1))];
    -- espaçados de 5 em 5 horas: nunca se sobrepõem, em nenhuma quadra
    v_start := date_trunc('hour', now()) + interval '2 hours' + i * interval '5 hours';

    insert into public.matches (venue_id, creator_id, sport, title, starts_at, ends_at, capacity)
    values (v_venue.id, v_creator, v_sport, '[Teste] ' || v_titles[i], v_start, v_start + interval '90 minutes', 6 + (i % 3) * 4)
    returning id into v_match;

    insert into public.participants (match_id, user_id)
    values (v_match, v_creator)
    on conflict do nothing;
  end loop;
end $$;
