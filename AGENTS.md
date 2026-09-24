# Chatbook maintenance

Only `irewon1-lgtm/Myapp` branch `chatbook-app-20260923` and the existing https://chatbook-library-20260923.netlify.app site are in scope. Do not change main or other apps. Preserve existing books, IDs, notes, bookmarks, sync data and reader UI.

## User workflow and one current authoring source

The user sends YouTube URLs to ChatGPT, NOT to an AI generation server in this app. Read the actual `/engine-spec.json` for a new book: the current rules AND the representative example. Save its contentDigest and snapshot with the job; resume that snapshot, not a newer or remembered version. `engine/cli.mjs prepare` saves the snapshot and `spec JOB_DIR` reads it. Never silently migrate old jobs. Write the Korean book from obtained source material, then register only authorized content. No Gemini/OpenAI paid generation API is part of this migration. A book-only request does not change common policy.

Edit `public/engine/learning-policy.json` authoring in place. Replace changed rules and the relevant example; remove superseded requirements. Do not append historical exceptions or regenerate unchanged books. Runtime policy/defaults and the manifest must agree. Verify the example's meaning when writing style changes. Old versions stay in Git history, outside normal authoring input. Current means the validated live publication, not untested source. When the current spec cannot be fetched, disclose that limitation rather than silently calling an old copy current.

`npm run build` generates spec JSON/HTML, engine-info JSON/HTML, llms.txt and version.json from the same source with actual npm tests. Never edit generated output. Structural tests/hash consistency do not certify translation, prose, complete video access or reader understanding. Unperformed tests stay unverified. Do not impose arbitrary page/paragraph counts or fixed per-concept templates. Editorial issues require local editing, not infinite retries or full-book rewrites.

## Ordinary publication: GitHub only, no Netlify deploy

Books, chapters, reader UI, CSS, JavaScript and current authoring rules use `.github/workflows/chatbook-live-publish.yml`. It builds, tests with Chromium, creates immutable `live/assets` and `live/release.json`, then atomically advances `live/channel.json`. It does not call Netlify. Actual production HTTPS/offline verification follows publication. Inspect that result and the published identities before reporting completion. Keep source writes atomic where possible, and never force-push over concurrent work.

Legacy production helpers throw immediately. Historical production/migration workflows are archived as text in `docs/disabled-workflows`. Do not restore these as a normal update path. Two initial Netlify deployments were actually used on 2026-09-25 KST: bootstrap installation, then a cold-routing correction. Receipts are `.deployment/live-bootstrap-receipt.json` and `.deployment/live-routing-correction-receipt.json`; never rewrite that history as one or zero. This completed transition does not authorize additional production deployments.

`netlify/**`, `netlify.toml`, `public/live-guard.js`, `public/live-sw.js`, and PWA identity are the frozen bootstrap contract. Ordinary publication must fail if they change. A necessary security/server/loader update requires a separately approved and tested bootstrap deployment. Do not weaken or rewrite this contract to silently turn an ordinary change into a paid deployment.

## Recovery and data boundaries

The fixed same-origin gateway serves only fixed-repository manifest-listed files, verifies size/SHA-256, retains CSP self-only scripts, pins a complete version, and preserves the original origin and sync endpoints. Frozen bootstrap routes must work on a cold CDN without any test-server static shortcut. The guard defers record writes during startup and recovers broken startup to a prior known-good or bundled reader. This is not a guarantee against every later runtime or semantic error.

Offline caching advances only after the whole release is verified. First offline use needs one successful online preparation. Never clear localStorage, IndexedDB, unrelated caches, user downloads, or notes to upgrade. Keep public metadata network-only; an offline cached spec must not pretend to be latest. Never publish credentials, private records, connection codes or environment dumps.

Never silently remove existing book/chapter/block IDs. Explicitly requested structural removals need documented retired anchors in `live/retired-anchors.json`; preserve annotation records. Actual Chromium tests are not physical Galaxy tests, and disposable local test records are not proof of every existing user's sync state. Hosting traffic/compute can still consume credits; no-production-deploy updates do not mean unlimited free hosting.
