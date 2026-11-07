# Handoff (07/11/2026)

## Estado
Rodada "dia do jogo e comunidade" pronta: 7 commits locais `4e09477..85c1527` (sem push; `main` está 19 à frente de `origin/main`). Testes 39/39, lint 0 erros, conferido no emulador `ourinhos_pixel`. Detalhes em `docs/PRD.md` seção 17.

## Feito
- "Eu levo" (0020, `match_gear`, `claim_gear`), cronômetro com serviço em primeiro plano e apito de alarme, selo Universitário, Agenda "Você vai", abrir o jogo após publicar, roteiro de teste (`docs/TESTE-USABILIDADE.md`) e roadmap curto (PRD §17).
- Revisão (`revisor`) feita uma vez; achados médios corrigidos em `85c1527`.

## Pendente (ordem)
1. **Rodar `supabase/migrations/0020_quem_leva.sql` no SQL Editor** (CLI deu timeout no login role em 07/11). Sem ela "Eu levo" só esconde os botões.
2. Push dos commits (usuário autoriza).
3. Apagar jogos de teste de 07/11 (privados, terminam hoje): "Reserva · Futebol" 18:30 Arena Society, "Teste Uni" 20:00 Arena90, "Reserva · Futebol" 20:00 Ginásio do Monstrinho.
4. Testar "Eu levo" com 2 contas depois da 0020.
5. Baixa prioridade (revisor): relógio monotônico no cronômetro; validar booleanos em `set_match_options` (0018: valor não booleano quebra o feed de todos); "Jogar de novo" não copia selo nem equipamento; criador que só organiza aparece com "Você vai" na Agenda.
6. Fazer o teste de usabilidade com 5 pessoas antes do beta público.
7. Antigos: Firebase/push, política de privacidade, fotos com marca (futsal, biribol), justificar FGS `specialUse` no Play Console.
