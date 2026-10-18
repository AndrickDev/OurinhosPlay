# Revisão sênior (18/10/2026)

Rascunho para decisão. Nada aqui foi implementado. Preços são hipóteses a validar com donos reais.

## Tese
Jogador é audiência; dono de quadra é cliente. O produto precisa dar ao dono o que ele não faz sozinho: agenda sem "bolo", sinal pago, horário ocioso preenchido e relatório. O fluxo de jogo atual é o motor de demanda.

## Diagnóstico
- Dono não tem conta: só existe `admin` global dado à mão (`AdminScreen.kt`). Sem multi-tenant, sem pagamento, sem perfil público da quadra, sem push.
- Ourinhos (~100 mil habitantes) não sustenta o negócio sozinha (ver teto abaixo). Serve de laboratório; o produto tem de ser replicável por cidade.
- Cartão do jogo (`Components.kt:195`): tudo com peso parecido; local e dia em cinza pequeno; sem preço por pessoa, sem rostos de quem vai, sem confiança no organizador; todos os cartões parecem iguais.
- Chat (`MatchExtras.kt:115`): só frases prontas e 1 foto por pessoa. Segura flood, mas empurra a conversa para o WhatsApp (vazamento do usuário).
- Higiene: PRD desatualizado (Sprint 6), 3 commits sem push, zero testes, sem Crashlytics, sem LGPD.

## Teto de receita em Ourinhos (conta de padaria, a validar)
Se houver ~40 quadras privadas e 20 aderirem ao Pro a R$ 99/mês: ~R$ 2 mil/mês. É validação, não negócio. O ganho real vem de repetir a receita em cidades parecidas (20-60 mil a 300 mil habitantes) com onboarding rápido.

## Manter o usuário dentro da plataforma
- WhatsApp só como canal de entrada (convite por link). Operação (reserva, aviso, chat, pagamento) dentro do app, senão a plataforma não cobra nada.
- Motivos para ficar: sinal pago e garantia da vaga, histórico e reputação (comparecimento), grupo fixo semanal, chat do jogo, lista de espera.
- Dono recebe tudo por push dentro do app; sem robô de WhatsApp.

## Chat proposto
Texto livre por jogo, só entre participantes, com: limite de ritmo (ex.: 1 msg/2 s), denunciar e silenciar, organizador pode fixar aviso e silenciar alguém, até 3 fotos por pessoa por jogo (comprimidas). Frases prontas ficam como atalhos. Some 6 h depois do jogo (mantido).

## Cartão proposto (a validar em mockup)
Linha 1: dia + horário em destaque. Linha 2: local e bairro. Linha 3: rostos de quem vai + vagas. Rodapé: preço por pessoa (ou "Grátis"), nível e um único estado/ação (Entrar, Lista de espera, Lotado). Quadra patrocinada leva selo discreto. Destaque visual só para o que exige ação.

## Dono dentro do app (modo dono, mesmo APK)
Reaproveita `AdminScreen`, filtrado por `owner_id`:
- Agenda da semana por quadra; aprovar/recusar pedidos; bloquear horário.
- Preço por faixa de horário (nobre/ocioso) e promoção de horário vazio.
- Receber sinal por Pix e ver repasses.
- Validar código do "Cheguei"/cupom na porta.
- Ocupação, receita e retorno de jogadores.
- Contratar destaque e ver resultado (visualizações, entradas).
- Push de reserva, cancelamento e no-show.
Portal web fica para depois (relatórios em desktop), só se donos pedirem. Não interfere no app do jogador: mesmo backend, papel diferente.

## Monetização
Dentro: plano Pro do dono (R$ 79-149/mês); destaque pago com selo (1-2 vagas por bairro); taxa 5-8% sobre sinal pago no app (Pix com split, sem custódia nossa); horário ocioso com taxa só se fechar; torneio com inscrição e patrocínio.
Fora: página pública + QR na quadra; grupo fixo mensalista; cupom de consumo com parceiros; relatório para patrocinadores; embaixador por cidade.
Ordem: Pro grátis no início para encher oferta → destaque → sinal Pix.

## Roadmap
| Fase | Conteúdo | Nível |
|---|---|---|
| 0 | Higiene: PRD, migrations 0015/0016, testes mínimos, Crashlytics, limpar dados de teste | 1 |
| 1 | Convite por link + push | 2 |
| 2 | Modo dono: `owner_id`, RLS, agenda, preço por horário, bloqueios | 3 |
| 3 | Pix com sinal e cancelamento | 3 |
| 4 | Destaque, Pro, página pública da quadra | 2-3 |
| 5 | Design: cartão, chat, tokens, onboarding, ícone | 2 (paralelo) |

## Entrevista com donos (quarta, 21/10; data em aberto)
Prioridade máxima: sem isso, preços e ordem acima são palpite. Objetivo: confirmar dor e disposição de pagar. Não vender; escutar.
1. Como você recebe reservas hoje? Quem anota? Onde?
2. Última vez que alguém não apareceu: o que aconteceu, quanto perdeu?
3. Quantas horas ficam vazias por semana? Quais?
4. Já cobrou sinal? Como? O cliente reclamou?
5. Como as pessoas descobrem sua quadra?
6. Quanto paga hoje por algo parecido (anúncio, sistema, comissão)?
7. Mostrar o app (5 min) e perguntar: o que faria você usar? O que faria desistir?
8. Pagaria R$ __/mês por agenda + sinal + relatório? E por ficar no topo? Qual valor faz pensar "caro"?
9. Aceitaria testar de graça por 30 dias com 1 quadra?
Registrar: nome, nº de quadras, horas vazias, respostas 2, 6 e 8 literais.
Meta: 3 donos; sucesso = 2 aceitam o piloto grátis.

## Concorrentes (pesquisa de 18/10/2026, fraca em preço)
Fontes são páginas de marketing e blogs de comparação (viés). Nenhum preço confirmado. Reler antes de decidir preço.
| Nome | Foco | Observação |
|---|---|---|
| Arena Score Match | reserva + Pix, check-in QR, mensalista, racha com rateio | "0% de comissão sobre clientes do próprio gestor"; planos não públicos; só São Paulo citada |
| TemQuadra | gestão e reserva de quadras | página sem preço nem comissão |
| Easyquadra, Click Quadra, RAQTO, PlayOn | reserva de quadras (RAQTO: raquete) | sem preço visível |
| FutBora, Chega+, Appito, Rachô | organizar racha (presença, sorteio, ranking, financeiro) | Appito: descoberta de jogos e reserva; Chega+: nota de jogador e carteira |
| Rachinha | racha + reserva, grupos fixos, chat | Android "chegando" |
| Webquadras, Agendei Quadras | sistema para o gestor | sem dado de app ativo |
| Playtomic (Espanha) | reserva + partida aberta | comissão do clube por reserva + taxa do jogador + assinatura Premium sem taxas; percentual não publicado |

Leitura: o mercado BR já tem reserva com Pix e racha, em geral para cidades grandes. Espaço provável: cidade média e dono sem sistema nenhum, onde o concorrente não faz onboarding. Hipótese; a entrevista de quarta confirma ou derruba.
Pendente: preços reais (pedir demo/orçamento a 2 concorrentes), avaliações nas lojas, presença em cidades de interior.

## Registro de decisões (18/10/2026, dono do produto)
- WhatsApp só como entrada (link de convite); reserva, aviso, chat e pagamento ficam no app.
- Modo dono dentro do mesmo app (reaproveita `AdminScreen`); portal web só se donos pedirem.
- Chat com texto livre só entre participantes, com limite de ritmo, denunciar/silenciar, aviso fixado e até 3 fotos.
- Ourinhos como laboratório, não como negócio; produto tem de ser replicável por cidade.
- Entrevista com 3 donos na quarta (21/10); sucesso = 2 aceitam piloto grátis de 30 dias. Até lá, nada de nível 3 (dono, Pix, destaque).
- Fase 0 aprovada (nível 1). Mockups de cartão e chat aprovados para gerar.

## Auditoria de pendências: o que concorrentes têm e o app não (18/10/2026)
Fonte: páginas públicas dos concorrentes (marketing). Status = estado do nosso código hoje. Nível segue o fluxo do CLAUDE.md global. Tudo abaixo de "dono" e "Pix" só entra depois da entrevista de 21/10.
| Recurso | Concorrentes | Nosso app hoje | Nível |
|---|---|---|---|
| Conta de dono e multi-quadra por dono | sim (gestor) | não: só `admin` global | 3 |
| Reserva de quadra com horário e pedido ao dono | sim | parcial: reserva rápida privada sem confirmação do dono | 3 |
| Pagamento por Pix com confirmação | Arena Score Match, Easyquadra | não (só chave Pix para rateio manual) | 3 |
| Sinal / garantia de vaga | sim | não | 3 |
| Check-in por QR Code | Arena Score Match | não ("Cheguei" é futuro) | 2 |
| Mensalista com cobrança automática | Arena Score Match, Agendei | parcial: jogo recorrente 4/8/12 semanas, sem cobrança | 3 |
| Rateio automático entre jogadores | sim | parcial: mostra valor por pessoa, sem cobrar | 3 |
| Vitrine/perfil público da quadra | sim | não | 2 |
| Push (reserva, cancelamento, vagas) | sim | não (só lembrete local; precisa Firebase) | 2 |
| Lista de espera | comum | não | 2 |
| Preço por faixa de horário | comum | parcial: preço/hora fixo por quadra | 2-3 |
| Relatório de ocupação/receita | sim | não | 2 |
| Avaliação/reputação do jogador | Chega+ | não (perfil sem ranking, por decisão) | 2 |
| Métricas de tração (jogadores, jogos) | n/a | `0017_metricas.sql` escrita, não rodada, sem tela | 1 |
