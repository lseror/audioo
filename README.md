# Audioo

Voice memo recorder pour Android : enregistre la voix en continu en arrière-plan,
découpe en sessions de 20 minutes, upload sur Google Drive en OGG Opus, pause auto
pendant les appels.

Cible : Samsung Galaxy Z Flip 6, Android 14+ (minSdk 29, targetSdk 34).

## Stack
- Kotlin + Jetpack Compose
- AGP 8.6, Gradle 8.10
- MediaRecorder (OGG Opus natif, API 29+)
- TelephonyCallback + AudioManager (détection phone + VoIP)
- Google Drive REST + WorkManager (upload background)
- Foreground Service + notification persistante

## Build local

Le toolchain n'est pas dans le repo. Sur uTCGDEV :

```bash
source /opt/datas/tools/android-env.sh
./gradlew assembleDebug
```

`local.properties` (gitignored) pointe vers `/opt/datas/android-sdk`. À recréer
si tu clones ailleurs.

## Suivi
Jira projet **AOO** (Audioo). Epic : AOO-1.

## Versioning
`VERSION` à la racine + `versionName` dans `app/build.gradle.kts`. Bump patch à
chaque commit qui modifie le code.
