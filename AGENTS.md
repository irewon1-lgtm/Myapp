# Chatbook maintenance

Only `irewon1-lgtm/Myapp` branch `chatbook-app-20260923` and the existing https://chatbook-library-20260923.netlify.app site are in scope. Do not change main or other apps. Preserve existing books, IDs, notes, bookmarks, sync data and reader UI.

## One current authoring source

Read `/engine-spec.json` for a new book. It contains the current rules AND one representative excerpt in one response. Save its contentDigest and snapshot with the job; resume that snapshot, not a newer or remembered version. `engine/cli.mjs prepare` saves the snapshot, `spec JOB_DIR` reads it, and old jobs are not silently migrated. A book-only request does not modify common policy. Explicit user requests can revise the current book or future policy; explain that scope.

Edit `public/engine/learning-policy.json` authoring in place. Replace changed rules and the relevant example; remove superseded requirements. Do not append historical exceptions, copy rules into another source, repeatedly summarize the whole policy, or regenerate unchanged books. Runtime policy in learning.mjs/defaults and the policy manifest must still agree. Verify the example's meaning when writing style changes. Old versions stay in Git history, outside normal authoring input. Current means the actually published bundle, including after rollback, not the highest version number.

`npm run build` generates current spec JSON/HTML, engine-info JSON/HTML, llms.txt and version.json from the same source with fresh actual npm tests. The generator is idempotent. Never edit generated output. Runtime defaults are configuration, not evidence of successful long-video generation. Structural tests/hash consistency do not certify translation, prose, complete source access or reader understanding. Unperformed tests stay unverified.

`node scripts/verify-release.mjs` rejects broken/mixed bundles and stale evidence. Do not impose page counts, paragraph counts or fixed per-concept templates as publication gates. Editorial issues require local editing, not infinite retries or full-book rewrites. A failed release keeps the previous site and drafts.

## Safe publication and storage

Use `.github/workflows/chatbook-fast-publish.yml` / `scripts/fast-publish.mjs` for production. It checks out the source commit, serializes with `chatbook-production`, and compares the current branch's app source before publishing. One-use handoff/current-result files are replaced, not accumulated; they are never public app content. Do not revive historical one-off installers as a normal publishing route. Resolve concurrent source changes before retrying; never force-push over others. A maintainer can change these safeguards, so they are not a platform permission boundary.

Publish/rollback the complete tested bundle. Compare live release and spec identities with tested artifacts. Metadata is read-only, not a reader runtime dependency, generation service or grant of edit/deploy authorization. Never include keys, credentials, user records or environment dumps in public metadata/artifacts.

Keep all metadata aliases network-only and no-store/revalidation protected. Use only the current app cache for offline fallback. Remove only retired `chatbook-offline-*` generated caches after normal worker activation; do not force a worker into an open reader. Never clear localStorage, IndexedDB, unrelated caches or saved user downloads to perform an upgrade. Preserve these boundaries when changing the pipeline.
