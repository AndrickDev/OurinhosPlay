# Handoff (07/11/2026)

## Estado
Rodada "dia do jogo e comunidade" pronta: 7 commits locais `4e09477..85c1527` (sem push; `main` está 19 à frente de `origin/main`). Testes 39/39, lint 0 erros, conferido no emulador `ourinhos_pixel`. Detalhes em `docs/PRD.md` seção 17.

## Feito
- "Eu levo" (0020, `match_gear`, `claim_gear`), cronômetro com serviço em primeiro plano e apito de alarme, selo Universitário, Agenda "Você vai", abrir o jogo após publicar, roteiro de teste (`docs/TESTE-USABILIDADE.md`) e roadmap curto (PRD §17).
- Revisão (`revisor`) feita uma vez; achados médios corrigidos em `85c1527`.

## Rodada da noite 07/11
Placar no cronômetro, sorteio de times, "Jogar de novo" copia selo/equipamento, 0021 (opções só booleanas). Commits eebaa33..8a5ae29, 47/47 testes, lint 0. Revisor 1x, achados aplicados. Não visto no emulador (sem jogo em andamento com 4+). **0021 pendente no banco; push pendente.**

## Pendente (ordem)
1. ~~0020~~ rodada 07/11 (via `npx supabase db query --remote --yes -f`; CLI logado e linkado).
2. ~~Push~~ feito 07/11 (b63f3a3 em origin/main).
3. ~~Jogos de teste de 07/11~~ apagados.
4. Testar "Eu levo" com 2 contas depois da 0020.
5. Baixa prioridade (revisor): relógio monotônico no cronômetro; validar booleanos em `set_match_options` (0018: valor não booleano quebra o feed de todos); "Jogar de novo" não copia selo nem equipamento; criador que só organiza aparece com "Você vai" na Agenda.
6. Fazer o teste de usabilidade com 5 pessoas antes do beta público.
7. Antigos: Firebase/push, política de privacidade, fotos com marca (futsal, biribol), justificar FGS `specialUse` no Play Console.
