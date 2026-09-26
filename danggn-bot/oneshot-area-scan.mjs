import fs from 'node:fs/promises';

const BASE = 'https://www.daangn.com';
const SEARCH = BASE + '/kr/buy-sell/';
const UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36';
const WINDOW_HOURS = 48;
const SEARCH_LIMIT = 80;
const DETAIL_LIMIT = 800;
const QUERIES = ['노트북', '그램', '갤럭시북', 'ThinkPad'];

const TARGETS = {
  seongnam: { name: '성남시', province: '경기도' },
  gwangjin: { name: '광진구', province: '서울특별시' },
  songpa: { name: '송파구', province: '서울특별시' },
  gangnam: { name: '강남구', province: '서울특별시' },
  seocho: { name: '서초구', province: '서울특별시' },
  gangdong: { name: '강동구', province: '서울특별시' }
};

const slug = process.env.TARGET_SLUG;
const target = TARGETS[slug];
if (!target) throw new Error('Unknown TARGET_SLUG: ' + slug);

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
    if (c === closeChar && --depth === 0) return input.slice(startIndex, i + 1);
  }
  return null;
}

function extractJsonAfterMarker(html, marker, openChar) {
  const markerIndex = html.indexOf(marker);
  if (markerIndex < 0) return null;
  const tail = html.slice(markerIndex + marker.length);
  const start = tail.indexOf(openChar);
  if (start < 0) return null;
  const raw = extractBalancedJson(tail, start, openChar);
  if (!raw) return null;
  try { return JSON.parse(raw); } catch { return null; }
}

function articleId(value) {
  const s = String(value || '').trim();
  if (/^[a-z0-9]+$/i.test(s)) return s;
  return s.match(/-([a-z0-9]+)\/?(?:[?#].*)?$/i)?.[1]
    ?? s.match(/buy-sell\/([a-z0-9]+)\/?(?:[?#].*)?$/i)?.[1]
    ?? null;
}

function priceNum(value) {
  if (typeof value === 'number') return Number.isFinite(value) ? value : 0;
  const n = Number.parseFloat(String(value ?? '').replace(/[^0-9.-]/g, ''));
  return Number.isFinite(n) ? n : 0;
}

function normalizeTimestamp(value) {
  if (value == null || value === '') return null;
  if (typeof value === 'number') {
    const ms = value < 1e12 ? value * 1000 : value;
    const d = new Date(ms);
    return Number.isFinite(d.getTime()) ? d.toISOString() : null;
  }
  const text = String(value).trim();
  if (!text) return null;
  if (/^\d{10,13}$/.test(text)) return normalizeTimestamp(Number(text));
  const d = new Date(text);
  return Number.isFinite(d.getTime()) ? d.toISOString() : null;
}

function firstTimestamp(raw, keys) {
  for (const key of keys) {
    const normalized = normalizeTimestamp(raw?.[key]);
    if (normalized) return normalized;
  }
  return null;
}

function normalizeArticle(raw, sourceQuery, sourceRegion) {
  const id = articleId(raw.href || raw.id) || 'unknown';
  const url = raw.href
    ? (String(raw.href).startsWith('http') ? raw.href : BASE + raw.href)
    : SEARCH + id + '/';
  return {
    id,
    title: raw.title ?? '제목 없음',
    price: priceNum(raw.price),
    url,
    location: raw.locationName ?? raw.region?.name,
    regionPath: [raw.region?.name1, raw.region?.name2, raw.region?.name3].filter(Boolean).join(' ') || undefined,
    postedAt: firstTimestamp(raw, ['createdAt','created_at','publishedAt','published_at','dateCreated','datePublished']),
    boostedAt: firstTimestamp(raw, ['boostedAt','boosted_at','bumpedAt','bumped_at']),
    status: raw.status ?? 'Ongoing',
    favoriteCount: raw.favoriteCount ?? 0,
    chatCount: raw.chatCount ?? 0,
    sourceQuery,
    sourceRegion
  };
}


function parseLdJson(html) {
  const out = [];
  for (const m of html.matchAll(/<script[^>]+type=['"]application\/ld\+json['"][^>]*>([\s\S]*?)<\/script>/gi)) {
    try { out.push(JSON.parse(m[1])); } catch {}
  }
  return out;
}

function parseSearch(html, sourceQuery, sourceRegion) {
  const embedded = extractJsonAfterMarker(html, '"fleamarketArticles":', '[') ?? [];
  if (Array.isArray(embedded) && embedded.length) {
    return embedded.map(item => normalizeArticle(item, sourceQuery, sourceRegion));
  }
  for (const block of parseLdJson(html)) {
    if (block?.['@type'] !== 'ItemList' || !Array.isArray(block.itemListElement)) continue;
    return block.itemListElement
      .map(x => x?.item)
      .filter(Boolean)
      .map(item => ({
        id: articleId(item.url) || 'unknown',
        title: item.name ?? '제목 없음',
        price: priceNum(item.offers?.price),
        url: item.url,
        location: item.address?.addressLocality,
        postedAt: normalizeTimestamp(item.datePosted ?? item.dateCreated ?? item.datePublished),
        status: 'Ongoing',
        favoriteCount: 0,
        chatCount: 0,
        sourceQuery,
        sourceRegion
      }));
  }
  return [];
}

async function getText(url, attempts = 2) {
  let lastError;
  for (let attempt = 1; attempt <= attempts; attempt++) {
    try {
      const response = await fetch(url, {
        signal: AbortSignal.timeout(10000),
        headers: {
          'User-Agent': UA,
          'Accept': 'text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8',
          'Accept-Language': 'ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7'
        }
      });
      if (!response.ok) throw new Error('HTTP ' + response.status + ' ' + response.statusText);
      return await response.text();
    } catch (e) {
      lastError = e;
      if (attempt < attempts) await new Promise(r => setTimeout(r, 250));
    }
  }
  throw lastError;
}

async function resolveRegions() {
  const url = new URL(BASE + '/kr/api/v1/regions/keyword');
  url.searchParams.set('keyword', target.province + ' ' + target.name);
  const data = JSON.parse(await getText(url));
  const rows = Array.isArray(data?.locations) ? data.locations : [];

  const districtLevel = rows
    .filter(r => r && Number(r.depth) === 2)
    .filter(r => r.name1 === target.province)
    .filter(r => typeof r.name2 === 'string' && (r.name2 === target.name || r.name2.startsWith(target.name + ' ')))
    .filter(r => /^\d{1,8}$/.test(String(r.id)))
    .map(r => ({
      id: String(r.id),
      name: String(r.name2 || r.name || '').trim(),
      slug: String(r.name2 || r.name || '').trim() + '-' + String(r.id)
    }))
    .filter(r => r.name);

  if (districtLevel.length) return districtLevel;

  return rows
    .filter(r => r && Number(r.depth) === 3)
    .filter(r => r.name1 === target.province)
    .filter(r => typeof r.name2 === 'string' && (r.name2 === target.name || r.name2.startsWith(target.name + ' ')))
    .filter(r => /^\d{1,8}$/.test(String(r.id)))
    .map(r => ({
      id: String(r.id),
      name: String(r.name3 || r.name || '').trim(),
      slug: String(r.name3 || r.name || '').trim() + '-' + String(r.id)
    }))
    .filter(r => r.name);
}

async function searchOne(query, region) {
  try {
    const routeUrl = new URL(BASE + '/kr/buy-sell/all/');
    routeUrl.searchParams.set('search', query);
    routeUrl.searchParams.set('in', region.slug);
    routeUrl.searchParams.set('_data', 'routes/kr.buy-sell._index');
    const text = await getText(routeUrl);
    if (text.trim()) {
      const data = JSON.parse(text);
      if (data?.region?.id != null && String(data.region.id) !== String(region.id)) return [];
      const rows = data?.allPage?.fleamarketArticles;
      if (Array.isArray(rows) && rows.length) {
        return rows.map(item => normalizeArticle(item, query, region.slug)).slice(0, SEARCH_LIMIT);
      }
    }
  } catch {}

  const url = new URL(SEARCH);
  url.searchParams.set('search', query);
  url.searchParams.set('in', region.slug);
  const html = await getText(url);
  return parseSearch(html, query, region.slug).slice(0, SEARCH_LIMIT);
}

async function detailOne(item) {
  try {
    const html = await getText(item.url);
    const product = extractJsonAfterMarker(html, '"product":', '{')
      ?? extractJsonAfterMarker(html, '\\"product\\":', '{');
    if (!product) return item;
    return {
      ...item,
      description: product.content ?? '',
      location: product.locationName ?? product.region?.name ?? item.location,
      regionPath: [product.region?.name1, product.region?.name2, product.region?.name3].filter(Boolean).join(' ') || item.regionPath,
      status: product.status ?? item.status,
      price: priceNum(product.price ?? item.price),
      postedAt: firstTimestamp(product, ['createdAt','created_at','publishedAt','published_at','dateCreated','datePublished']) ?? item.postedAt,
      boostedAt: firstTimestamp(product, ['boostedAt','boosted_at','bumpedAt','bumped_at']) ?? item.boostedAt
    };
  } catch (e) {
    return { ...item, detailError: String(e) };
  }
}

function isRecent(item, nowMs) {
  if (!item.postedAt) return false;
  const t = new Date(item.postedAt).getTime();
  return Number.isFinite(t) && t >= nowMs - WINDOW_HOURS * 3600000 && t <= nowMs + 300000;
}

function belongs(item) {
  const p = ((item.regionPath ?? '') + ' ' + (item.location ?? '')).trim();
  return p.includes(target.name);
}

function analyzeSpecs(item) {
  const text = ((item.title ?? '') + '\n' + (item.description ?? '')).replace(/\s+/g, ' ');
  const labeledRam =
    text.match(/(?:RAM|메모리|램)\s*[:\-]?\s*(16|32)\s*(?:GB|G)\b/i)?.[1]
    ?? text.match(/\b(16|32)\s*(?:GB|G)\s*(?:RAM|메모리|램)\b/i)?.[1]
    ?? null;
  const genericRam = text.match(/\b(16|32)\s*GB\b/i)?.[1] ?? null;
  const ram = labeledRam ?? genericRam;

  const storageForward = text.match(/(?:SSD|NVMe|M\.?2)[^.;,\n]{0,32}?(512\s*(?:GB|G)|1\s*TB|1024\s*(?:GB|G))/i);
  const storageReverse = text.match(/(512\s*(?:GB|G)|1\s*TB|1024\s*(?:GB|G))[^.;,\n]{0,32}?(?:SSD|NVMe|M\.?2)/i);
  const labeledStorage = storageForward?.[1] ?? storageReverse?.[1] ?? null;
  const genericStorage = text.match(/\b(512\s*GB|1\s*TB|1024\s*GB)\b/i)?.[1] ?? null;
  const storageRaw = labeledStorage ?? genericStorage;
  const mixedBad =
    /(?:SSD|NVMe|M\.?2)[^.;,\n]{0,24}?256\s*(?:GB|G)[^.;,\n]{0,48}?(?:HDD|하드)[^.;,\n]{0,24}?(?:1\s*TB|1024\s*(?:GB|G))/i.test(text)
    || /(?:HDD|하드)[^.;,\n]{0,24}?(?:1\s*TB|1024\s*(?:GB|G))[^.;,\n]{0,48}?(?:SSD|NVMe|M\.?2)[^.;,\n]{0,24}?256\s*(?:GB|G)/i.test(text);
  const storageGB = storageRaw ? (/1\s*TB|1024/i.test(storageRaw) ? 1024 : 512) : null;

  return {
    ok: Boolean(ram && storageGB && !mixedBad),
    ramGB: ram ? Number(ram) : null,
    storageGB,
    confidence: labeledRam && labeledStorage ? 'explicit' : 'needs_verification'
  };
}

function extractCpu(text) {
  const t = String(text ?? '').replace(/\s+/g, ' ');
  return t.match(/\b(?:Intel\s*)?Core\s*Ultra\s*[3579]\s*\d{3}[A-Z]*\b/i)?.[0]
    ?? t.match(/\bCore\s*[3579]\s*\d{3}[A-Z]*\b/i)?.[0]
    ?? t.match(/\bi[3579]-?\d{4,5}[A-Z]{0,2}\b/i)?.[0]
    ?? t.match(/\bRyzen\s*[3579]\s*\d{4}[A-Z]{0,3}\b/i)?.[0]
    ?? t.match(/\bApple\s*M[1-4](?:\s*(?:Pro|Max|Ultra))?\b/i)?.[0]
    ?? null;
}

async function mapLimit(items, limit, fn) {
  const out = new Array(items.length);
  let index = 0;
  async function worker() {
    while (true) {
      const i = index++;
      if (i >= items.length) return;
      out[i] = await fn(items[i], i);
    }
  }
  await Promise.all(Array.from({ length: Math.min(limit, items.length) }, worker));
  return out;
}

const startedAt = new Date();
const nowMs = startedAt.getTime();
const regions = await resolveRegions();
if (!regions.length) throw new Error('No regions resolved for ' + target.name);

const tasks = [];
for (const region of regions) for (const query of QUERIES) tasks.push({ region, query });

const searched = await mapLimit(tasks, 2, async ({region, query}) => {
  try { return await searchOne(query, region); }
  catch (e) { return [{ _searchError: String(e), sourceRegion: region.slug, sourceQuery: query }]; }
});

const errors = searched.flat().filter(x => x?._searchError);
const found = new Map();
for (const item of searched.flat().filter(x => !x?._searchError)) {
  const key = item.id !== 'unknown' ? item.id : item.url;
  const prev = found.get(key);
  if (!prev) found.set(key, item);
  else {
    if (!prev.postedAt && item.postedAt) prev.postedAt = item.postedAt;
    if (!prev.boostedAt && item.boostedAt) prev.boostedAt = item.boostedAt;
  }
}

const detailQueue = [...found.values()]
  .filter(item => item.status === 'Ongoing')
  .filter(item => !item.price || (item.price >= 100000 && item.price <= 1500000))
  .filter(item => !item.postedAt || isRecent(item, nowMs))
  .filter(item => {
    const p = ((item.regionPath ?? '') + ' ' + (item.location ?? '')).trim();
    return !p || p.includes(target.name);
  })
  .sort((a,b) => {
    const at = a.postedAt ? new Date(a.postedAt).getTime() : 0;
    const bt = b.postedAt ? new Date(b.postedAt).getTime() : 0;
    return bt - at;
  })
  .slice(0, DETAIL_LIMIT);

const detailed = await mapLimit(detailQueue, 4, detailOne);
const candidates = detailed
  .filter(item => !item.detailError)
  .filter(item => item.status === 'Ongoing')
  .filter(item => belongs(item))
  .filter(item => isRecent(item, nowMs))
  .map(item => ({ item, specs: analyzeSpecs(item) }))
  .filter(x => x.specs.ok)
  .map(({item, specs}) => ({
    id: item.id,
    area: target.name,
    location: item.location ?? null,
    regionPath: item.regionPath ?? null,
    title: item.title,
    price: Number(item.price) || 0,
    cpuHint: extractCpu((item.title ?? '') + '\n' + (item.description ?? '')),
    ramGB: specs.ramGB,
    storageGB: specs.storageGB,
    specConfidence: specs.confidence,
    postedAt: item.postedAt,
    boostedAt: item.boostedAt ?? null,
    description: String(item.description ?? '').slice(0, 2200),
    url: item.url,
    favoriteCount: item.favoriteCount ?? 0,
    chatCount: item.chatCount ?? 0
  }));

const result = {
  generatedAt: new Date().toISOString(),
  scanWindowHours: WINDOW_HOURS,
  target,
  regionCount: regions.length,
  regions: regions.map(r => r.slug),
  searchTaskCount: tasks.length,
  searchErrorCount: errors.length,
  uniqueListingCount: found.size,
  recentListingCount: detailed.filter(item => isRecent(item, nowMs)).length,
  candidateCount: candidates.length,
  errors: errors.slice(0,20),
  candidates
};

const outPath = 'oneshot-result-' + slug + '.json';
await fs.writeFile(outPath, JSON.stringify(result, null, 2) + '\n');
console.log('RESULT_SUMMARY=' + JSON.stringify({
  target: target.name,
  regionCount: result.regionCount,
  searchTaskCount: result.searchTaskCount,
  searchErrorCount: result.searchErrorCount,
  uniqueListingCount: result.uniqueListingCount,
  recentListingCount: result.recentListingCount,
  candidateCount: result.candidateCount
}));
console.log('RESULT_JSON=' + JSON.stringify(result));
