import fs from 'node:fs/promises';

const REQUEST_PATH = new URL('./request-fast.json', import.meta.url);
const RESULT_PATH = new URL('./result-fast.json', import.meta.url);
const BASE = 'https://www.daangn.com';
const SEARCH = BASE + '/kr/buy-sell/';
const UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36';

const sleep = (ms) => new Promise(r => setTimeout(r, ms));

function extractBalancedJson(input, startIndex, openChar) {
  const closeChar = openChar === '{' ? '}' : ']';
  let depth = 0, inString = false, escaped = false;
  for (let i = startIndex; i < input.length; i++) {
    const c = input[i];
    if (inString) {
      if (escaped) { escaped = false; continue; }
      if (c === '\\') { escaped = true; continue; }
      if (c === '"') inString = false;
      continue;
    }
    if (c === '"') { inString = true; continue; }
    if (c === openChar) depth++;
    if (c === closeChar) {
      depth--;
      if (depth === 0) return input.slice(startIndex, i + 1);
    }
  }
  return null;
}

function extractJsonAfterMarker(html, marker, openChar) {
  const mi = html.indexOf(marker);
  if (mi < 0) return null;
  const tail = html.slice(mi + marker.length);
  const si = tail.indexOf(openChar);
  if (si < 0) return null;
  const json = extractBalancedJson(tail, si, openChar);
  if (!json) return null;
  try { return JSON.parse(json); } catch { return null; }
}

function articleId(value) {
  const s = String(value || '').trim();
  if (/^[a-z0-9]+$/i.test(s)) return s;
  return s.match(/-([a-z0-9]+)\/?(?:[?#].*)?$/i)?.[1]
    ?? s.match(/buy-sell\/([a-z0-9]+)\/?(?:[?#].*)?$/i)?.[1]
    ?? null;
}

function priceNum(v) {
  if (typeof v === 'number') return Number.isFinite(v) ? v : 0;
  const n = Number.parseFloat(String(v ?? '').replace(/[^0-9.-]/g, ''));
  return Number.isFinite(n) ? n : 0;
}

function normalizeArticle(raw, sourceQuery) {
  const id = articleId(raw.href || raw.id) || 'unknown';
  const url = raw.href
    ? (String(raw.href).startsWith('http') ? raw.href : BASE + raw.href)
    : SEARCH + id + '/';
  return {
    id,
    title: raw.title ?? '제목 없음',
    price: priceNum(raw.price),
    url,
    imageUrl: raw.thumbnail ?? undefined,
    location: raw.locationName ?? raw.region?.name,
    regionPath: [raw.region?.name1, raw.region?.name2, raw.region?.name3].filter(Boolean).join(' ') || undefined,
    regionSlug: raw.region?.name && raw.region?.dbId ? raw.region.name + '-' + raw.region.dbId : undefined,
    postedAt: raw.createdAt,
    boostedAt: raw.boostedAt,
    chatCount: raw.chatCount ?? 0,
    favoriteCount: raw.favoriteCount ?? 0,
    status: raw.status ?? 'Ongoing',
    sellerName: raw.user?.nickname,
    mannerTemperature: raw.user?.score,
    sourceQuery
  };
}


function parseRegionLinks(html) {
  const out = [];
  const seen = new Set();
  const re = /<a[^>]+href=['"]([^'"]*\bin=([^&'"]+)[^'"]*)['"][^>]*>([\s\S]*?)<\/a>/gi;
  for (const m of html.matchAll(re)) {
    const slug = decodeURIComponent(m[2] ?? '');
    if (!slug || seen.has(slug)) continue;
    seen.add(slug);
    const name = (m[3] ?? '').replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim() || slug.split('-')[0] || slug;
    out.push({ name, slug });
  }
  return out;
}

function parseLdJson(html) {
  const out = [];
  for (const m of html.matchAll(/<script[^>]+type=['"]application\/ld\+json['"][^>]*>([\s\S]*?)<\/script>/gi)) {
    try { out.push(JSON.parse(m[1])); } catch {}
  }
  return out;
}

function parseSearch(html, sourceQuery) {
  const embedded = extractJsonAfterMarker(html, '"fleamarketArticles":', '[') ?? [];
  if (Array.isArray(embedded) && embedded.length) return embedded.map(x => normalizeArticle(x, sourceQuery));

  for (const block of parseLdJson(html)) {
    if (block?.['@type'] !== 'ItemList' || !Array.isArray(block.itemListElement)) continue;
    return block.itemListElement.map(x => x?.item).filter(Boolean).map(item => ({
      id: articleId(item.url) || 'unknown',
      title: item.name ?? '제목 없음',
      price: priceNum(item.offers?.price),
      url: item.url,
      imageUrl: Array.isArray(item.image) ? item.image[0] : item.image,
      status: 'Ongoing',
      sellerName: item.offers?.seller?.name,
      sourceQuery
    }));
  }
  return [];
}

async function getHtml(url) {
  const res = await fetch(url, {
    signal: AbortSignal.timeout(12000),
    headers: {
      'User-Agent': UA,
      'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
      'Accept-Language': 'ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7'
    }
  });
  if (!res.ok) throw new Error('HTTP ' + res.status + ' ' + res.statusText + ' for ' + url);
  return await res.text();
}

async function searchOne(query, regionSlug, limit) {
  const url = new URL(SEARCH);
  url.searchParams.set('search', query);
  if (regionSlug) url.searchParams.set('in', regionSlug);
  const html = await getHtml(url);
  return {
    items: parseSearch(html, query).slice(0, limit),
    regions: parseRegionLinks(html)
  };
}

async function detailOne(item) {
  try {
    const html = await getHtml(item.url);
    const product = extractJsonAfterMarker(html, '"product":', '{')
      ?? extractJsonAfterMarker(html, '\\"product\\":', '{');
    if (product) {
      return {
        ...item,
        description: product.content ?? '',
        categoryName: product.category?.name,
        viewCount: product.viewCount,
        sellerReviewCount: product.user?.reviewCount,
        sellerName: product.user?.nickname ?? item.sellerName,
        mannerTemperature: product.user?.score ?? item.mannerTemperature,
        location: product.locationName ?? product.region?.name ?? item.location,
        regionPath: [product.region?.name1, product.region?.name2, product.region?.name3].filter(Boolean).join(' ') || item.regionPath,
        regionSlug: product.region?.name && product.region?.dbId ? product.region.name + '-' + product.region.dbId : item.regionSlug,
        status: product.status ?? item.status,
        postedAt: product.createdAt ?? item.postedAt,
        boostedAt: product.boostedAt ?? item.boostedAt
      };
    }
    const ld = parseLdJson(html).find(x => x?.['@type'] === 'Product');
    if (ld) return { ...item, description: ld.description ?? '' };
    return item;
  } catch (e) {
    return { ...item, detailError: String(e) };
  }
}

function hasSpec(text, values, unit) {
  const s = text.toLowerCase().replace(/\s+/g, '');
  return values.some(v => s.includes(String(v) + unit.toLowerCase()));
}


function matchesTargetSpecs(item, request) {
  const text = ((item.title ?? '') + ' ' + (item.description ?? '')).replace(/\s+/g, ' ');
  const ramMatch =
    /(?:RAM|메모리|램)\s*[:\-]?\s*(16|32)\s*(?:GB|G)\b/i.test(text) ||
    /\b(16|32)\s*GB\s*(?:RAM|메모리|램)\b/i.test(text);
  const storageValue = '(?:512\\s*(?:GB|G)|1\\s*TB|1024\\s*(?:GB|G))';
  const ssdMatch =
    new RegExp('(?:SSD|NVMe|M\\.?2)[^\\n]{0,24}' + storageValue, 'i').test(text) ||
    new RegExp(storageValue + '[^\\n]{0,24}(?:SSD|NVMe|M\\.?2)', 'i').test(text);
  return ramMatch && ssdMatch;
}

function isTargetArea(item, request) {
  const place = ((item.regionPath ?? '') + ' ' + (item.location ?? '')).trim();
  const cities = request.targetCities ?? [];
  return cities.length === 0 || cities.some(city => place.includes(city));
}

function isRecentEnough(item, request) {
  if (!request.postedSince) return true;
  if (!item.postedAt) return false;
  return new Date(item.postedAt).getTime() >= new Date(request.postedSince).getTime();
}

function score(item, request) {
  const text = (item.title + ' ' + (item.description ?? '')).toLowerCase();
  let s = 0;
  const ramVals = request.ramGB ?? [16, 32];
  const ssdVals = request.ssdGB ?? [512, 1024];

  if (hasSpec(text, ramVals, 'gb')) s += 28;
  if (hasSpec(text, [512], 'gb') || /512\s*(gb|g)/i.test(text)) s += 18;
  if (hasSpec(text, [1], 'tb') || /1\s*tb/i.test(text) || /1024\s*(gb|g)/i.test(text)) s += 18;

  if (/i[357]-?1[2345]\d{2}|core\s*ultra|ryzen\s*[3579]\s*[56789]\d{3}/i.test(text)) s += 18;
  else if (/i[357]-?11\d{2}|i[357]-?10\d{2}|ryzen\s*[3579]\s*4\d{3}/i.test(text)) s += 8;

  if (/갤럭시북|galaxy\s*book|그램|gram|thinkpad|씽크패드|latitude|elitebook|probook|vivobook|zenbook|ideapad/i.test(text)) s += 8;
  if (/미개봉|새상품|거의\s*새|배터리\s*좋/i.test(text)) s += 3;
  if (/부품용|고장|화면.*불량|키보드.*불량|액정.*깨|배터리.*불량/i.test(text)) s -= 30;
  if (item.status === 'Closed') s -= 20;

  const p = item.price || 0;
  if (p > 0 && p <= 700000) s += 14;
  else if (p <= 900000) s += 8;
  else if (p <= 1200000) s += 2;
  else if (p > 1500000) s -= 5;

  return s;
}

const request = JSON.parse(await fs.readFile(REQUEST_PATH, 'utf8'));
const regionSlug = request.regionSlug || '';
const regionSlugs = request.regionSlugs?.length ? request.regionSlugs : [regionSlug];
const queries = request.queries?.length ? request.queries : ['노트북'];
const perQuery = Math.max(5, Math.min(300, request.perQuery ?? 30));
const detailLimit = Math.max(0, Math.min(80, request.detailLimit ?? 40));

const all = [];
const errors = [];
const discoveredRegions = [];
for (const rs of regionSlugs) {
  for (const q of queries) {
    try {
      const found = await searchOne(q, rs, perQuery);
      all.push(...found.items.map(item => ({ ...item, sourceRegionSlug: rs })));
      for (const r of found.regions) {
        if (!discoveredRegions.some(x => x.slug === r.slug)) discoveredRegions.push(r);
      }
    } catch (e) {
      errors.push({ query: q, regionSlug: rs, error: String(e) });
    }
    await sleep(350);
  }
}

const map = new Map();
for (const item of all) {
  const key = item.id !== 'unknown' ? item.id : item.url;
  if (!map.has(key)) map.set(key, item);
  else {
    const prev = map.get(key);
    prev.sourceQuery = Array.from(new Set([].concat(prev.sourceQuery ?? [], item.sourceQuery ?? [])));
  }
}

let items = [...map.values()]
  .filter(x => !request.onlyOngoing || x.status === 'Ongoing')
  .filter(x => !request.maxPrice || !x.price || x.price <= request.maxPrice)
  .filter(x => !request.minPrice || !x.price || x.price >= request.minPrice);

items.sort((a, b) => (b.favoriteCount ?? 0) - (a.favoriteCount ?? 0));
const detailed = [];
for (const item of items.slice(0, detailLimit)) {
  detailed.push(await detailOne(item));
  await sleep(700);
}
items = [...detailed, ...items.slice(detailLimit)];

const likelySpec = items.filter(item => {
  const text = (item.title ?? '').replace(/\s+/g, ' ');
  return /(?:16|32)\s*(?:GB|G)\b/i.test(text) || /(?:512\s*(?:GB|G)|1\s*TB|1024\s*(?:GB|G))/i.test(text);
});
const targetDetailed = [];
for (const item of likelySpec.slice(0, request.targetDetailLimit ?? 120)) {
  targetDetailed.push(item.description !== undefined ? item : await detailOne(item));
  await sleep(300);
}
const localRecent = targetDetailed.filter(item => isTargetArea(item, request) && isRecentEnough(item, request));
const exactTargetCandidates = localRecent
  .filter(item => matchesTargetSpecs(item, request))
  .map(item => ({ ...item, score: score(item, request) }))
  .sort((a, b) => b.score - a.score || (a.price || 1e15) - (b.price || 1e15));

console.log('TARGET_SCAN_STATS=' + JSON.stringify({
  likelySpec: likelySpec.length,
  localRecent: localRecent.length,
  detailed: targetDetailed.length,
  exact: exactTargetCandidates.length,
  postedSince: request.postedSince ?? null,
  targetCities: request.targetCities ?? []
}));
console.log('TARGET_CANDIDATES_JSON=' + JSON.stringify(exactTargetCandidates.slice(0, 80)));

for (const item of items) item.score = score(item, request);
items.sort((a, b) => b.score - a.score || (a.price || 1e15) - (b.price || 1e15));

const result = {
  ok: errors.length < queries.length,
  generatedAt: new Date().toISOString(),
  request,
  stats: {
    rawCount: all.length,
    uniqueCount: map.size,
    finalCount: items.length,
    detailedCount: detailed.length
  },
  errors,
  discoveredRegions,
  items: items.slice(0, request.outputLimit ?? 60)
};

await fs.writeFile(RESULT_PATH, JSON.stringify(result, null, 2) + '\n');
console.log(JSON.stringify(result.stats));
