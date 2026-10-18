# Handoff (18/10/2026, madrugada)

## Onde paramos
Revisão sênior de negócio + Fase 0 + preparação da entrevista com donos (quarta 21/10). App de debug instalado no Galaxy S21 FE (adb por rede), mas a tela estava bloqueada: **design ainda não conferido visualmente**.

## Feito (commits locais, sem push; último 9defd60)
- `docs/REVISAO-SENIOR-2026-10.md`: tese, concorrentes, decisões, tabela de pendências vs concorrentes.
- `docs/ENTREVISTA-DONOS.md`: roteiro v2 (sem induzir "sim"; preço por 4 faixas; compromisso = piloto 30 dias + agenda).
- Testes JUnit mínimos (`RolesTest`, 6 passam) e PRD atualizado (seção 14).
- `supabase/migrations/0017_metricas.sql` (função `admin_metrics()`, só admin): **escrita, NÃO rodada no Supabase**.

## Retomar aqui (ordem)
1. Usuário desbloqueia o celular → `adb exec-out screencap` da home, detalhe do jogo, criar jogo e Painel admin; avaliar design e pedir as queixas anteriores dele que ainda não ficaram certas.
2. Usuário roda no SQL Editor: 0016 (confirmar), 0017, e a query de contagem (jogadores, jogos, ativos 30d) para levar número à entrevista.
3. Tela de métricas dentro do Painel admin (decidido: no app, responsiva, sem web), chamando `admin_metrics()`. Nível 1-2.
4. Mockups de cartão e chat (aprovados), página no navegador.
5. Opcional: usuário roda `! npx rea-agents setup` (REA, MCP de engenharia reversa; repo morluto/rea). Uso só para ver telas/funções dos apps concorrentes; sem extrair endpoints/chaves privadas. Alternativa: instalar Appito, Agendei Quadras, Bora Pro Play e gravar tela.
6. Pedir demo/orçamento a 2 concorrentes (único preço achado: Atacante App a partir de R$199).

## Decisões vigentes
- Só Ourinhos por ora (laboratório). Modo dono dentro do mesmo app. WhatsApp só como entrada.
- Nada de nível 3 (dono, Pix, destaque) antes da entrevista; se <2 de 3 donos aceitarem piloto, não construir modo dono.
- Crashlytics bloqueado (precisa projeto Firebase do usuário).
- Commit/push só quando o usuário pedir; commits sem Co-Authored-By.
