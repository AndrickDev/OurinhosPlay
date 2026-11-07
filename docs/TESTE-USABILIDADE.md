# Teste de usabilidade (beta local)

Objetivo: descobrir se alguém que nunca viu o app consegue marcar e entrar num jogo **sem explicação**. Barato, presencial, no celular da própria pessoa (APK de teste).

## Quem e quanto
- **5 pessoas**, uma por vez, ~15 min cada. Cinco já mostram a maioria dos problemas.
- Mistura: 2 que organizam pelada, 2 que só jogam, 1 universitário. Pelo menos 1 com mais de 40 anos.
- Ninguém que já viu o app.

## Regras para quem conduz
- Diga só: "Pense em voz alta. Se travar, o problema é do app, não seu."
- **Não ajude.** Se a pessoa pedir ajuda, responda "o que você tentaria?". Ajuda só após 2 min parada (conta como falha).
- Anote: tarefa, tempo, onde hesitou, o que falou, se concluiu.

## Tarefas (nesta ordem)
| # | Tarefa dita à pessoa | Concluiu se... | Limite |
|---|---|---|---|
| 1 | "Entre no app com sua conta Google." | Chegou na lista de jogos | 2 min |
| 2 | "Ache um jogo de futebol para hoje ou amanhã." | Usou filtro ou rolou e abriu um jogo | 1 min |
| 3 | "Entre nesse jogo." | Apareceu na lista "Quem vai" | 1 min |
| 4 | "Avise que você leva o colete." | Tocou "Eu levo" no item certo | 1 min |
| 5 | "Marque um jogo seu para sábado, 18h, 10 vagas, só para universitários." | Jogo publicado com selo | 4 min |
| 6 | "Mande o jogo para um amigo." | Abriu o compartilhar | 1 min |
| 7 | "No dia do jogo: ligue o cronômetro com 2 tempos de 20 min." | Cronômetro rodando | 2 min |
| 8 | "Desista do jogo da tarefa 3." | Saiu da lista | 1 min |

## Indicadores
- **Taxa de conclusão por tarefa** (meta: ≥ 4 de 5 em cada).
- **Tempo** por tarefa (comparar com o limite).
- **Erros**: toque no lugar errado, voltar atrás, abrir tela que não leva à tarefa.
- **Desistência**: pediu ajuda ou passou do limite.
- Ao fim, 2 perguntas: "De 0 a 10, quanto recomendaria a um amigo?" e "O que mais te irritou?".

## Depois
- Lista única de problemas, ordenada por **quantas pessoas** tropeçaram × **gravidade** (bloqueou / atrasou / incomodou).
- Corrigir só o que ≥ 2 pessoas tropeçaram. O resto vira observação.
- Métricas do painel admin (`AdminMetricsTab`: jogadores, ativos 30d, jogos, vagas preenchidas, % não cancelados) acompanham o uso real depois do teste. **Não adicionar telemetria nova** antes de o teste mostrar uma pergunta que elas não respondem.

## Planilha de anotação (copiar por pessoa)
```
Pessoa: ____  Perfil: ____  Idade: ____
T1 __s  ok/falha  hesitou em: ______  falou: ______
T2 ...
NPS: __  Irritou: ______
```
