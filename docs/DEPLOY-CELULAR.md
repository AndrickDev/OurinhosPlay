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
