-- Apaga os jogos "[Teste] ..." e os jogadores falsos criados por jogo_rolando.sql.
-- Rodar: npx supabase db query --remote --yes -f supabase/testes/limpar_testes.sql
delete from public.matches where title like '[Teste] %';
delete from auth.users where email like '%@teste.ourinhosplay.invalid';
select (select count(*) from public.matches where title like '[Teste] %') as jogos_teste,
       (select count(*) from auth.users where email like '%@teste.ourinhosplay.invalid') as usuarios_teste;
