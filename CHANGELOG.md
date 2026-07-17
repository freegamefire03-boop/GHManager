# Changelog

All notable changes to this project are logged here, newest first.

## 2026-07-17 (token-save fixes)
- Fixed: Tokens could fail to save / disappear. Root causes: (1) token metadata was packed into a delimiter string (`::`/`||`) that broke if a token name contained those characters; now stored as JSON. (2) `addToken` restored the previously active token via an async coroutine in `finally`, racing with result handling; restore is now done synchronously in order.
- Fixed: Add-token dialog cleared the name/PAT fields immediately even when validation failed; fields now clear only after a successful save.
- Note: existing tokens saved in the old (broken) metadata format are ignored after this update; re-add tokens if the list appears empty.

## 2026-07-17 (signing fix)
- Fixed: Release APK was unsigned, causing "App not installed / package appears to be invalid" on device. The `release` build type now uses the debug signing config so side-loaded test builds install correctly. (Not for production distribution — a dedicated release keystore is still needed for that.)

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
