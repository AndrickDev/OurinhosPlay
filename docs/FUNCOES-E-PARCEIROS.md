# Funções por vaga, reserva rápida e parceiros

Desenho da rodada de 17/10/2026. Objetivo do produto: ser a melhor forma de praticar esporte, com a melhor UI/UX e o menor atrito possível, e abrir caminho para monetizar por **serviços em cima do app** (não por assinatura do jogador).

## Dois jeitos de usar, o mesmo app

| Perfil | Porta de entrada | O que vê |
|---|---|---|
| **Só quer a quadra** | "Só reservar" | Modalidade, local e horário. Reserva privada de 1 vaga, sem lista, chat nem rateio. Abre na última quadra usada. "Abrir vagas" depois, com um toque. |
| **Quer montar o jogo** | "Jogo com a galera" | Vagas, funções (goleiro...), rateio, nível, repetição. O que é opcional fica em "Mais opções". |

Regra de ouro: tudo que é opcional tem padrão inteligente. Quem não mexe em nada ainda cria um jogo válido.

## Funções por vaga (modelo C)

- Cada modalidade pode ter **uma função rara** que costuma faltar: goleiro (futebol, futsal) e levantador (vôlei). As demais modalidades seguem com vaga única.
- O criador define quantas vagas dessa função, se ficam **reservadas**, e o **benefício** no rateio: nada (paga tudo), metade ou isento. Padrão: 2 vagas, reservadas, isento.
- **Reservada** quer dizer que ninguém ocupa essa vaga entrando sem função. Se sobrar vazia, vira vaga comum `release_hours` antes do jogo (1, 3, 6 ou 24 h; padrão 3 h).
- Quem entra na função recebe o benefício. O rateio da tela do jogo já desconta.
- O feed mostra "Falta goleiro" nos jogos com a função aberta.

### Banco (migration `0015_funcoes.sql`)

| Objeto | O que faz |
|---|---|
| `match_roles` | Funções do jogo: vagas, reservada, horas para liberar, benefício (0/50/100). |
| `participants.role` | Função escolhida ao entrar. |
| `match_roles_status(match)` | Situação por função: ocupadas, travada, quando libera. |
| `create_match(..., p_roles jsonb)` | Cria o jogo e suas funções; confere que a soma das funções cabe nas vagas. |
| `join_match(match, p_role)` | Entra com ou sem função; respeita vagas reservadas ainda vazias. |
| `open_reservation(match, vagas)` | Transforma a reserva rápida em jogo aberto (só o criador). |
| `matches_feed.roles_open` | Funções com vaga sobrando, para o aviso no feed. |

O app funciona sem a migration (jogos sem função continuam normais); as funções só aparecem depois de rodá-la.

## Duração livre

Os atalhos são 1h, 1h30 e 2h; "Outro" abre um ajuste de 15 em 15 minutos (e de 1 em 1 hora), de 30 minutos a 6 horas. O horário de término aparece sempre ("Termina às 08:15", com "dia seguinte" quando passa da meia-noite). No banco, a migration `0016_duracao_livre.sql` relaxa a regra antiga (só 60, 90 ou 120).

## Visual

Referência: Apple. Decisões, e por quê:

- **Vidro só em controles flutuantes** (só a barra de abas da tela inicial; a barra de ação dentro do jogo é sólida), como no Liquid Glass da Apple. Blur real do que rola por trás (biblioteca Haze), em tom grafite neutro; em aparelhos sem suporte a blur, cai para um fundo quase opaco.
- **Conteúdo em superfícies sólidas e neutras.** Cartões translúcidos sobre fundo colorido perdiam contraste e deixavam o app com cara de vários temas ao mesmo tempo.
- **Fundo azul-noite sólido** (`#0A0E1F`, de volta ao tom da primeira versão, mais sóbrio e sem cinza), sem degradê nem luzes coloridas. Laranja-pista é a única cor de ação; a cor do esporte aparece só em sinais pequenos (ícone, vagas, horário).
- **Fotografia no lugar de ilustração.** A arte vetorial (quadra e bola) parecia forçada e pouco realista, então foi removida. Cada modalidade tem uma foto (Pexels, ver `CREDITOS-FOTOS.md`): miniatura no feed, capa com degradê escuro no jogo e cartões de foto na hora de escolher a modalidade. O horário vira numerais de placar (Barlow condensado itálico), a única assinatura ousada do app.
- Títulos em Barlow semibold, sem itálico; o itálico fica na marca e nos horários.
- As barras flutuantes ficam **fora** da área que elas borram (irmãs do conteúdo); dentro dela o blur não age.
- Evolução: trocar as fotos genéricas por registros das quadras reais de Ourinhos, com autorização, e tirar as que mostram marcas (futsal).

## Monetização: serviços em cima do app

Premissa: o app é grátis para o jogador e para a quadra. A receita vem de serviços ligados ao uso real da quadra. Nesta fase, tudo isso é **protótipo** para mostrar a parceiros; ainda não há integração.

| # | Modelo | Como funciona | Receita | Atrito |
|---|---|---|---|---|
| 1 | **Cupom de consumo local** | Jogo confirmado e "Cheguei" geram um código. O grupo ganha desconto na quadra ou na lanchonete parceira; o parceiro valida o código. | Valor fixo por resgate ou acordo mensal | Baixo |
| 2 | **Torneio com premiação patrocinada** | Inscrição por equipe. O comércio local banca a premiação em troca de marca. | Taxa de inscrição + cota de patrocínio | Médio |
| 3 | **Horário ocioso negociável** | A quadra define um preço-piso nos horários vazios. O grupo faz uma oferta; o dono aceita ou contra-propõe. Grupo completo no app vale desconto automático. | Taxa fixa por reserva fechada | Médio |
| 4 | **Serviços no jogo** | Árbitro, bola, colete, hidratação, fotógrafo, churrasco pós-jogo. | Comissão por serviço | Médio |
| 5 | **Passe de benefícios** | Assinatura barata do jogador com descontos nos parceiros. | Recorrência | Alto (precisa de massa crítica) |

Riscos conhecidos:

- **Comissão sem caixa integrado.** Não dá para medir o consumo real; começar com valor fixo por resgate.
- **Custódia de dinheiro.** Evitar segurar premiação ou aluguel; o Pix vai direto ao dono, o app só cobra a taxa.
- **Premiação.** Competição por habilidade é tranquila; sorteio ou rifa exige autorização, então evitar.
- Todos os modelos dependem de parceiros e de um portal simples para eles (cadastrar oferta, validar código, ver relatório).

Ordem sugerida: 1, depois 2, depois 3.

## Próximas etapas deste desenho

1. Lista de espera por função, com promoção automática quando alguém sai.
2. Sorteio de times equilibrado por nível e por função (um goleiro por time).
3. "Posso jogar de..." no perfil e push quando abrir vaga de goleiro.
4. Camada de parceiros em modo protótipo (cupom ligado ao "Cheguei").
5. Modo atleta (corrida, ciclismo, evolução): por último.
