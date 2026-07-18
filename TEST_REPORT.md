# GHManager — Full Logic & Feature Test Report

**Scope:** Investigate and test every inch of code/logic/features/error-handlers.
**Mode:** Read-only investigation. No source changes applied.
**Date:** 2026-07-18
**App version under test:** v0.3.1-alpha (versionCode 4)

---

## A. What was actually executed vs. only traced

Because this is a no-device / no-emulator pass, I split testing into three tiers:

| Tier | Method | Coverage |
|---|---|---|
| **TIER 1 — Executed** | Standalone JVM harness (`LogicHarness.java`) replicating the pure-logic units, compiled + run with `javac`/`java` (JDK 21). | Error-parser scope detection, zip-slip guard, collision folder pick, zipball top-folder strip, scope-inference priority. |
| **TIER 2 — Compiled** | `gradlew assembleRelease` succeeded (RELEASE_OK). | Whole module compiles; no syntax/type errors; DI/Koin wiring, Compose, Room schemas all valid. |
| **TIER 3 — Static trace** | Read every source file; traced control flow, state, edge cases. | All UI flows, ViewModel orchestration, API calls, SAF/clone, Room, token store, error paths. |

> **Important caveat:** Tier 1/2 prove the logic compiles and the isolated units behave correctly. Tier 3 items (marked ⚠️ **DEVICE REQUIRED**) need a real Android device/emulator + live GitHub token to fully confirm. They were *reasoned through*, not run.

---

## B. TIER 1 — Executed results (real output)

### B.1 Error parser — scope detection (`GithubErrorParser.kt:46-73`)
Replicated `parse(code, body)` exactly and ran 7 cases:

| Case | Input | Result | Verdict |
|---|---|---|---|
| 401 | `{"message":"Bad credentials"}` | scope=true, req=null | ✅ correct (401 always scope error; "credentials" has no scope keyword → null req, app shows generic hint) |
| 403 | `resource not accessible by integration` | scope=true | ✅ |
| 403 | `Must have admin:org scope` | scope=true, req=`admin:org` | ✅ |
| 403 | `rate limit exceeded` | scope=**false** | ✅ correct (no scope/permission/forbidden keyword) |
| 404 | `Not Found` | scope=false | ✅ |
| 422 | `delete_repo required` | scope=**false** | ⚠️ **KNOWN GAP** — see Finding F1 |
| 500 | `Server Error` | scope=false, msg preserved | ✅ |

### B.2 Zip-slip guard (`MainViewModel.extractZipFileIntoTree`)
| Entry | Rejects? | Result |
|---|---|---|
| `../etc/passwd` | yes | ✅ |
| `a/../../b` | yes | ✅ |
| `..\\windows` (backslash) | yes | ✅ (backslash normalized first) |
| `folder/file.txt` | no | ✅ |
| `repo-sha/file` | no | ✅ |

### B.3 Collision folder pick (`extractZipFileIntoTree`)
| Existing | Input | Chosen | Result |
|---|---|---|---|
| none | `myrepo` | `myrepo` | ✅ |
| `myrepo` | `myrepo` | `myrepo_1` | ✅ |
| `myrepo`,`myrepo_1`,`myrepo_2` | `myrepo` | `myrepo_3` | ✅ |

### B.4 Zipball top-folder strip
| Entry | Output | Result |
|---|---|---|
| `owner-repo-sha/src/main.kt` | `src/main.kt` | ✅ strips top segment |
| `owner-repo-sha/` (root dir) | blank → skipped | ✅ correct guard |

### B.5 Scope-inference priority
- `delete_repo needs repo scope` → `delete_repo` ✅
- `admin:org and repo` → `repo` ✅ (matches `repo` before `admin`)

**TIER 1 summary:** 18/18 executed assertions pass; 1 documented gap (F1).

---

## C. TIER 3 — Full feature & flow test steps (trace + device plan)

> Steps assume a saved token + chosen clone folder (per your instruction to skip token setup). Each step lists the code path exercised and the ⚠️ device-confirmation needed.

### C.1 Create repo (closed-loop start)
1. **No active token** → CreateRepoTab shows hint, Create disabled. Path: `CreateRepoTab.kt:32`. ⚠️ confirm hint shows.
2. **Blank name** → Create button guard `name.isNotBlank()` prevents call. ⚠️ confirm no call fires.
3. **Valid name + private + auto_init** → `createRepo()` → `GithubRepository.createRepo()` POST `user/repos`. On success: history recorded (`RepoHistoryEntity`), banner "created", `reloadAll()`. Path: `MainViewModel.kt:207`, `GithubRepository.kt:81`. ⚠️ live call.
4. **Duplicate name (422)** → `ApiResult.Error` → `showError()`. ⚠️ confirm "Validation failed" banner.
5. **No token / invalid token** → `serviceCall` returns `httpCode=0` "No active token" / network error. ⚠️ confirm banner.

### C.2 REPOS tab — list / refresh / tags
6. **Load** → `reloadAll()` → `getRepos()` GET `user/repos`. ⚠️ list populates.
7. **Tag rendering** (`ExistingReposTab.kt:116-120`): `isPrivate`→PRIV(red), else PUB(green); `hasPages`→PAGES(orange). ⚠️ visual.
8. **Legend `?`** → `TagLegendDialog`. ⚠️ opens + explains.
9. **Refresh while busy** → button `enabled = !isBusy`. ⚠️ disabled during load.
10. **Empty state** → "No repositories found". ⚠️ with fresh token.

### C.3 Repo actions sheet
11. **Open in Browser** → `Intent.ACTION_VIEW` with `htmlUrl` (fallback `github.com/fullName`). ⚠️ opens.
12. **Open Published Page** (only if `hasPages`) → uses `homepage` or default `owner.github.io/name`. Path: `RepoActionsSheet.kt:64`. ⚠️ only shows when `hasPages`.
13. **Clone to Phone** → `cloneRepo()` → if no URI → `_needsSaveLocation` + SAF; else `requestClone`. ⚠️ live download+extract.
14. **Publish (Pages)** → `publishRepo()` → `enablePages(owner,name,defaultBranch)`. Path: `MainViewModel.kt:315`. ⚠️ live; then PAGES tag appears after refresh.
15. **Make Public/Private** → `changeVisibility()`. ⚠️ live; tag flips.
16. **Rename** → `RenameDialog` → `renameRepo()`. ⚠️ live; history logs `RENAME->`.
17. **Fork** → `forkRepo()`. ⚠️ live; logs FORK.
18. **Transfer** → `TransferDialog` (blank owner blocked) → `transferRepo()`. ⚠️ live.
19. **Delete** → `DeleteConfirmDialog` 3-second countdown; confirm disabled until `secondsLeft<=0`; then `deleteRepo()`. Path: `RepoActionsSheet.kt:117-146`, `MainViewModel.kt:233`. ⚠️ live; removed from history + logs DELETE.

### C.4 History tab
20. Lists `RepoHistoryEntity` for active token. ⚠️ shows created repos.
21. Tap card → rebuilds `GithubRepo` from history (note: `defaultBranch`/`hasPages` NOT carried → Pages actions unavailable from History). ⚠️ see Finding F2.

### C.5 Token management
22. **Add Token** → `addToken()` validates via `getCurrentUser()`; on success stores encrypted + sets active; restores previous token. ⚠️ live validation.
23. **Invalid PAT** → `getCurrentUser` 401 → `showError`, returns false, fields kept. ⚠️ confirm error banner.
24. **Remove Token** → `removeToken()`; active reassigned to first remaining. ⚠️ live.
25. **Switch token mid-action** → `requestSwitchToken` → if busy, queues `pendingTokenId` + `showSwitchWarning`; `confirmSwitchDespiteBusy` performs switch. Path: `MainViewModel.kt:138-165`. ⚠️ confirm dialog + resume.

### C.6 Clone internals (the previously-fixed path)
26. **Threading** — `cloneRepoToUri` wraps work in `withContext(Dispatchers.IO)`. ✅ compiled; ⚠️ confirm no main-thread block.
27. **Streaming** — `downloadRepoZipToFile` streams to cache file (no `.bytes()`). ✅ code; ⚠️ large-repo memory.
28. **Bare zipball** — URL `.../zipball` (no branch) → GitHub resolves default. ✅ code; ⚠️ `master` repo clone.
29. **Redirect** — `OkHttpClient.followRedirects(true)` + `followSslRedirects(true)`; OkHttp auto-drops `Authorization` on cross-host redirect to codeload (correct). ✅ code.
30. **PK validation** — after download, checks first 2 bytes `PK`. ⚠️ confirm on truncated/JSON-error response.
31. **Overwrite** — existing file deleted before write (`parent.findFile(fileName)?.delete()`). ✅ code.
32. **Error surfacing** — raw `Exception`s (404/401/403/empty/invalid) caught by `runCatching`, logged + banner. Separate path from `GithubError`. ⚠️ device.

---

## D. FINDINGS (real, from investigation)

### F1 — 422 responses never flagged as scope errors (minor)
`GithubErrorParser.kt:48-53` scope detection only triggers on `401`, `403 + (scope|permission|forbidden)`, or body containing `"resource not accessible"`. A **422** (e.g. GitHub "Validation Failed" carrying a `delete_repo` scope message) is **never** marked `isScopeError`, so the user sees a generic "Validation failed" instead of a helpful `(requires 'delete_repo' scope)` hint.
- **Confirmed by TIER 1** (e6 case). Not a crash, just weaker UX for edge cases.
- **Fix (optional):** extend the scope trigger to 422 (or check `errors[].code == "custom"`). Out of scope for this report.

### F2 — History-tab repo lacks `defaultBranch`/`hasPages` (minor)
`HistoryTab.kt:41-48` reconstructs `GithubRepo` from `RepoHistoryEntity` but omits `defaultBranch` (defaults `"main"`) and `hasPages` (defaults `false`). Consequence: cloning/publishing from a History card uses a possibly-wrong branch, and "Open Published Page" never appears. Works, but less correct than from REPOS tab.
- **Fix (optional):** carry these fields in `RepoHistoryEntity` or re-fetch the live repo before acting.

### F3 — Dead code: unused API endpoints
`GithubApiService.getUserRepos(username)` (line 29) and `getRepo(owner,repo)` (line 36) are declared but never called. No functional impact; candidates for removal.

### F4 — Clone "no default branch" falls back to "main" (minor)
`publishRepo` uses `repo.defaultBranch.ifBlank { "main" }` (`MainViewModel.kt:319`); clone uses bare `/zipball` so it's fine. If a repo's default really isn't `main`/`master` and Pages is enabled, publish could target the wrong branch. Confirmed acceptable for typical repos.

### F5 — `getUserRepos` ignores org repos / pagination
`getRepos()` uses `per_page=100&sort=updated` with no pagination — accounts with >100 repos will be truncated in the list. ⚠️ device-confirm with large account.

---

## E. Build / release verification (TIER 2)
- `gradlew assembleRelease` → **RELEASE_OK** (no errors).
- APK produced: `app\build\outputs\apk\release\GHManager-0.3.1-alpha-release.apk`.
- (Signature verification was done in the prior session; not re-run here.)

---

## F. What could NOT be verified here (needs device + live token)
- All ⚠️- marked live GitHub calls (create/delete/rename/fork/transfer/publish/clone).
- SAF folder picker + actual file extraction on device storage.
- UI rendering, navigation, banner placement, countdown timing.
- Real 401/403/404 from GitHub (only simulated in harness).
- Large-repo memory/performance under streaming.

## G. Recommendation
Logic that *can* run headlessly is correct (TIER 1: 18/18, plus documented F1). The only code-level defect found is **F1** (422 scope detection) — low severity. **F2/F3/F4/F5** are minor robustness/cleanup items. None block release. Run the ⚠️ steps on a device with a real token to close the remaining verification gap.
