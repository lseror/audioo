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
- Relecture in-app (MediaPlayer) + transcription OpenAI (gpt-4o-transcribe)
- Clés API saisies par l'utilisateur, stockées chiffrées (EncryptedSharedPreferences)

## Build local

Le toolchain n'est pas dans le repo. Sur uTCGDEV :

```bash
source /opt/datas/tools/android-env.sh
./gradlew assembleDebug
```

`local.properties` (gitignored) pointe vers `/opt/datas/android-sdk`. À recréer
si tu clones ailleurs.

## Build release signé

La signature lit `keystore.properties` à la racine (**gitignored**, jamais commité).
Si le fichier est absent, le build release se fait sans `signingConfig`.

1. Générer le keystore (une seule fois, **sauvegarder le mot de passe** — non
   récupérable, sinon plus aucune mise à jour signée de la même app n'est possible) :

```bash
keytool -genkeypair -v -keystore audioo-release.jks -alias audioo \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass '<motdepasse>' -keypass '<motdepasse>' \
  -dname "CN=Laurent Seror, OU=Audioo, O=Serortech, L=Paris, C=FR"
```

2. Créer `keystore.properties` à la racine :

```properties
storeFile=/chemin/absolu/audioo-release.jks
storePassword=<motdepasse>
keyAlias=audioo
keyPassword=<motdepasse>
```

3. Builder :

```bash
source /opt/datas/tools/android-env.sh
./gradlew assembleRelease   # APK : app/build/outputs/apk/release/app-release.apk
./gradlew bundleRelease     # AAB : app/build/outputs/bundle/release/app-release.aab (Play Store)
```

> ⚠️ Le Google Sign-In (Drive) en release utilise le **SHA-1 du certificat release**,
> différent du debug — il faut l'enregistrer dans le client OAuth Android du projet
> Google Cloud, sinon `DEVELOPER_ERROR` (erreur 10).

## Suivi
Jira projet **AOO** (Audioo). Epic : AOO-1.

## Versioning
`VERSION` à la racine + `versionName` dans `app/build.gradle.kts`. Bump patch à
chaque commit qui modifie le code.
