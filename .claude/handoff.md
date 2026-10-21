# Handoff (20/10/2026, noite)

## Estado
Entrevista com 3 donos: **quarta 21/10**. Nada nível 3 do modo dono antes dela (>=2 de 3 aceitarem piloto = começa; senão não).
Commit local `3e4c4aa` + mudanças **não commitadas** (sem push, sem commit: usuário não pediu).

## Feito hoje (não commitado)
- Supabase por CLI: projeto vinculado, histórico 0001-0018 aplicado e sincronizado (`npx supabase migration list|db push|db query --linked`). Jogo de teste "Pelada de terca" apagado.
- Painel admin > aba **Métricas** (`ui/AdminMetricsTab.kt`, `data/Metrics.kt`), testada no emulador com dados reais.
- **Rostos** (iniciais, até 4 + "+N") no cartão: `Faces` em `Components.kt`, coluna `faces` no feed.
- **Exclusão de conta**: Perfil > "Excluir minha conta" > RPC `delete_my_account`; erro aparece no Perfil. Bloqueia admin e quem tem jogo futuro com outras pessoas.
- **Versão** no rodapé do Perfil. Testes: `FacesTest`, `MetricsTest`. Lint 0 erros.
- Docs: PRD seção 16.

## Pendente
1. ~~0019~~ aplicada em produção (20/10). Falta testar no emulador: rostos no cartão e fluxo de exclusão com conta de teste (conta admin é bloqueada).
2. Testar no emulador: criar jogo com interruptor ligado (pontinhos), rostos após a 0019, fluxo de exclusão com conta de teste.
3. Avaliar design no S21 FE (ícones novos em SportIcons.kt, estado vazio, botão Entrar); decidir se os 7 interruptores Material (CreateMatch.kt:426,488,585; Profile.kt:161; AdminScreen.kt:245,468,660) ficam verdes como `OptionSwitch`.
4. Firebase/Crashlytics/push (usuário quer configurar depois), política de privacidade e consentimento de localização, fotos com marca (futsal, biribol) antes da Play Store.
5. Ícones repetidos resolvidos; tênis ainda usa o do Material.
