# Rodar no PC (emulador, sem celular)

Já instalado nesta máquina: JDK 17, Android SDK 35, emulator, imagem `google_apis;x86_64` (API 35), AVD `ourinhos_pixel` (Pixel 7). Aceleração WHPX ativa.

## Comandos
```powershell
cd C:\Users\andri\OurinhosPlay
.\scripts\emulador.ps1   # sobe o emulador e espera o boot (1ª vez ~2-4 min)
.\scripts\deploy.ps1     # compila, instala e abre o app
```

## Útil
```powershell
adb devices                      # deve listar emulator-5554  device
adb logcat -s OurinhosPlay       # logs
emulator -list-avds              # AVDs existentes
.\gradlew.bat :app:assembleDebug # só compilar
```

## Problemas
- Emulador lento/trava: ligue **Plataforma do Hipervisor do Windows** (Recursos do Windows) e reinicie.
- `adb devices` vazio: `adb kill-server; adb start-server`.
- Login Google no emulador exige imagem **google_apis** (a instalada já é).
