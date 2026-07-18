# GH Manager — GitHub Repository Manager (Android)

A Kotlin/ Jetpack Compose Android app for managing GitHub repositories using
Personal Access Tokens (PATs). Built with Clean Architecture / MVVM, encrypted
token storage, and precise API error handling.

## Status
Active development — core engine working and **verified on-device** (v0.3.3-alpha,
versionCode 6). R8-optimized release build (1.88 MB). Dark theme support (System/Dark/Light toggle).

## Features
- **Dark theme**: GitHub-inspired dark palette (`#0D1117` bg, `#161B22` surface, `#2F81F7` accent). System/Dark/Light toggle in Settings. Follows phone default on first launch.
- **Multi-token management**: add several GitHub PATs (each under a user-defined
  name), stored encrypted at rest via `EncryptedSharedPreferences`.
- **Global token switcher**: dropdown in the top app bar switches the active
  token across all tabs; switching wipes session data and reloads lists.
- **Mid-action protection**: attempting to switch tokens while a process is
  busy shows a warning dialog.
- **Swipeable tabs**: swipe left/right (or tap) between the three tabs.
- **Tab 1 — Create Repo**: name, description, public/private, auto-init README.
- **Tab 2 — History**: local log of repos created through this app (per token).
- **Tab 3 — REPOS**: all repos on the active account via the GitHub API, shown
  as a clean name-only list with color-coded tags (PUB/PRIV/PAGES) on the right,
  a "?" legend explaining the tags, and a Refresh button to re-fetch.
- **Repo actions (bottom sheet)**: Open in browser, Open published Pages site
  (when Pages is live), Clone to phone (downloads
  the repo archive ZIP and extracts it into a user-chosen folder via the Storage
  Access Framework), Publish to GitHub Pages (via API, using the repo's default
   branch), change visibility, rename, fork, transfer, and delete (with a
   3-second confirmation countdown to prevent accidents). Code editing / file
   upload omitted.
- **Private-repo Publish helper**: if **Publish (Pages)** fails because a free
   plan can't host Pages on private repos, the app shows a banner with a
   **"Make public & publish"** confirm button — tap it to make the repo public
   and retry Pages automatically (prompt-with-confirm, never automatic).
- **Bulletproof error handling**: every API call parses the HTTP/API response;
  token scope/permission errors are detected and reported explicitly
  (e.g. "requires 'delete_repo' scope").
- **R8-optimized release build**: minified + resource-shrunk APK (~1.88 MB vs ~18 MB debug) with baseline profiles for faster cold starts.

## Tech Stack
- Kotlin, Jetpack Compose (Material 3), DayNight theming
- MVVM + Koin for DI
- Retrofit + OkHttp (Bearer auth injected per active token)
- Room (history + action log) + EncryptedSharedPreferences (tokens + theme preference)
- R8 minification + resource shrinking + baseline profiles (PGO)
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
  security/       TokenStore (EncryptedSharedPreferences), ThemeStore
  ui/             MainViewModel, MainScreen, screens/*, components/*
  ui/theme/       AppTheme, ThemeMode enum, dark/light color schemes
  di/             Koin module
  MainActivity, GHManagerApplication
app/proguard-rules.pro  R8 keep rules (Compose lifecycle, coroutines)
app/src/main/baseline-prof.txt  ART profile for PGO
```

## Known Issues / TODO
- `android:allowBackup="false"` — tokens and clone history are **not** backed up to the cloud. On a true reinstall (uninstall → install), all data is lost. In-place updates (`adb install -r`) with the same signing key preserve data.
- Clone saves an **extracted folder** (not a full git clone). The default save
  location is chosen on first clone (SAF picker) and can be changed in Settings.
- Settings is a dialog (no separate route yet).
- No unit/instrumentation tests yet (key UI controls carry `testTag`/`contentDescription`
  hooks for future Espresso coverage).
- After deleting a repo the list may briefly show the stale entry until the next
  Refresh (GitHub is authoritative). Optimistic list removal is a future hardening.
- GitHub Pages can only be published on **public** repos with a free account; the app
   surfaces GitHub's 422 ("plan does not support Pages") when attempted on a private repo
   and offers a **"Make public & publish"** confirm to flip it public and retry.
