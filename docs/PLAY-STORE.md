# Play Store: textos prontos para os formulários

Rascunho de 07/11/2026. Copiar e colar no Play Console. Conferir antes de enviar.

## 1. Serviço em primeiro plano (`FOREGROUND_SERVICE_SPECIAL_USE`)
Play Console → Conteúdo do app → Permissões de serviço em primeiro plano.

- **Tipo:** Uso especial (`specialUse`).
- **Descrição (cole):**
  > O OurinhosPlay tem um cronômetro de partida esportiva que o próprio usuário inicia no detalhe do jogo.
  > Ele conta os tempos da partida (ex.: 2 × 20 min com intervalo) e toca um apito no fim de cada tempo,
  > inclusive com a tela bloqueada, porque o celular fica no bolso ou no banco durante o jogo. O serviço
  > mostra uma notificação fixa com o tempo restante e os botões Pausar/Retomar/Encerrar, para sozinho
  > no fim da partida e tem limite de 4 horas. Nenhum dos tipos padrão (mediaPlayback, dataSync, etc.)
  > descreve um cronômetro que precisa disparar alertas sonoros no tempo exato.
- **Impacto se negado:** o apito atrasaria ou não tocaria com a tela bloqueada (o Android suspende o app).
- **Vídeo (pedido pelo formulário):** gravar o emulador: abrir jogo → Cronômetro → Iniciar com apito →
  bloquear a tela → apito no fim de 1 min. Ver `docs/TESTES-PENDENTES.md`, caso C3.

## 2. Segurança dos dados (Data safety)
Base: `docs/POLITICA-PRIVACIDADE.md`.

| Pergunta | Resposta |
|---|---|
| Coleta ou compartilha dados? | Coleta, não compartilha |
| Dados criptografados em trânsito? | Sim (HTTPS do Supabase) |
| O usuário pode pedir exclusão? | Sim, no app (Perfil → Excluir minha conta) |
| Informações pessoais | Nome (exibição), e-mail, IDs de usuário: funcionalidade do app e gerenciamento de conta |
| Fotos | Sim, opcional, funcionalidade do app (foto do jogo) |
| Mensagens | Sim, outras mensagens no app (chat rápido do jogo) |
| Localização, contatos, financeiro, saúde | Não coletados |
| Atividade no app | Interações no app (jogos criados/entrados): funcionalidade do app |

## 3. Pendências antes de enviar
- Publicar a política numa URL pública e colocar o link no Perfil do app.
- Trocar as fotos com marca: futsal e biribol (`docs/CREDITOS-FOTOS.md`).
- Push (Firebase) ainda não existe; se entrar, atualizar o Data safety (ID do dispositivo).
