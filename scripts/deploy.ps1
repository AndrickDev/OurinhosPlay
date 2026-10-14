#requires -Version 5.1
# Compila, instala no celular via adb e abre o app.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not $env:JAVA_HOME) { $env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User') }
$adb = Join-Path ([Environment]::GetEnvironmentVariable('ANDROID_HOME', 'User')) 'platform-tools\adb.exe'
if (-not (Test-Path $adb)) { throw "adb nao encontrado em $adb. Veja docs/SETUP-WINDOWS.md" }

$devices = & $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '\tdevice$' }
if (-not $devices) { throw "Nenhum dispositivo. Rode .\scripts\emulador.ps1 (PC) ou veja docs/DEPLOY-CELULAR.md (adb devices deve listar 'device')." }

& .\gradlew.bat :app:assembleDebug --console=plain
if ($LASTEXITCODE -ne 0) { throw 'Build falhou.' }

$apk = Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk'
& $adb install -r $apk
if ($LASTEXITCODE -ne 0) { throw 'Instalacao falhou.' }

& $adb shell am start -n com.ourinhosplay.app.debug/com.ourinhosplay.app.MainActivity
Write-Host 'OK: app instalado e aberto.' -ForegroundColor Green
