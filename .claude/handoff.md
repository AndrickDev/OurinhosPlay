# Handoff (07/11/2026, tarde)

## Estado
`main` = `origin/main` (607557e) + mudanças **não commitadas** desta rodada. Testes 51/51, lint 0 erros, app instalado no emulador (deslogado).

## Feito (sem commit)
- Relógio monotônico no cronômetro (`clockNow()` em `ui/ClockService.kt`).
- Agenda: "Você organiza" para criador fora da lista (`ticketAction`, `TicketActionTest`).
- `supabase/testes/jogo_rolando.sql` + `limpar_testes.sql`.
- `docs/TESTES-PENDENTES.md` (spec de testes), `docs/POLITICA-PRIVACIDADE.md`, `docs/PLAY-STORE.md`, PRD §17 atualizado.

## Bloqueado
- ~~Seed~~ rodado pelo usuário 07/11; testes A1-A5, B1-B4, B7, B8, C1-C3, D1 passaram no emulador (ver TESTES-PENDENTES.md). Dados de teste apagados 07/11 (limpar_testes.sql: 0 e 0).
- Faltam A6, B6, B9, C4, D2, D3, E1, E2.

## Próximo
1. Usuário: commit? Rodar o seed, logar no emulador, seguir `docs/TESTES-PENDENTES.md` (A-E).
2. Decidir foto de futsal (bola com desenho de marca); biribol sem marca visível.
3. Firebase/push precisa do projeto Firebase do usuário (google-services.json).
4. Roadmap "Agora": lista de espera por função.
