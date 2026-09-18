# CodingCoding textbook LIVE policy

This branch is the **only** runtime content source for the CodingCoding learning app.

## Fixed identity

- Repository: `irewon1-lgtm/Myapp`
- Branch: `codingcoding-textbook-live`
- Runtime root: `app/src/main/assets/textbook/v5`
- Project id: `codingcoding`
- Content id: `codingcoding-textbook-v5`

The app must validate `app/src/main/assets/textbook/v5/project_lock.json` before accepting any TRACK content.

## Isolation rule

Do not copy or merge textbook content from another chat, project, experimental branch, recovery branch, preview branch, or legacy content directory into this LIVE branch unless the user explicitly chooses that exact content for CodingCoding.

Other branches may be used for experiments, but the installed app must not read them.

## Update rule

Normal textbook edits do **not** require an app-structure change or APK rebuild.

For an existing TRACK, edit only its live content files:

- `track_XX/manifest.json`
- `track_XX/manifest_NN.json`
- `track_XX/*.md`
- `track_XX/*_sources.json`

The runtime lists the TRACK directory, calculates a signature from Git blob SHAs, and refreshes the device cache when any live file changes. PART additions, removals, reordering, title edits, and prose edits are driven by the manifest and content files.

Only runtime/schema changes require app code changes.

## Cache isolation

The app uses a CodingCoding-specific cache and preferences namespace. Legacy V5 caches are intentionally ignored so content from an older branch cannot leak into the CodingCoding reader.
