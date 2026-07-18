# GHManager — Closed-Loop Manual Test Cycle

Assumes: at least one token is already saved, a default clone folder is chosen,
and you are on the REPOS tab (with one account). Every cycle ends where it starts:
**create → exercise → delete**. Repeat for each feature you want to re-verify.

---

## Phase 0 — Start state
1. Open the app. Confirm a token is active (top-right shows its name, not "No token").
2. Confirm the REPOS tab is visible (Create Repo / History / REPOS).
3. Note the current repo count on the REPOS tab.

---

## Phase 1 — Create
4. Go to **Create Repo** tab.
5. Enter a unique test name (e.g. `ghm-test-<date>`).
6. Add a short description.
7. Toggle **Private** on, then off (confirm toggle works).
8. Tap **Create Repo**.
9. Confirm a success note appears (top, below tabs).
10. Go to **REPOS** tab → Refresh → confirm the new repo appears with the **PUB** (green) tag.

---

## Phase 2 — Open in browser
11. On the new repo, open its action sheet.
12. Tap **Open in Browser** → confirm it opens the GitHub page in a browser. Return to app.

## Phase 3 — Tags / legend
13. Confirm the repo row shows the **PUB** tag (green = public).
14. Tap the **?** icon (left of Refresh) → confirm the tag legend dialog opens and explains PUB / PRIV / PAGES.
15. Dismiss the legend.

## Phase 4 — Clone to phone
16. In the repo sheet, tap **Clone to Phone (download zip)**.
17. If prompted, pick the save folder (first run) → confirm clone runs.
18. Wait for success note → open the save folder on the device → confirm an extracted folder named `ghm-test-<date>` exists with repo contents.

## Phase 5 — Visibility toggle (PRIV tag)
19. In the sheet, change visibility to **Private** (or use rename/visibility action).
20. Refresh REPOS → confirm the tag is now **PRIV** (red).
21. Change it back to **Public** → Refresh → confirm **PUB** (green) again.

## Phase 6 — Rename
22. In the sheet, tap **Rename** → set name to `ghm-test-<date>-renamed`.
23. Refresh → confirm the new name appears; old name is gone.

## Phase 7 — Publish to GitHub Pages (PAGES tag)
24. In the sheet, tap **Publish to GitHub Pages**.
25. Wait for success → Refresh → confirm the **PAGES** (orange) tag appears and the repo shows a Pages URL.
26. Tap **Open Published Page** → confirm it opens the live Pages site (may take a minute to go live).

## Phase 8 — History
27. Go to **History** tab → confirm it logs the Create, Clone, visibility, rename, and Publish actions for this repo.

## Phase 9 — Mid-action switch guard
28. Start a Clone on the repo, then try to switch the active token mid-action.
29. Confirm the "Action in progress" warning appears. Tap **Cancel**.
30. Let the clone finish → success note appears.

---

## Phase 10 — Delete (close the loop)
31. In the repo sheet, tap **Delete**.
32. Confirm the 3-second countdown confirmation appears → wait and confirm.
33. Refresh REPOS → confirm the repo is removed.
34. Go to **History** → confirm a DELETE entry was logged.
35. Repo count on REPOS tab matches the Phase 0 count.

---

## Closed-loop check
- The app is back to the same start state (no leftover test repos).
- History contains the full create→...→delete trail.
- No lingering error notes. Cycle can be repeated from Phase 1.

## Quick smoke (if short on time)
Run a minimal loop: Phase 1 (create) → Phase 2 (open browser) → Phase 4 (clone) → Phase 10 (delete). This still exercises the create/clone/delete core and leaves no residue.
