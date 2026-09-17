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


## Approved Android validation — corrected final result

GitHub Actions was explicitly approved for Android build/runtime validation. Exactly three executions occurred. The three-run ceiling has been reached; no fourth execution is permitted in this recovery cycle.

### Attempt 1 — FAILED before APK install
- Run: `35285016158`
- `assembleDebug`: completed successfully.
- API 34 emulator booted.
- Smoke harness failed because the emulator action invoked the script through `/bin/sh`, where `set -o pipefail` was unsupported.
- APK install was never reached.
- Diagnostic steps were configured with `continue-on-error: true`; their green step presentation is not accepted as proof of diagnostic PASS.

### Attempt 2 — FAILED before APK install
- Run: `35285454728`
- `assembleDebug`: completed successfully.
- API 34 emulator booted.
- Smoke harness failed because the action executed script lines in separate shells; the local `APK` variable did not survive to the following command.
- APK install was never reached.
- Diagnostic steps were configured with `continue-on-error: true`; their green step presentation is not accepted as proof of diagnostic PASS.

### Attempt 3 — workflow SUCCESS, Android runtime PASS, V5 quality gates FAIL
- Run: `35286019461`
- Head: `a17c0a0e067a801412b33c0ffead1e0aaa33f140`
- Workflow conclusion: SUCCESS.
- Important: the workflow conclusion is not a full CLEAN PASS because diagnostic test steps intentionally used `continue-on-error: true`.

#### Android/build checks actually PASS
- `assembleDebug`: PASS.
- API 34 emulator boot: PASS.
- APK install: PASS; log contains `Performing Streamed Install` then `Success`.
- MainActivity cold launch: PASS; `Status: ok`.
- Activity observed: `com.futuretech.poweruser/.MainActivity`.
- app PID remained alive after launch.
- activity stack contained MainActivity.
- UIAutomator window dump completed and contained the coding-book shelf UI.
- launch crash scan found no `FATAL EXCEPTION` or `Process: com.futuretech.poweruser` crash pattern.
- runtime gate: `RECOVERY_INSTALL_LAUNCH_GATE=PASS`.

#### V5 content audit — FAIL
Artifact `diagnostic_summary.txt` records:
- `V5_AUDIT_OUTCOME=failure`
- `V5_AUDIT_EXIT=1`

Artifact `v5_content_audit.json` records:
- active V5 directories: `track_01`, `track_02`, `track_07`, `track_10` only.
- part counts: T01=105, T02=105, T07=15, T10=15.
- semantic chars: T01=1,429,424; T02=521,621; T07=129,211; T10=120,770.
- error_count=2,555.
- long_duplicate_groups=0.
- twelve_token_duplicate_ratio=0.00018204.

The missing TRACK 03/04/05/06/08/09/11 directories are an explicit audit error.

#### Targeted V5 tests — FAIL
Artifact `diagnostic_summary.txt` records:
- `V5_TARGETED_TEST_OUTCOME=failure`

Observed targeted failures include:
1. `V5BookReaderSourceContractTest.V5RepositoryMergesManifestShardsBeforeValidatingTheBook`
   - the test expects the literal source string `context.assets.list(trackDir)`, while the current repository implementation reaches asset listing through the `listSnapshotFiles` helper and uses `context.assets.list(dir)`. This is a stale/string-contract test mismatch, but it remains a real failing test until corrected and rerun.
2. `V5PrerequisiteConceptContractTest.everyPrerequisiteNamesARealEarlierSection`
   - actual content defect: T01-P23 references nonexistent prerequisite `T01-P16-S17-deadline-time`.

#### Full debug unit tests — FAIL
Artifact `diagnostic_summary.txt` records:
- `FULL_UNIT_TEST_OUTCOME=failure`

Four failing JUnit suites / nine failures were found:
- `V5BookReaderSourceContractTest`: 1 failure.
- `V5PrerequisiteConceptContractTest`: 1 failure.
- `V5BookScaleCorpusGateTest`: 4 failures.
- `V5EditorialDensityGateTest`: 3 failures.

Important scale failure:
- TRACK 01 measured by the literal ×5 JUnit gate: actual 1,436,170; minimum 1,455,630; short by 19,460.
- all eleven V5 track directories are required, but only 01/02/07/10 exist in this recovery branch.

Editorial failures include:
- banned filler/meta-teaching phrases still present in active V5 content.
- excessive code/example ratio in T02 P64/P65.
- shallow prose chapters below the 420-character chapter floor in multiple T01/T02 parts.

### Artifact evidence
- Actions artifact: `v5-recovery-android-validation`
- Artifact id: `10524517017`
- Artifact ZIP SHA-256 reported by GitHub: `6d979daee23c357e7b271fbc6153d402fe75931a87fd0f11c47c2f78e8752d13`
- Extracted debug APK SHA-256: `1e3990abd6f65fe704536ff0447407f3851b87229722c098d2b85a58200fe10a`

### Final verified status
- Hybrid reader wiring: VERIFIED.
- Existing recovery corpus reader connection for T01/T02/T07/T10: VERIFIED to the static/runtime scope described above.
- Android debug APK build: PASS.
- API 34 APK install + MainActivity launch + process survival + basic UI presence + launch crash gate: PASS.
- V5 content audit: FAIL.
- targeted V5 tests: FAIL.
- full unit test suite: FAIL.
- complete 11-TRACK corpus: FAIL / incomplete.
- frozen-baseline ×5 requirement: FAIL.
- full app CLEAN PASS: NOT ACHIEVED.

No deployment, release, production change, canonical/main merge, or Netlify operation was performed.

Per the three-execution ceiling, Actions validation stops here. Further fixes may be prepared without Actions, but another Actions execution requires a new explicit approval and a new validation cycle.


## 11-TRACK fresh-install reader verification — 2026-09-18

Goal: verify that a freshly installed recovery APK exposes and opens all 11 TRACKs using current V5 books where available and the existing V4/V3 compatibility reader elsewhere. This is a reader-availability verification, not a claim that every TRACK has finished V5 corpus.

### Exact routing verified
- TRACK 01: V5 reader
- TRACK 02: V5 reader
- TRACK 03: V4/V3 compatibility reader
- TRACK 04: V4/V3 compatibility reader
- TRACK 05: V4/V3 compatibility reader
- TRACK 06: V4/V3 compatibility reader
- TRACK 07: V5 reader
- TRACK 08: V4/V3 compatibility reader
- TRACK 09: V4/V3 compatibility reader
- TRACK 10: V5 reader
- TRACK 11: V4/V3 compatibility reader

The installed APK contains V1/V2/V3 textbook assets for TRACK 01 through TRACK 11. Bundled V5 asset directories are exactly track_01, track_02, track_07, and track_10.

### Actions attempts
1. Run 35288203016 — FAIL before runtime because the global V5 audit correctly rejected the incomplete 11-track V5 corpus. Emulator test was not run.
2. Run 35288267014 — FAIL before runtime because the full unit suite contains final-V5 completeness/quality gates that currently fail. Emulator test was not run.
3. Run 35288590440 — SUCCESS for the fresh-install 11-track reader gate after preserving those final-V5 failures as diagnostics rather than misclassifying them as an app-launch blocker.

### Run 35288590440 actual runtime evidence
- workflow conclusion: SUCCESS
- app + instrumentation APK compile gate: PASS
- API 34 emulator: PASS
- instrumentation test suite: com.futuretech.poweruser.textbook.AllTracksReaderRoutingInstrumentedTest
- test: allElevenTracksOpenInTheInstalledReader
- tests=1, failures=0, errors=0, skipped=0
- exact APK reinstall: PASS (adb install: Success)
- MainActivity cold launch: Status: ok
- observed activity: com.futuretech.poweruser/.MainActivity
- runtime gate: ALLTRACKS_RUNTIME_GATE=PASS
- APK signature verification: APK Signature Scheme v2 = true

### Exact tested APK
- file: Coding_Textbook_Recovery_11TRACKS_20260918.apk
- versionName: 1.2.16
- versionCode: 19
- size: 56,879,921 bytes
- SHA-256: 74da4cc496807741961f60fad93b32dfa39c533374a4ab954f2ecefc731ad7d6
- Actions artifact id: 10525501914

### Final-V5 completeness remains FAIL
The same run deliberately retained the full unit-suite diagnostics. 622 tests ran and 9 failed, including final V5 scale/density/prerequisite/source-contract gates. The V5 content audit reports only track_01, track_02, track_07, track_10 as current V5 directories and error_count=2555. Therefore this result must not be described as all 11 TRACKs being complete V5 books or meeting the frozen-baseline ×5 target.

No deployment, release, production change, or main/canonical merge was performed.
