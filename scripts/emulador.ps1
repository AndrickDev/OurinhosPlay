#requires -Version 5.1
# Inicia o emulador (AVD ourinhos_pixel) e espera o boot terminar.
$ErrorActionPreference = 'Stop'
$sdk = [Environment]::GetEnvironmentVariable('ANDROID_HOME', 'User')
if (-not $sdk) { $sdk = "$env:LOCALAPPDATA\Android\Sdk" }
$emu = Join-Path $sdk 'emulator\emulator.exe'
$adb = Join-Path $sdk 'platform-tools\adb.exe'
if (-not (Test-Path $emu)) { throw "Emulador nao instalado. Veja docs/EMULADOR-PC.md" }

$ja = & $adb devices | Select-String 'emulator-\d+\s+device'
if (-not $ja) {
    Start-Process -FilePath $emu -ArgumentList '-avd', 'ourinhos_pixel', '-no-snapshot-save', '-gpu', 'auto'
    Write-Host 'Iniciando emulador...'
    & $adb wait-for-device
}
do { Start-Sleep 3 } until ((& $adb shell getprop sys.boot_completed 2>$null).Trim() -eq '1')
Write-Host 'Emulador pronto.' -ForegroundColor Green
