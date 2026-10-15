#requires -Version 5.1
# Gera o APK para mandar a amigos de teste e copia para a Área de Trabalho.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not $env:JAVA_HOME) { $env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User') }

& .\gradlew.bat :app:assembleBeta --console=plain
if ($LASTEXITCODE -ne 0) { throw 'Build falhou.' }

$apk = Join-Path $root 'app\build\outputs\apk\beta\app-beta.apk'
$dest = Join-Path ([Environment]::GetFolderPath('Desktop')) 'OurinhosPlay-teste.apk'
Copy-Item $apk $dest -Force
Write-Host "OK: $dest" -ForegroundColor Green
Write-Host 'Mande esse arquivo aos amigos (WhatsApp, Drive). Eles precisam estar na lista de testadores do Google.'
