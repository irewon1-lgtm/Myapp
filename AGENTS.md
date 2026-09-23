# Chatbook maintenance rules

Scope: this repository's `chatbook-app-20260923` branch and the existing site https://chatbook-library-20260923.netlify.app only. Do not merge this work into main or change the other apps. Preserve books, book IDs, notes, bookmarks, sync data, and the current reader UI unless the user explicitly requests those changes.

## Generated engine information

`npm run build` must generate `dist/engine-info.json`, `dist/engine-info.html`, `dist/llms.txt`, and `dist/version.json` together. These are read-only information, not a runtime dependency of the reader and not an administration API. Do not edit generated files manually or use a previous deployment's metadata with a new app.

Read the executable `public/engine/learning.mjs` VERSION, PROFILE, POLICY and DEFAULTS. Keep `public/engine/defaults.json` and `public/engine/learning-policy.json` consistent when changing the engine. `core.mjs` is the compatibility API; its version need not equal the current production learning engine. App version comes from package.json. A source fingerprint identifies changes even when a numeric version is unchanged.

Every supported build must run the actual current npm test suite and stop on failure. Only publish fresh, source-bound test evidence. Configuration limits are not measured video success. Never turn structural tests into a claim of semantic fidelity, complete video access, physical Galaxy testing, or production end-to-end success. Unperformed checks stay explicitly unverified.

Keep `node scripts/verify-release.mjs` as the pre-publication gate. App identity, engine identity, public metadata, and published file hashes must agree. Production verification must compare the deployed version.json and engine-info.json to the exact tested sourceDigest. Publish the same bundle or roll back the same bundle; do not update metadata separately.

Metadata URLs must not enter service-worker offline caches; keep no-store/revalidation headers. Missing/unavailable metadata must never prevent books, quizzes or the reader from opening. The publisher should reject missing metadata, but the running reader must not depend on it.

Do not publish API keys, credentials, encrypted handoffs, user notes, connection codes, or environment dumps in metadata, logs, or source archives. Use only explicit non-secret build identifiers. Public URLs do not confer edit/deploy permission.

If a later architecture change requires replacing this build pipeline, migrate its metadata generation, validation, cache exclusions and these maintenance rules in the same change. Do not silently bypass them. A maintainer with write access can change these safeguards; they are not an unchangeable platform permission boundary.
