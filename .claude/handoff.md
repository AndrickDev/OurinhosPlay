# Handoff (10/11/2026)

## Estado
`main` 4 commits à frente de `origin/main` (c89b167..b9fe735, sem push) + mudanças **não commitadas** desta rodada. Testes 53/53, lint 0 erros, app instalado no emulador (logado, sons ligados). Dados de teste apagados (0 e 0).

## Feito nesta rodada (sem commit)
- Testes A6, B6, B9, C4, D2, D3, E1, E2 passaram (docs/TESTES-PENDENTES.md, seção 10/11).
- Fix: Histórico mostra "Encerrado" e sem "Falta goleiro" (`ticketAction(past)`, `MatchTicket(past)`).
- Fix: feed mostra "Você vai" em jogo em que você está (`MatchRepository.openMatches` + `MineRepository.joinedIds`).
- Texto de "Sons do app" (Profile.kt) cita a exceção do apito.
- PRD "Rodada de 10/11/2026".

## Próximo
1. Usuário autorizar: commit desta rodada + push dos 4 commits anteriores.
2. Observações: detalhe não recarrega ao reabrir (mostrar dado novo ao voltar); "Jogar de novo" copiar valor da quadra? (decisão); B5 som do apito só ouvindo no celular real.
3. F1-F3 (Play Store): preencher política de privacidade e publicar URL; formulário FGS + vídeo; decidir foto de futsal.
4. Firebase/push: precisa do google-services.json do usuário.
5. Roadmap "Agora": lista de espera por função; G1 teste com 5 pessoas.
