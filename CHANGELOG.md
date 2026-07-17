# Changelog

All notable changes to this project are logged here, newest first.

## 2026-07-17
- Added: Full Kotlin/Jetpack Compose Android app scaffold (Gradle, manifest, Application class).
- Added: Encrypted token storage via EncryptedSharedPreferences (TokenStore).
- Added: GitHub API layer (Retrofit + OkHttp) with per-token Bearer auth and a precise error parser that detects token scope/permission errors.
- Added: Room DB for local repo-creation history and an action log.
- Added: MainViewModel holding active-token state, busy state, and reactive lists.
- Added: 3-tab layout (Create Repo, History, Existing Repos) with a top-app-bar global token switcher.
- Added: Token settings dialog to add/remove PATs (validated against the GitHub API).
- Added: Repo actions bottom sheet: clone (zip), change visibility, rename, fork, transfer, delete.
- Added: Mid-process token-switch warning dialog.
- Fixed: Build issues — added android.useAndroidX, lifecycle-runtime-compose dep, fixed RenameRepoRequest/ActionLogEntity constructor usage.
- Build verified: `gradlew assembleDebug` succeeds.

## 2026-07-17 (v0.2.0-alpha)
- Changed: "Clone to Phone" now downloads the GitHub archive ZIP and **extracts it** into the chosen folder (previously only saved the zip).
- Added: First-run save-location picker via Android Storage Access Framework (SAF). The chosen folder URI is persisted as the default and granted persistent read/write permission.
- Added: Settings option to change the default clone save location at any time.
- Added: SaveLocationStore (encrypted) to persist the default save directory URI.
- Added: `androidx.documentfile` dependency for SAF tree extraction.
- Build verified: `gradlew assembleDebug` succeeds. Version bumped to 0.2.0-alpha (versionCode 2).
