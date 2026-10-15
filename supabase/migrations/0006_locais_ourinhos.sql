-- Locais reais de Ourinhos (fonte: ourinhos_esportes.md). Só modalidades que o app tem.
-- Rodar no Supabase -> SQL Editor -> New query -> colar tudo -> Run. Pode rodar mais de uma vez.
-- Valores e horários podem mudar: confirmar com o local antes de divulgar.

-- Remove os locais "exemplo" da etapa 2 (só os que ainda não têm jogo).
delete from public.venues v
where v.name in (
    'Quadra Poliesportiva (exemplo 1)',
    'Quadra de Areia (exemplo 2)',
    'Arena Society (exemplo 3)',
    'Vila Odilon - quadras e campo',
    'Cohab Padre Murant - quadra',
    'Jardim Santos Dumont - quadra',
    'UNESP Ourinhos - quadra'
  )
  and not exists (select 1 from public.matches m where m.venue_id = v.id);

insert into public.venues (name, kind, address, sports) values
  ('Orla Beach Tennis Ourinhos',
   'private', 'Ourinhos - SP (R$ 70/hora; raquetes e bolinhas emprestadas; sáb fechado) - @orlabeachtennisourinhos',
   array['beach_tennis', 'volei']),
  ('Ginásio do Monstrinho',
   'public', 'Rua dos Expedicionários, 1549, Jardim Matilde, Ourinhos - SP (atividades municipais gratuitas)',
   array['futebol', 'volei', 'basquete']),
  ('Estádio Djalma Bahia (Monstrinho)',
   'public', 'Rua dos Expedicionários, 1549, Jardim Matilde, Ourinhos - SP (campo de futebol)',
   array['futebol']),
  ('CAT SESI Ourinhos',
   'private', 'Rua Professora Maria José Ferreira, 100, Bairro das Crianças, Ourinhos - SP (carteirinha SESI)',
   array['volei', 'basquete']),
  ('Winner Arena Sports',
   'private', 'Ourinhos - SP (complexo esportivo; society e quadras; valores a confirmar)',
   array['futebol']),
  ('Arena90',
   'private', 'Ourinhos - SP (quadras de areia e society; valores a confirmar) - arena90.com.br',
   array['futebol', 'volei', 'beach_tennis'])
on conflict (lower(name)) do update
  set address = excluded.address,
      sports  = excluded.sports,
      kind    = excluded.kind;
