# Changelog

All notable changes to this project are logged here, newest first.

## 2026-07-17 (snackbar/popup fix)
- Fixed: The message popup shown after actions was barely readable (no explicit text color, could sit under the nav bar, truncated long messages, auto-hid too fast). It now uses an explicit high-contrast on-container text color, respects nav-bar padding, is full-width and scrollable for long messages, has a "Dismiss" button, and lingers longer (10s) for errors/long messages.

## 2026-07-17 (v0.3.0-alpha — Repos tab UX + Publish)
- Changed: Renamed the "Existing" tab to "REPOS".
- Changed: Repo cards now show only the repository name (removed the cluttered full-name/visibility/description line) for easier scanning.
- Added: Swipe left/right to move between the three tabs (Create Repo / History / REPOS), synced with the tab bar.
- Added: "Open in Browser" action in the repo actions sheet (opens the repo's GitHub page).
- Added: "Publish (GitHub Pages)" action — enables Pages via `POST /repos/{owner}/{repo}/pages` using the repo's actual default branch (so it works for `master` repos too, not just `main`). Empty/branch-less repos surface a clear API error.
- Added: Refresh button on the REPOS tab to re-fetch the current account's repositories.
- Added: Delete confirmation dialog with a 3-second countdown that keeps the confirm button disabled until it reaches 0, preventing accidental deletes.
- Fixed: Bottom action ("Delete") in the repo sheet was hard to tap under the system nav bar; the sheet now applies navigation-bar padding and is scrollable.

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
