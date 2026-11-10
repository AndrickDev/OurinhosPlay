# Testes pendentes (spec de 07/11/2026)

Lista do que falta conferir no aparelho/emulador. Cada caso tem preparo, passos e resultado esperado.
Marque `[x]` ao passar. Bug encontrado: anote o caso (ex.: "B3 falhou: ...").

Testes automáticos (já passam): `.gradlew.bat :app:testDebugUnitTest` → 53/53; `:app:lintDebug` → 0 erros.

## Preparo (uma vez)
1. `.\scripts\emulador.ps1` e `.\scripts\deploy.ps1` (instala a versão atual).
2. Entrar com a sua conta (admin).
3. Criar os dados de teste (5 jogadores falsos, "[Teste] Jogo rolando" começado há 5 min com 6 na lista,
   e "[Teste] Só organizo" amanhã):
   `npx supabase db query --remote --yes -f supabase/testes/jogo_rolando.sql`
4. Ao terminar tudo: `npx supabase db query --remote --yes -f supabase/testes/limpar_testes.sql`
   (resultado esperado: `jogos_teste 0`, `usuarios_teste 0`).

O jogo rolando dura 85 min a partir da criação. Se passar disso, rode o passo 3 de novo (ele apaga e recria).

## A. Sorteio de times
- [x] **A1** Agenda → "[Teste] Jogo rolando" → cartão "Sortear times" aparece (6 confirmados, não é evento).
- [x] **A2** "Sortear" com 2 times: dois times de 3; "Teste Ana" (goleiro) num deles.
- [x] **A3** "+" em Times: chega a 3 (máximo = metade dos confirmados, até 4) → 3 times de 2; não passa de 3.
- [x] **A4** "Sortear de novo" muda a composição (repita 3 vezes; pelo menos uma muda).
- [x] **A5** "Mandar no grupo" abre o compartilhamento com o texto dos times.
- [x] **A6** Num jogo com menos de 4 confirmados ou em evento, o cartão não aparece.

## B. Cronômetro e placar
- [x] **B1** No detalhe do jogo rolando, o cartão do cronômetro aparece (janela: 30 min antes até 2 h depois do fim).
- [x] **B2** Configurar 2 tempos de 1 min, intervalo 1 min → "Iniciar com apito": apito no início; contagem regressiva.
- [x] **B3** Placar Time A × Time B: tocar no número soma gol; "−" desfaz; não fica negativo.
- [x] **B4** Notificação mostra título + placar, tempo regressivo e Pausar/Encerrar.
- [~] **B5** Bloquear a tela: apito no fim do 1º tempo; intervalo; apito no início e no fim do 2º. _(07/11: fases trocaram certo com a tela bloqueada; o som do apito falta ouvir)_
- [x] **B6** Pausar pela notificação → tempo congela; Retomar → continua de onde parou.
- [x] **B7** Fim do jogo: placar final fica na tela até "Fechar"; notificação vira "Fechar".
- [x] **B8 (relógio monotônico, novo)** Com o cronômetro rodando, mudar a hora do emulador
  (Configurações → Sistema → Data e hora → desligar automático → adiantar 10 min).
  Esperado: o cronômetro **não pula**; segue contando normal. Depois religar a hora automática.
- [x] **B9** Perfil com sons desligados: o apito não toca; com sons ligados, toca mesmo no silencioso.

## C. "Eu levo" (simulando a segunda pessoa)
- [x] **C1** No detalhe do jogo rolando, painel de equipamento mostra "bola" com "Teste Bruno" levando; você não consegue assumir a bola.
- [x] **C2** "Eu levo" no colete: aparece seu nome; tocar de novo solta.
- [x] **C3 (segunda conta de verdade, opcional)** Rodar no SQL Editor, como o Bruno, para provar a regra de corrida:
  ```sql
  begin;
  select set_config('request.jwt.claims',
    json_build_object('sub', (select id::text from auth.users where email = 'teste2@teste.ourinhosplay.invalid'), 'role', 'authenticated')::text, true);
  set local role authenticated;
  select public.claim_gear((select id from public.matches where title = '[Teste] Jogo rolando'), 'colete', true);
  rollback;
  ```
  Esperado: erro "Alguém já vai levar" se você estiver com o colete; sem erro se o colete estiver livre.
- [x] **C4** Quem sai do jogo deixa de levar: no SQL, apagar a participação do Bruno
  (`delete from public.participants where user_id = (select id from auth.users where email='teste2@teste.ourinhosplay.invalid');`)
  e reabrir o detalhe: a bola volta a ficar livre.

## D. Agenda
- [x] **D1 (novo)** Agenda → Próximos: "[Teste] Só organizo" mostra **"Você organiza"**; "[Teste] Jogo rolando" mostra **"Você vai"**.
- [x] **D2** Histórico: cartões sem "Você vai"/"Você organiza" e com "Jogar de novo".
- [x] **D3** "Jogar de novo" copia título, quadra, equipamento e selo universitário.

## E. Opções e selo
- [x] **E1** Criar jogo → Mais opções → Público → Universitário ligado → selo no cartão e no detalhe.
- [x] **E2** Interruptores de equipamento por esporte (verde/cinza) salvam e aparecem como pontinhos no cartão.

## F. Antes da Play Store (sem pessoas)
- [ ] **F1** Revisar `docs/POLITICA-PRIVACIDADE.md` (campos `[preencher]`), publicar numa URL pública e pôr o link no Perfil.
- [ ] **F2** Preencher o formulário de serviço em primeiro plano com `docs/PLAY-STORE.md` §1 e gravar o vídeo do B5.
- [ ] **F3** Fotos: decidir sobre `sport_futsal.jpg` (bola com desenho de marca conhecida, sem logo legível); `sport_biribol.jpg` conferida em 07/11 sem marca visível.

## G. Com pessoas (depois)
- [ ] **G1** Teste de usabilidade com 5 pessoas: `docs/TESTE-USABILIDADE.md`.
- [ ] **G2** "Eu levo" com 2 celulares reais (mesmo roteiro do C, sem SQL).

## Execução de 07/11/2026 (emulador, pelo Claude)
Passaram: A1-A5, B1-B4, B7, B8, C1-C3, D1. B5 parcial (fases ok com a tela bloqueada, o som não dá para ouvir via adb).
Faltam: A6, B6, B9, C4, D2, D3, E1, E2 (e F/G).
Observação: o painel "Eu levo" mostra só o primeiro nome ("Teste leva"), então os jogadores falsos aparecem iguais. Num jogo real com nomes repetidos, considerar mostrar "Nome#tag".

## Execução de 10/11/2026 (emulador, pelo Claude)
Passaram: A6 (3 confirmados → sem "Sortear times"; evento barrado no código, `MatchDetail.kt:888`), B6 (pausado congelou em 00:32; retomou do mesmo ponto),
B9 (silencioso + sons ligados → apito como `USAGE_ALARM`; sons desligados → nenhum player), C4 (trigger soltou a bola; app mostra "Tem bola" após recarregar),
D2, D3 (título, quadra, goleiro, bola e Universitário copiados), E1, E2. Dados de teste apagados (0 e 0).

Bugs achados e corrigidos na mesma rodada:
- D2: Histórico mostrava "Entrar" e "Falta goleiro" em jogo passado → agora "Encerrado" e sem selo de função (`ticketAction(past)`).
- Feed mostrava "Entrar" em jogo em que você já está → agora "Você vai" (feed marca `joined`).
- Texto de "Sons do app" dizia que respeita o silencioso sem citar a exceção do apito.

Observações (não corrigidas):
- Detalhe aberto de novo sem reiniciar o app mostrou lista antiga (Bruno ainda lá); reiniciando, certo. Falta atualizar ao reabrir/puxar.
- "Jogar de novo" não copia o valor da quadra (fora da spec; decidir se deve).
- Uma vez, logo após instalar, a Home abriu vazia até "Atualizar"; não reproduziu em 3 tentativas.
