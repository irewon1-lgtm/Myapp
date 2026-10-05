// Pending work is bounded independently of the 30-day price history.
const WINDOW_MS = 6 * 60 * 60 * 1000;
const list = value => [].concat(value ?? []).filter(Boolean);
export function canonicalKey(item) {
  if (item.id && item.id !== 'unknown') return String(item.id);
  const url = new URL(item.url, 'https://www.daangn.com');
  url.search = ''; url.hash = '';
  return url.origin + url.pathname.replace(/\/+$/, '');
}
export function mergeListings(map, item) {
  const key = canonicalKey(item), old = map.get(key);
  if (!old) { map.set(key, { ...item, sourceRegion: list(item.sourceRegion), sourceQuery: list(item.sourceQuery), sourceCities: list(item.sourceCities) }); return; }
  for (const field of ['sourceRegion', 'sourceQuery', 'sourceCities']) old[field] = [...new Set([...list(old[field]), ...list(item[field])])];
  for (const field of ['postedAt', 'boostedAt', 'location', 'regionPath']) if (!old[field] && item[field]) old[field] = item[field];
}
export function migrateState(state) {
  const grouped = new Map();
  for (const item of Object.values(state.items ?? {})) {
    if (!item?.url) continue;
    const key = canonicalKey(item);
    if (!grouped.has(key)) grouped.set(key, []);
    grouped.get(key).push(item);
  }
  state.items = {};
  for (const [key, rows] of grouped) {
    rows.sort((a,b) => new Date(b.lastSeenAt ?? 0) - new Date(a.lastSeenAt ?? 0));
    const newest = { ...rows[0] };
    newest.firstSeenAt = rows.map(x=>x.firstSeenAt).filter(Boolean).sort()[0];
    newest.postedAt ??= rows.find(x=>x.postedAt)?.postedAt ?? null;
    // A completed observation at the same or a later time wins over stale pending.
    const pending = rows.find(x=>x.pendingDetailEvent);
    if (pending && !rows.some(x=>!x.pendingDetailEvent && new Date(x.lastSeenAt) >= new Date(pending.lastSeenAt))) newest.pendingDetailEvent = pending.pendingDetailEvent;
    newest.sourceCities = [...new Set(rows.flatMap(x=>[...list(x.sourceCities), x.city]).filter(Boolean))];
    newest.sourceRegion = [...new Set(rows.flatMap(x=>list(x.sourceRegion)))];
    state.items[key] = newest;
  }
  state.version = 2;
  return state;
}
export function clearTask(saved) {
  saved.pendingNewnessCheck = false; saved.pendingDetailEvent = null;
  saved.newnessProbeSkipped = false; saved.pendingReason = null;
  saved.nextEligibleAt = 0; saved.attempts = 0; saved.lastError = null;
}
export function expireTasks(state, now) {
  for (const saved of Object.values(state.items)) {
    const age = now - new Date(saved.firstSeenAt ?? 0).getTime();
    if (saved.pendingNewnessCheck && (!Number.isFinite(age) || age > WINDOW_MS || saved.postedAt)) {
      // Date resolution is classified during search; retain recent resolved work.
      if (!saved.postedAt || age > WINDOW_MS) {
        saved.pendingNewnessCheck = false; saved.newnessUnknownExpired = true;
      }
    }
    if (saved.pendingDetailEvent?.changeType === 'new' && age > WINDOW_MS) saved.pendingDetailEvent = null;
    if (saved.detailTerminal || (!saved.pendingNewnessCheck && !saved.pendingDetailEvent)) {
      saved.pendingReason = null; saved.newnessProbeSkipped = false;
      if (saved.detailTerminal) { saved.pendingNewnessCheck = false; saved.pendingDetailEvent = null; }
    }
  }
}
export function priority(item) {
  if (item.freshTask && (item.changeType === 'price_drop' || item.changeType === 'new')) return 0;
  if (item.freshTask && item.newnessHint === true) return 0;
  if (item.freshTask) return 1;
  return item.lowPriority ? 3 : 2;
}
export function retryTask(saved, error, now) {
  saved.attempts = (saved.attempts ?? 0) + 1;
  saved.lastAttemptAt = new Date(now).toISOString(); saved.lastError = error;
  const terminal = /HTTP (404|410)/.test(error) || saved.attempts >= 12 ||
    (/POSTED_AT_UNKNOWN/.test(error) && saved.attempts >= 3) ||
    (!/HTTP (429|403)/.test(error) && saved.attempts >= 6);
  if (terminal) {
    const attempts = saved.attempts;
    clearTask(saved); saved.attempts = attempts; saved.lastError = error;
    saved.detailTerminal = true; saved.newnessUnknownExpired = /POSTED_AT_UNKNOWN/.test(error);
  } else saved.nextEligibleAt = now + Math.min(WINDOW_MS, (/HTTP (429|403)/.test(error) ? 3600000 : 60000) * 2 ** (saved.attempts - 1));
}
export async function runDetailPool(queue, deadline, fetchDetail, { workers = 3, now = Date.now } = {}) {
  // Coalescing is per scan, including failures: HTTP retries happen in later scans.
  const unique = new Map();
  for (const item of queue) mergeListings(unique, item);
  const tasks = [...unique.values()].sort((a,b)=>priority(a)-priority(b));
  let cursor = 0, stopped = false;
  const promises = new Map(), results = [];
  await Promise.all(Array.from({ length: workers }, async ()=> {
    while (!stopped && cursor < tasks.length && now() < deadline) {
      const item = tasks[cursor++], key = canonicalKey(item);
      if (!promises.has(key)) promises.set(key, Promise.resolve().then(()=>fetchDetail(item)));
      const result = await promises.get(key); results.push(result);
      if (/HTTP (429|403)/.test(result.detailError ?? '')) stopped = true;
    }
  }));
  return results;
}
