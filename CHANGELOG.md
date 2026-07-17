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
