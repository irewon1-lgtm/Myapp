# TEXTBOOK V5 recovery status — 2026-09-18

This file records only checks that were actually performed. It is not a full-app PASS report.

## Recovery branch

- Branch: `textbook-v5-recovery-20260918`
- Canonical base observed before recovery: `textbook-v5-deep-book-engine` @ `d79b2cbf1b5bfaf736cc9ad077b34a53ba2f0cd1`
- Functional recovery commit before this status note: `c0bbe871f81deb5b891eab3ad41fc766f7e8bcf7`
- Recovery ref update used `force=false`.

## V5 corpus actually present on recovery branch

- TRACK 01: subtree `bbe0f0b5b5a8c56f2032658a1fbdb8427dc195aa`; P105 asset/source exist and `manifest_07.json` connects P99–P105.
- TRACK 02: subtree `4088ba082a7cbdb64e88a278ec8eed1128efac80`; preserved byte-identical to canonical; `manifest_11.json` connects P101–P105.
- TRACK 07: recovery subtree `ae4810f703eebfd7b962e053e7491e294dbd6360`; original P10–P15 assets existed but were not in the manifest. Recovery adds `manifest_02.json` connecting P10–P15.
- TRACK 10: subtree `4d25e4134eb504cc78c939b7dc273689e9b79bd5`; preserved byte-identical to canonical; current Git corpus is 15 BLOCK/PART files and manifest P01–P15.

No fake V5 directories were created for TRACK 03/04/05/06/08/09/11.

## Hybrid reader integration actually applied

- INTERNET permission is present.
- `android:name=.PowerUserApplication` is present.
- Hybrid `V5BookAssetRepository` is included and reads `manifest.json` plus `manifest_XX.json` shards.
- Hybrid `AdaptiveTextbookScreen` is included.
- One-time recovery cache migration deletes the old V5 overlay once, records the integrated bundled revision, and then allows later verified overlays to persist normally.
- `channel.json` records recovery content baseline `41184044e91c98ba9e49c45f71e2445c464b928d` and TRACK 01/02/07/10 manifest metadata.

Important limitation: the hybrid repository's live remote polling URL still targets `textbook-v5-deep-book-engine`. The recovery branch itself is therefore not claimed as the live hot-update source. This should only be promoted after canonical integration is separately approved and verified.

## Recovered Library evidence that is not yet Git-integrated

### TRACK 05

Library artifacts exist:

- `TRACK05_APP_OVERLAY_FREE.zip`
- `TRACK05_REBUILD_P01_P105_CHECKPOINT.zip`
- `P01_P105_VALIDATION.json`
- `TRACK05_CHECKPOINT_STATUS.md`

The readable validation records:

- PART 105
- CHAPTER 630
- learner-facing chars 541,933
- local P01–P105 static validation PASS
- repeated-shingle hits 0
- final frozen ×5 target 1,455,630
- remaining 913,697
- final ×5 scale gate FAIL
- Android/Gradle/build/install/device NOT RUN

The binary ZIP exists in the Library, but this session's Files runtime denied raw-byte materialization and returned no readable ZIP content. Therefore its manuscript bytes were not guessed or fabricated into Git. The old Git payload branch also lacks payload parts 15–20, so it is not a valid substitute for the complete ZIP.

### TRACK 03

`TRACK03_CURRICULUM_MAP_105.md` exists. Its status map says P001–P091 AUTHORED and P092–P105 PLANNED. Therefore TRACK 03 is not claimed complete.

### TRACK 10 additional depth

`T10_P16_P25_READY.zip` exists in the Library but raw-byte materialization is unavailable in this session. Git commit search found no P16–P25 commit, and the current TRACK 10 Git subtree contains only the 15 BLOCK files. The ZIP was therefore not guessed into Git.

### TRACK 04/06/08/09/11

No recoverable V5 corpus artifact was found in the accessible Git branches/recent Library artifact inventory during this recovery pass. These tracks are not claimed recovered.

## CLEAN PASS results actually verified

1. Branch ancestry/no overwrite: recovery is based on the observed canonical head; no force update was used.
2. Workflow trigger safety: canonical workflows were inspected before write; recovery branch is not in their push allowlists.
3. Actions execution after recovery write: queried recovery-branch workflow runs and observed zero runs before this status-note commit. Recheck required after this note.
4. Canonical preservation: TRACK 02 and TRACK 10 subtree SHAs match canonical exactly.
5. TRACK 01 P105 existence + manifest shard connection: verified.
6. TRACK 02 P105 manifest shard connection: verified.
7. TRACK 07 P10–P15 missing-manifest defect: detected, repaired with `manifest_02.json`, then refetched from the recovery branch.
8. TRACK 10 current end-state: P01–P15 only in Git; no claim of P105 PARTs.
9. Reader manifest-shard support: source code inspected; shards are merged and contiguous part/order rules are enforced.
10. Android application wiring: manifest and application/reader blobs refetched from the recovery branch.
11. Missing-track honesty gate: no placeholder/fake TRACK 03/04/05/06/08/09/11 directories created.
12. Library recovery gate: TRACK05/T10 ZIP existence verified, raw bytes unavailable; integration intentionally stopped rather than inventing content.

## NOT RUN / NOT PASS

- Full Gradle: NOT RUN
- Android APK build from this recovery branch: NOT RUN
- APK install/launch: NOT RUN
- emulator/device page/swipe/resume: NOT RUN
- deployment/release/production change: NOT RUN
- main/canonical merge: NOT RUN

Therefore this branch is a verified static recovery candidate, not a verified full-app release.
