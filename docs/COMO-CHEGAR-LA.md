# Como chegar lá: do protótipo ao beta público

Guia de execução dos itens que faltam, na ordem em que valem a pena. Estimativas são de esforço de uma pessoa, em dias úteis, e servem só como referência.

## Ordem sugerida

1. **Login dos amigos funcionando** (hoje bloqueia qualquer teste). Ver [DEPLOY-CELULAR.md, seção 7](DEPLOY-CELULAR.md). Meio dia.
2. **Limpar os dados de teste** do banco, deixando só jogos de verdade. Meio dia.
3. **Primeiros passos** e **carregamento com esqueletos**. 1 a 2 dias.
4. **Métricas de uso**, antes de chamar mais gente, para medir desde o começo. 1 dia.
5. **Lista de espera** e **equipamento em interruptores**. 3 a 4 dias.
6. **Notificações push**. 2 a 3 dias.
7. **Convite por link**. 1 a 2 dias, mais o tempo de ter um domínio.
8. **Play Store**. 2 dias de trabalho, mais o prazo de teste fechado exigido pelo Google.
9. **Parceiros**: só depois que houver gente jogando toda semana.

## Primeiros passos (tutorial na primeira abertura)

O mais simples que funciona: três telas deslizáveis, com "Pular" sempre visível, mostradas só uma vez.

| Tela | Mensagem |
|---|---|
| 1 | Veja quem joga hoje em Ourinhos e entre com um toque. |
| 2 | Reserve a quadra em três toques, ou monte o time com goleiro garantido. |
| 3 | Chame a galera e receba aviso quando abrir vaga. (Aqui o app pede a permissão de notificação.) |

Guarde um sinal "já viu" no aparelho (`DataStore`) e mostre a sequência só se ele não existir. Sem animação elaborada: texto curto, a foto da modalidade e um botão.

## Transições de tela

Hoje abrir um jogo troca a tela de uma vez. O ganho barato é uma transição curta (200 a 250 ms) de esmaecer e deslizar levemente, ao abrir e fechar o jogo e o fluxo de criar, usando `AnimatedContent` do Compose. Evite efeitos longos ou chamativos: a ideia é só tirar o "corte seco".

## Métricas de uso

Duas opções gratuitas e adequadas:

- **PostHog** (plano gratuito generoso, funis e retenção prontos).
- **Firebase Analytics** (grátis, e reaproveita o projeto do Firebase usado nas notificações).

Eventos mínimos: `app_aberto`, `login_concluido`, `jogo_criado` (com modalidade e se foi "só reservar" ou "com a galera"), `jogo_entrou` (com função, se houver), `jogo_lotou`, `cheguei_na_quadra`, `convite_compartilhado`, `convite_aberto`.

Números que importam:

| Métrica | Por quê |
|---|---|
| Retorno em 1 e 7 dias | Diz se o app vira hábito |
| Jogos criados por semana | Mostra se há oferta |
| Taxa de lotação ao começar (confirmados ÷ vagas) | Mostra se a pelada realmente fecha |
| Tempo até o primeiro "Bora lá" | Mede o quanto o feed ajuda |
| Quantos criam vs. quantos só entram | Mostra se há quem organize |

## Notificações push

1. Criar um projeto no **Firebase** (gratuito) e baixar o `google-services.json` para os pacotes `com.ourinhosplay.app.debug` e, depois, `com.ourinhosplay.app`.
2. Incluir o Firebase Cloud Messaging no app e pedir a permissão de notificação (Android 13 ou superior).
3. No Supabase, uma tabela `device_tokens(user_id, token)` guarda o aparelho de cada pessoa.
4. Uma **Edge Function** do Supabase envia a mensagem quando algo acontece: vaga aberta, jogo cancelado, promoção da lista de espera, amigo entrou.
5. Preferências por tipo de aviso no perfil, para ninguém desligar tudo por causa de um tipo.

## Convite por link

- **Rápido, sem domínio:** o app já compartilha o código de 6 caracteres. Dá para somar um link `ourinhosplay://j/CODIGO` que abre direto no jogo para quem já tem o app.
- **Definitivo:** *App Links* do Android. Precisa de um domínio (pode ser um gratuito em Netlify, Vercel ou GitHub Pages) com o arquivo `/.well-known/assetlinks.json` contendo o pacote e a impressão digital SHA-256 do certificado, e um `intent-filter` com `autoVerify` para `https://seu-dominio/j/CODIGO`. Quem não tem o app cai numa página simples que leva à Play Store.
- A SHA-256 da chave de debug deste PC é `97:ED:28:F4:46:8B:CD:AF:7E:52:73:73:A4:AF:2C:9A:A8:4E:FE:DA:D7:EF:CE:4D:91:9D:32:86:8F:9B:A1:D3`. A de produção será outra.

## Play Store

1. Conta de desenvolvedor (taxa única de cerca de US$ 25).
2. Keystore próprio de release e novo cliente OAuth Android com o pacote `com.ourinhosplay.app` (ver seção 7 de [DEPLOY-CELULAR.md](DEPLOY-CELULAR.md)).
3. Gerar o pacote `.aab`, ficha da loja (ícone, capturas de tela, descrição) e classificação etária.
4. **Política de privacidade** em uma página pública, e o formulário de segurança de dados.
5. **Excluir conta dentro do app**: o Google exige para apps que criam conta. Hoje só o admin remove pessoas, então falta esta função para o próprio usuário.
6. Contas pessoais novas costumam precisar de um **teste fechado** com um número mínimo de testadores por um período antes de liberar para todos. As regras mudam; confira o valor atual no Play Console antes de planejar a data.

## Fotos

As atuais são de banco de imagens (ver [CREDITOS-FOTOS.md](CREDITOS-FOTOS.md)). Para o lançamento, o ideal é registrar as quadras reais de Ourinhos, com autorização de quem aparece, e usar o logo ou a marca dos locais apenas com permissão.
