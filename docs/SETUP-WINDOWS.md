# Setup do ambiente — Windows 11

Para cada item: **instalar → verificar**. Só avance quando a verificação passar.

| # | Item | Instalar | Verificar (PowerShell) | Esperado |
|---|---|---|---|---|
| 1 | Git | já instalado | `git --version` | `git version 2.x` |
| 2 | Node.js LTS | já instalado | `node --version` | `v2x` |
| 3 | JDK 17 | `winget install Microsoft.OpenJDK.17 -e` | `java -version` | `openjdk version "17.x"` |
| 4 | Android cmdline-tools | zip de `dl.google.com` → `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest` | `sdkmanager.bat --version` | número de versão |
| 5 | SDK packages | `sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"` | `adb version` | `Version 37.x` |
| 6 | Licenças | `sdkmanager --licenses` (aceitar) | pasta `Sdk\licenses` com arquivos | existe |
| 7 | Android Studio | `winget install Google.AndroidStudio -e` | abre pelo menu Iniciar | abre |
| 8 | Supabase CLI | `npx supabase@latest --version` | idem | `2.x` |
| 9 | Gradle (via wrapper) | já vem no projeto | `.\gradlew.bat --version` | `Gradle 8.9` |

## Variáveis de ambiente (usuário)
- `JAVA_HOME` = `C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot`
- `ANDROID_HOME` = `C:\Users\andri\AppData\Local\Android\Sdk`
- `PATH` inclui `%ANDROID_HOME%\platform-tools`

Abra um **terminal novo** depois de alterar variáveis.

## Primeiro build
```powershell
cd C:\Users\andri\OurinhosPlay
.\gradlew.bat :app:assembleDebug
```
APK: `app\build\outputs\apk\debug\app-debug.apk`.

## Problemas comuns
- `SDK location not found` → falta `local.properties` com `sdk.dir=C:/Users/andri/AppData/Local/Android/Sdk` (não vai pro git).
- `java` não reconhecido → terminal aberto antes do `JAVA_HOME`; feche e abra.
- Build lento na 1ª vez → Gradle baixa dependências; as próximas são rápidas.
- `adb devices` vazio → ver [DEPLOY-CELULAR.md](DEPLOY-CELULAR.md).
