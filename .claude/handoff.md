# Handoff (20/10/2026, madrugada)

## Onde paramos
Cartão novo + interruptores de equipamento por esporte, prontos e instalados no emulador `ourinhos_pixel` (testes JUnit passam, app compila). Entrevista com 3 donos é **quarta 21/10**; nada nível 3 (dono, Pix, destaque) antes dela.

## Feito (commit local, sem push)
- `data/Options.kt`: catálogo de interruptores por modalidade (bola, colete, rede, raquete, bolinha, água).
- `supabase/migrations/0018_opcoes_jogo.sql`: `matches.options`, `matches_feed.options`, RPC `set_match_options`. **NÃO rodada no Supabase.** Sem ela o app funciona, só não grava/mostra as opções.
- `CreateMatch.kt`: "Mais opções" > "O que vai ter" (interruptor verde/cinza; mexeu em um, os outros da modalidade valem "não").
- `Components.kt`: `MatchTicket` redesenhado (capa com foto, horário placar, selo, preço/pessoa, pontinhos, estado) e `OptionSwitch`.
- `MatchDetail.kt`: resumo de equipamento. PRD seção 15.

## Retomar aqui (ordem)
1. Usuário roda no SQL Editor: 0016 (confirmar), 0017 e 0018.
2. Criar jogo novo com interruptor ligado e conferir os pontinhos no cartão. Apagar o jogo de teste "Pelada de terca" (criado antes da 0018).
3. Avaliar o design no celular real (Galaxy S21 FE) e ajustar cartão se preciso. Interruptores antigos (laranja) vs novos (verde): decidir se uniformiza.
4. Depois da entrevista (21/10): se >=2 de 3 donos aceitarem piloto, começar modo dono (`owner_id`, RLS, perfil público, destaque pago). Senão, não construir.
5. Pendentes: rostos de quem vai no cartão (precisa participantes no feed), push/Firebase, Crashlytics, LGPD, tela de métricas no Painel admin (usa 0017), trocar fotos com marca (futsal, biribol) antes da Play Store.

## Decisões vigentes
- Só Ourinhos por ora. Modo dono dentro do mesmo app. WhatsApp só como entrada.
- Usuário final vê só o que importa; personalização fica atrás de "Mais opções".
- Commit/push só quando o usuário pedir; commits sem Co-Authored-By. Push ainda não autorizado.
