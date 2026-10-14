# OurinhosPlay

App Android nativo (Kotlin + Jetpack Compose) para organizar jogos esportivos em Ourinhos/SP, com anti-choque de horário em quadras. Backend: Supabase. iOS documentado para fase futura.

## Docs
- [PRD](docs/PRD.md) — produto, fluxos, dados, sprints
- [Setup Windows](docs/SETUP-WINDOWS.md) — instalar e verificar o ambiente
- [SQL do Supabase](supabase/migrations/0001_profiles.sql) — rodar no SQL Editor
- [Emulador no PC](docs/EMULADOR-PC.md) — rodar sem celular
- [Deploy no celular](docs/DEPLOY-CELULAR.md) — APK direto no aparelho
- [Handoff iOS](docs/IOS-HANDOFF.md) — retomar no MacBook

## Rodar (PC, emulador)
```powershell
cd C:\Users\andri\OurinhosPlay
.\scripts\emulador.ps1   # sobe o emulador e espera o boot
.\scripts\deploy.ps1     # compila, instala e abre o app
```
Celular físico: veja [DEPLOY-CELULAR](docs/DEPLOY-CELULAR.md).

## Configurar Supabase
1. Copie `secrets.properties.example` para `secrets.properties`.
2. Preencha `SUPABASE_URL` e `SUPABASE_ANON_KEY` (Dashboard → Project Settings → API).
3. Nunca commite `secrets.properties` (já no `.gitignore`).

As chaves chegam ao app via `BuildConfig.SUPABASE_URL` / `BuildConfig.SUPABASE_ANON_KEY`.

## Stack
Kotlin 2.0 • Compose (Material 3) • AGP 8.7 • Gradle 8.9 • minSdk 26 • Supabase
