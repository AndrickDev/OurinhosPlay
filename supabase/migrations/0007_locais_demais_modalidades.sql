-- Demais locais de esporte de Ourinhos (fonte: ourinhos_esportes.md).
-- Modalidades ainda sem suporte no app ficam guardadas na coluna sports (tenis, padel, natacao...)
-- e aparecem quando o esporte for liberado. Locais sem esporte suportado não entram na criação de jogo.
-- Rodar no Supabase -> SQL Editor -> colar tudo -> Run. Pode rodar mais de uma vez.
-- Confirmar valores/horários antes de divulgar.

insert into public.venues (name, kind, address, sports) values
  ('Ourinhos Tênis Clube', 'private',
   'Rua Professor Francisco Dias Negrão, 1240, Ourinhos - SP. Tel (14) 99866-1126. 2 quadras rápidas, 2 de saibro, 2 de padel, 5 piscinas (2 de biribol). Clube privado.',
   array['tenis', 'padel', 'natacao', 'biribol', 'academia']),
  ('Casa de Esportes São Luiz', 'private',
   'Rua Paraná, 490, Centro, Ourinhos - SP. Tel (14) 3322-2342. Tênis; preço não publicado.',
   array['tenis']),
  ('Espaço Esportivo', 'private',
   'Rua 9 de Julho, 274, Centro, Ourinhos - SP. Tel (14) 3322-1393. Tênis; preço não publicado.',
   array['tenis']),
  ('Piscinas do Monstrinho', 'public',
   'Rua dos Expedicionários, 1549, Jardim Matilde, Ourinhos - SP. 2 piscinas (1 semiolímpica). Atividades municipais gratuitas.',
   array['natacao']),
  ('Pista de atletismo do Monstrinho', 'public',
   'Rua dos Expedicionários, 1549, Jardim Matilde, Ourinhos - SP. Pista municipal.',
   array['atletismo', 'corrida']),
  ('Academia do Monstrinho', 'public',
   'Rua dos Expedicionários, 1549, Jardim Matilde, Ourinhos - SP. Academia municipal.',
   array['academia']),
  ('SEMEC - Secretaria de Esportes e Lazer', 'public',
   'Rua dos Expedicionários, 1549, Jardim Matilde, Ourinhos - SP. Tel (14) 3302-4220. 38 modalidades (judô, capoeira, natação, futsal, vôlei, basquete, tênis, atletismo e paralímpicas).',
   array['judo', 'capoeira', 'natacao', 'futsal', 'volei', 'basquete', 'tenis', 'atletismo']),
  ('Parque Ecológico Tânia Mara Netto Silva', 'public',
   'Junto ao complexo da FAPI, Ourinhos - SP. Trilhas, área verde e lago. Gratuito.',
   array['corrida', 'caminhada', 'ciclismo']),
  ('Parque Olavo Ferreira de Sá', 'public',
   'Próximo à Vila Christoni, Ourinhos - SP. Área de lazer e caminhada. Gratuito.',
   array['corrida', 'caminhada']),
  ('Orla do Rio Paranapanema', 'public',
   'Margens do Paranapanema, Ourinhos - SP. Caminhada, corrida e atividades ao ar livre. Gratuito.',
   array['corrida', 'caminhada', 'ciclismo']),
  ('Ciclovias e ciclofaixas urbanas', 'public',
   'Avenidas e áreas de lazer de Ourinhos - SP. Gratuito.',
   array['ciclismo', 'corrida'])
on conflict (lower(name)) do update
  set address = excluded.address,
      sports  = excluded.sports,
      kind    = excluded.kind;

-- Futsal e jogos de quadra do Monstrinho/SESI aparecem hoje como futebol e vôlei/basquete.
update public.venues
   set sports = array['futebol', 'volei', 'basquete', 'futsal']
 where lower(name) = lower('Ginásio do Monstrinho');
