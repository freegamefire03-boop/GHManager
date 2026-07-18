# Changelog

All notable changes to this project are logged here, newest first.

## 2026-07-18 (v0.3.3-alpha — dark theme + performance optimization + R8 fix)
- Added: GitHub-inspired **dark theme** (`#0D1117` background, `#161B22` surface, `#2F81F7` primary accent). `ThemeMode` enum (SYSTEM/DARK/LIGHT) persisted via `ThemeStore` (EncryptedSharedPreferences). Default = SYSTEM (follows phone setting). Settings dialog has a 3-button theme toggle (System/Dark/Light) at the top.
- Changed: `AndroidManifest` theme set to `Theme.AppCompat.DayNight.NoActionBar` to prevent a white flash on launch when the phone is in dark mode.
- Changed: `app/build.gradle` now enables `minifyEnabled true` and `shrinkResources true` for the release build type, reducing the APK from ~18 MB (debug) to **1.88 MB** (release) via R8 tree-shaking and resource deduplication.
- Added: Hand-written `baseline-prof.txt` (ART profile rules) and `ProfileInstaller:1.4.1` dependency to guide Ahead-of-Time (AOT) compilation on first launch for faster cold starts.
- Added: `proguard-rules.pro` keep rules for `androidx.lifecycle.compose.**`, `androidx.compose.runtime.**`, `androidx.compose.ui.platform.*CompositionLocals*`, and `kotlinx.coroutines.**` — prevents R8 from stripping `LocalLifecycleOwner` and other CompositionLocals needed at runtime.
- Fixed: Release APK crashed on launch (`IllegalStateException: CompositionLocal LocalLifecycleOwner not present`) because R8 stripped the lifecycle-runtime-compose CompositionLocal fields. The ProGuard keep rules above resolve this.
- Changed: `gradle.properties` sets `android.enableR8.fullMode=false` — R8 full mode aggressively strips even more and caused additional crashes; standard minification already delivers the size/perf gains.
- Changed: `activeToken` in `MainScreen.kt` is now wrapped in `remember(tokens, activeTokenId)` so Compose doesn't re-compute it on every recomposition.
- Changed: `LazyColumn.items` in `ExistingReposTab.kt` now keys items by `it.fullName` for stable recomposition and smoother list updates.

## 2026-07-18 (v0.3.2-alpha — delete/clone error bug fix + red delete button)
- Fixed: Deleting a repository always showed a bogus error banner ("Request failed with HTTP 204" / similar) even though the delete succeeded. Root cause: `serviceCall` in `GithubRepository.kt` treated a successful **204 No Content** response (DELETE returns an empty body) as an error because `resp.body()` was null. Now an HTTP-success with a null body is treated as success (returns `ApiResult.Success(Unit)`), so `deleteRepo` shows the correct "Repository '…' deleted" confirmation. This also fixes any other empty-body success responses.
- Changed: Post-mutation refreshes (delete/visibility/rename/fork/transfer/publish/Make-public-and-publish/create) now use a **quiet** reload (`reloadReposQuietly`) that updates the list without ever overwriting a success confirmation with a transient list-refresh error.
- Changed: The "Delete Repository" button in the repo action sheet is now **red** (Material error color) to make the destructive action obvious.

## 2026-07-18 (v0.3.2-alpha — "Make public & publish" UX)
- Added: When **Publish (Pages)** fails on a private repo because the GitHub plan doesn't support Pages on private repos (HTTP 422 with the plan/private/public keyword), the app now shows a banner asking "Make this repo public, then publish?" with a **"Make public & publish"** confirm button. Tapping it makes the repo public (`updateRepo` `private=false`) and retries `enablePages` automatically — a prompt-with-confirm, not an automatic change.
- Added: `GithubError.isPrivatePagesError` detection; `UiMessage` gained `actionLabel`/`action` fields and `UiAction.MAKE_PUBLIC_AND_PUBLISH`; `NoteBanner` renders an optional action button (`banner_action` testTag).
- Verified: banner message + button render correctly in-app (uiautomator). The exact GitHub sequence the confirm performs (PATCH public → HTTP 200; POST pages → HTTP 201 with live `html_url`) was proven working via the API.

## 2026-07-18 (v0.3.2-alpha — test hooks + on-device verification)
- Added: `contentDescription`/`testTag` semantics on key Compose controls (tabs, repo cards, Refresh, action-sheet buttons, delete-confirm, dialog Save buttons) so the UI can be driven deterministically by `uiautomator`/Espresso instead of blind coordinate taps. Harmless to keep; aids future automated testing.
- Verified on device (R58N34T8EBE, real PAT): app launches clean after the DB v1->2 upgrade; live GitHub API calls succeed (`GET /user/repos` 200); **Clone to Phone** downloads + extracts the zipball into `Download/GITHUB CLONES/<repo>/` (SAF write works); **Make Private** flips tag + server state; **Publish (Pages)** issues the correct `POST .../pages` and gracefully surfaces GitHub's 422 ("plan does not support Pages" — a free-account limitation, not a bug); **Delete** enforces the 3s countdown then `DELETE` -> 204, repo removed (list refreshes to 14).
- Note: a brief stale-render was observed right after Delete until an explicit Refresh; GitHub is authoritative (404). No blocker.

## 2026-07-18 (v0.3.2-alpha — HOTFIX: launch crash)
- Fixed: App crashed on launch ("opens then closes immediately") with `IllegalStateException: Room cannot verify the data integrity`. Cause: the F2 change added columns to `RepoHistoryEntity` (schema change) but the Room database version was not bumped. Bumped `AppDatabase` version 1 -> 2; `fallbackToDestructiveMigration()` wipes and rebuilds the local DB on upgrade (History is cleared on first launch of this version — expected for this personal app).
- Verified on device via ADB: launches cleanly (`Displayed ...MainActivity`), no crash, process stays alive.

## 2026-07-18 (v0.3.2-alpha — test-report fixes F1-F5)
- Fixed (F1): 422 responses are now recognized as scope/permission errors when the body references a scope term (scope/permission/delete_repo/requires), so users get a helpful "requires '<scope>' scope" hint instead of a generic "Validation failed".
- Fixed (F2): History-tab repos now carry their `default_branch` and `has_pages` flags, so cloning/publishing from History uses the correct branch and "Open Published Page" appears when applicable.
- Cleaned up (F3): removed two unused Retrofit endpoints (`getUserRepos`, `getRepo`).
- Fixed (F4): "Publish to GitHub Pages" now uses the repo's actual default branch and refuses (with a clear error) when the default branch is unknown, instead of silently assuming `main`.
- Fixed (F5): repo listing now paginates (100/page) so accounts with more than 100 repos are fully loaded.

## 2026-07-17 (v0.3.1-alpha — clone reliability fixes)
- Fixed: Clone/download could hang forever or fail silently — the network download and zip extraction now run on `Dispatchers.IO` instead of the main thread (was risking ANR / NetworkOnMainThreadException).
- Fixed: The repo archive is now streamed straight to a cache file instead of being buffered fully in memory via `.bytes()` (prevents OOM / hangs on larger repos). Temp file is deleted after extraction.
- Fixed: Clone now uses the bare `/zipball` endpoint so GitHub resolves the true default branch itself — removes the hardcoded-branch 404 (e.g. `master` repos).
- Added: User-Agent header on the zipball request (GitHub can reject API calls without one); added a 5-minute write timeout; validate the download is a real zip (`PK` signature) before extracting; clearer HTTP 401/403/404 error messages.
- Added: Zip-slip protection — entries containing `..` path segments are rejected.
- Fixed: Folder collision handling rewritten (clean `name`, `name_1`, `name_2`… selection) and existing files are now deleted before write instead of being appended to.

## 2026-07-17 (note banner reposition/redesign)
- Changed: The action-result note now appears at the TOP of the content area, directly below the tab bar (no longer covers the tab names or floats at the bottom). Redesigned as a rounded banner card with an "NOTE"/"ERROR" label and a close (X) icon on their own row, and the message text on a separate scrollable row — so the dismiss control no longer overlaps the text.

## 2026-07-17 (repo tags + open published page)
- Added: Color-coded tags on the right of each repo title instead of stacked text: green "PUB" (public), red "PRIV" (private), orange "PAGES" (GitHub Pages published).
- Added: "?" help icon (left of Refresh) on the REPOS tab that opens a legend explaining each tag.
- Added: "Open Published Page" action in the repo sheet (shown only for repos with Pages published); opens the repo's homepage URL if set, otherwise the default `https://<owner>.github.io/<repo>/`.
- Changed: GithubRepo model now includes `has_pages` and `homepage` from the API.

## 2026-07-17 (clone fix)
- Fixed: "Clone to Phone" always failed on repos whose default branch isn't `main` (e.g. `master`) because the download URL hardcoded `main` → HTTP 404. It now downloads via the GitHub API zipball endpoint, which resolves the default branch automatically.
- Fixed: Clone could not fetch private repos (the download used no auth). It now sends the active token's `Authorization: Bearer` header, so private repos clone too. Errors now report the HTTP code (with a hint on 404).

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
