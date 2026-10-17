# Rodar o app no seu celular (sem loja)

O app instalado pelo `adb` é um APK **debug**; o ID é `com.ourinhosplay.app.debug`, então convive com uma futura versão de release.

## 1. Preparar o celular (uma vez)
1. Configurações → Sobre o telefone → toque 7x em **Número da versão** (ativa opções do desenvolvedor).
2. Opções do desenvolvedor → ligue **Depuração USB**.
3. Conecte o cabo USB → no celular, aceite **"Permitir depuração USB deste computador"**.

## 2. Verificar a conexão
```powershell
adb devices
```
Deve listar o aparelho como `device` (não `unauthorized`, não vazio).

## 3. Instalar
```powershell
cd C:\Users\andri\OurinhosPlay
.\scripts\deploy.ps1
```
O script compila, instala com `adb install -r` (mantém dados) e abre o app.

Manual:
```powershell
.\gradlew.bat :app:assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.ourinhosplay.app.debug/com.ourinhosplay.app.MainActivity
```

## 4. Ver as mudanças
- **Via Android Studio:** botão ▶ Run com o aparelho selecionado. *Apply Changes* (⚡) atualiza sem reinstalar.
- **Via script:** rode `deploy.ps1` de novo após cada alteração.

## 5. Sem cabo (Wi-Fi), Android 11+
1. Celular e PC na mesma rede.
2. Opções do desenvolvedor → **Depuração sem fio** → *Parear dispositivo com código*.
3. No PC: `adb pair IP:PORTA_DE_PAREAMENTO` e digite o código.
4. `adb connect IP:PORTA` (a porta mostrada na tela principal da depuração sem fio).

## 6. Logs
```powershell
adb logcat --pid=$(adb shell pidof -s com.ourinhosplay.app.debug)
```

## Futuro (ainda grátis)
Firebase App Distribution: publica o APK e o celular recebe aviso de nova versão. Entra quando houver mais testadores além de você.

## 7. Amigos não conseguem entrar com o Google

O APK dos amigos (`.\scripts\gerar-apk.ps1`) é assinado com a chave de debug **do seu PC**. O login depende de três configurações no Google Cloud e no Supabase. Confira nesta ordem.

**1. Status de publicação da tela de consentimento (causa mais provável)**
- Google Cloud Console, projeto do app, **APIs e serviços → Tela de permissão OAuth** (ou **Google Auth Platform → Público**).
- Se o status for **Em teste**, só entram as contas listadas em **Usuários de teste** (até 100). Qualquer outro amigo é barrado.
- Duas saídas: adicionar o Gmail de cada amigo em **Usuários de teste**, ou clicar em **Publicar app** (status **Em produção**). O app só pede `openid`, `email` e `profile`, que não são escopos sensíveis, então publicar **não exige revisão do Google** e não tem relação com a Play Store.

**2. Cliente OAuth Android com a impressão digital certa**
- **APIs e serviços → Credenciais**: deve existir um cliente do tipo **Android** com
  - nome do pacote `com.ourinhosplay.app.debug`
  - SHA-1 `1F:8A:7C:BA:3B:9B:18:5C:A0:B2:AE:CE:BF:0E:2D:B1:20:0F:EF:1F` (é a da chave `~/.android/debug.keystore` deste PC; o comando é `keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android`).
- Se o APK for gerado em **outro computador**, a SHA-1 muda e é preciso cadastrar a nova.

**3. Supabase**
- **Authentication → Providers → Google**: ligado, com o ID do cliente **Web** em *Client IDs* (o mesmo valor de `GOOGLE_WEB_CLIENT_ID`).

**Como descobrir a causa exata:** o app mostra na tela de login o erro do Google no formato `Google: <tipo> - <mensagem>`. Peça a um amigo para mandar a foto dessa mensagem. Esse texto só aparece em APK gerado depois da mudança de `AuthViewModel.kt` (versões antigas mostram uma mensagem genérica), então **gere o APK de novo** antes de mandar.

**Quando for para a Play Store:** crie um keystore próprio, cadastre a SHA-1 dele (e a que o Google Play usa para assinar) em um novo cliente Android com o pacote `com.ourinhosplay.app`.
