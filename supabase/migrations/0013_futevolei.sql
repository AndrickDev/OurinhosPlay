-- Futevôlei como modalidade (fica ao lado do futebol, tema amarelo).
-- Rodar no Supabase -> SQL Editor -> New query -> colar tudo -> Run. Pode rodar mais de uma vez.

alter table public.matches drop constraint if exists matches_sport_check;
alter table public.matches
  add constraint matches_sport_check
  check (sport in (
    'futebol', 'futevolei', 'volei', 'basquete', 'beach_tennis',
    'futsal', 'tenis', 'padel', 'natacao', 'biribol', 'corrida', 'ciclismo', 'caminhada'
  ));

-- Locais de areia: Orla Beach Tennis e Arena90 (quadras de areia). Confirmar com o local.
update public.venues
   set sports = (select array(select distinct unnest(sports || array['futevolei'])))
 where lower(name) in (lower('Arena90'), lower('Orla Beach Tennis Ourinhos'));

update public.courts c
   set sports = (select array(select distinct unnest(c.sports || array['futevolei'])))
  from public.venues v
 where v.id = c.venue_id
   and lower(v.name) in (lower('Arena90'), lower('Orla Beach Tennis Ourinhos'));
