# Handoff iOS (Swift + SwiftUI) — para o MacBook

iOS **não está implementado** e não compila no Windows. Este guia deixa tudo pronto para retomar no Mac sem retrabalho de produto/backend.

## 1. O que já é reaproveitado (nada a refazer)
- **Backend Supabase inteiro:** tabelas, constraint anti-choque, RPCs (`create_match`, `join_match`, `leave_match`), RLS, seed. Ver `docs/PRD.md` §6 e `supabase/migrations/`.
- **Regras de negócio:** vivem no banco, não no app Android. O iOS só consome.
- **Fluxos e telas:** PRD §3–5.

## 2. O que precisa ser feito no Mac

### Pré-requisitos
- macOS atual + **Xcode** (App Store)
- `xcode-select --install`
- Conta Apple (gratuita basta para rodar no seu iPhone por 7 dias; Developer Program US$ 99/ano só para TestFlight/App Store)
- Swift Package: `supabase-swift`
- CocoaPods **não** necessário; usar Swift Package Manager

### Mapeamento Android → iOS

| Conceito | Android (feito) | iOS (a fazer) |
|---|---|---|
| UI | Jetpack Compose | SwiftUI |
| Navegação | `NavigationBar` M3 | `TabView` + `NavigationStack` |
| Auth Google | Credential Manager | `GoogleSignIn-iOS` → `signInWithIdToken` |
| Sessão segura | DataStore + Keystore | Keychain (o SDK Supabase já usa) |
| Vibração | `VibrationEffect` | `UINotificationFeedbackGenerator(.success)` |
| Som | `SoundPool` | `AVAudioPlayer` (respeitar silent switch: `AVAudioSession.ambient`) |
| Confete | Canvas | `TimelineView` + `Canvas` ou `CAEmitterLayer` |
| Abrir mapa | `geo:` intent | `MKMapItem.openInMaps` / URL `maps://` |
| Compartilhar | `ACTION_SEND` | `ShareLink` |
| Push | FCM | FCM via APNs (chave `.p8` no Firebase) |
| Cache offline | Room | SwiftData |
| A11y | TalkBack | VoiceOver (`accessibilityLabel`) |

### Configurações específicas Google/iOS
- Criar **OAuth Client iOS** no mesmo projeto Google Cloud (bundle id `com.ourinhosplay.app`).
- Adicionar o **client ID reverso** como URL Scheme no `Info.plist`.
- Autorizar o client iOS nos *Authorized Client IDs* do provider Google no Supabase.

## 3. Estrutura sugerida do projeto iOS
```
ios/
  OurinhosPlay.xcodeproj
  OurinhosPlay/
    App/            # @main, DI
    Core/           # SupabaseClient, Feedback (haptics+som), Config
    Features/
      Auth/ Feed/ Match/ CreateMatch/ MyGames/ Admin/ Venues/
    Resources/      # Assets, bora_la.caf
```
Manter a mesma organização por feature do Android facilita comparar.

## 4. Ordem de implementação recomendada
1. Projeto + `supabase-swift` + config (URL/anon key em `.xcconfig` fora do git)
2. Login Google + profile
3. Feed + filtros
4. Detalhe + `join_match` + feedback sensorial
5. Criar jogo (consumindo o mesmo `create_match`)
6. Meus Jogos
7. Admin
8. Push

## 5. Critérios de paridade
- [ ] Mesmo jogo criado no Android aparece no iOS e vice-versa
- [ ] Choque de horário recusado igual nas duas plataformas (mesma mensagem de erro)
- [ ] "Bora lá" com háptico + som + confete
- [ ] VoiceOver navega Feed → Detalhe → Confirmar

## 6. Cuidados
- Segredos (`anon key`, client IDs) fora do git, como no Android.
- Nunca usar `service_role` no cliente.
- O iOS **não deve reimplementar** validações de choque; chamar a RPC.
