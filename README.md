# GH Manager — GitHub Repository Manager (Android)

A Kotlin/ Jetpack Compose Android app for managing GitHub repositories using
Personal Access Tokens (PATs). Built with Clean Architecture / MVVM, encrypted
token storage, and precise API error handling.

## Status
Active development — core engine working, UI is deliberately minimal (wireframe
layouts, no custom theming) per the Core-Logic-First approach.

## Features
- **Multi-token management**: add several GitHub PATs (each under a user-defined
  name), stored encrypted at rest via `EncryptedSharedPreferences`.
- **Global token switcher**: dropdown in the top app bar switches the active
  token across all tabs; switching wipes session data and reloads lists.
- **Mid-action protection**: attempting to switch tokens while a process is
  busy shows a warning dialog.
- **Tab 1 — Create Repo**: name, description, public/private, auto-init README.
- **Tab 2 — History**: local log of repos created through this app (per token).
- **Tab 3 — Existing Repos**: all repos on the active account via the GitHub API.
- **Repo actions (bottom sheet)**: Clone to phone (zip download), change
  visibility, rename, fork, transfer, delete. Code editing / file upload omitted.
- **Bulletproof error handling**: every API call parses the HTTP/API response;
  token scope/permission errors are detected and reported explicitly
  (e.g. "requires 'delete_repo' scope").

## Tech Stack
- Kotlin, Jetpack Compose (Material 3)
- MVVM + Koin for DI
- Retrofit + OkHttp (Bearer auth injected per active token)
- Room (history + action log) + EncryptedSharedPreferences (tokens)
- Gradle wrapper (AGP 8.5, Kotlin 1.9.24, compileSdk 34)

## Setup / Run
1. Accept Android SDK licenses: `sdkmanager --licenses` (or via Android Studio).
2. Ensure `platforms;android-34` and `build-tools;34.0.0` are installed.
3. `./gradlew assembleDebug` (or open in Android Studio and run on a device/emulator).
4. Open the app → tap the gear icon → add a GitHub PAT (it is validated and the
   username is fetched). The token becomes the active token.

## Project Structure
```
app/src/main/java/com/ghmanager/app/
  data/remote/    API models, Retrofit service, error parser
  data/local/     Room DB, entities, DAOs
  data/repository/ GithubRepository, TokenRepository, HistoryRepository
  security/       TokenStore (EncryptedSharedPreferences)
  ui/             MainViewModel, MainScreen, screens/*, components/*
  di/             Koin module
  MainActivity, GHManagerApplication
```

## Known Issues / TODO
- Clone downloads a zip to `Downloads/GHManager` (not a full git clone).
- Settings screen is currently a dialog; no separate route yet.
- No unit/instrumentation tests yet.
- Token scope errors are inferred heuristically from response text.
