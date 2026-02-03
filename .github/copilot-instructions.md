# PlayTime Native – AI coding guide

## Big picture
- Android app written in Kotlin using Jetpack Compose + Material 3 with MVVM + Hilt DI.
- Data flow: UI (Compose screens) → ViewModels → Repository/Room → Services/Receivers.
- USP: scheduled playback via AlarmManager → `AlarmReceiver` → `MusicService` (Media3) with boot persistence.

## Key areas & examples
- App entry + DI: `MainActivity`, `PlayTimeApplication` (Hilt).
- UI screens: Home + Scheduler + Mini Player in `ui/screens` and `ui/components`.
- Playback: `service/MusicService` (Media3/ExoPlayer).
- Scheduling: `receiver/AlarmReceiver`, `receiver/BootReceiver`, `util/AlarmScheduler`.
- Data: `data/model` + `data/dao` + `data/database` + `data/repository`.

## Build / test / debug workflows (PowerShell)
- Build + install debug: `./gradlew installDebug`
- Clean + reinstall: `./gradlew clean installDebug`
- Tests: `./gradlew test`
- Lint: `./gradlew lint`
- Release APK: `./gradlew assembleRelease`
- Live debug script: `./debug_app.ps1` (launches app + logcat filter)

## Device/ADB conventions
- ADB path is expected at `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe` (see debug script).
- Logcat filters typically include: `PlayTime|MainActivity|MusicService|AlarmReceiver|AndroidRuntime|FATAL`.

## Project-specific patterns
- Compose UI should use the OLED-dark theme tokens (background #121212, accent #1DB954) from the app theme.
- ViewModels expose state for UI; playback and scheduling state live in `MusicViewModel` and `SchedulerViewModel`.
- Room schema changes require updating the database version in `PlayTimeDatabase`.
- Scheduled playback relies on exact alarms + wake locks + boot reschedule; be careful when touching receivers.

## Dependencies worth knowing
- Media3 (ExoPlayer), Room (KSP), Hilt, Coil, Accompanist permissions/system UI.
