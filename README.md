# Intelligent Gallery System

Android offline-first gallery app in Kotlin, with Python AI services (free/open-source models).

## Modules

- `app/` Android app (Jetpack Compose + Room + Retrofit)
- `ai-service/` Python FastAPI AI service

## Android quick start

1. Open project in Android Studio (Giraffe+ / Hedgehog+).
2. Let Gradle sync.
3. Run on emulator/device.
4. Ensure AI service is running at `http://10.0.2.2:8000/` for emulator.

## AI service quick start

See `ai-service/README.md`.

## Configurable branding

- App name: `app/src/main/res/values/strings.xml` (`app_name`)
- Launcher icon:
  - foreground vector: `app/src/main/res/drawable/ic_launcher_foreground.xml`
  - icon mapping: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
  - background color: `app/src/main/res/values/colors.xml`

## Current implemented flow

- App asks media permissions on launch (images/video/audio).
- After permission, app scans device gallery through MediaStore.
- On-device ML Kit face detection runs for newly indexed images.
- Unknown faces are shown one-by-one with cropped preview for naming.
- Entered names are saved in local Room DB and used for future auto-match.
