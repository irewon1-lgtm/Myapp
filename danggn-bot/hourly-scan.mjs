import fs from 'node:fs/promises';

const STATE_PATH = new URL('./hourly-state.json', import.meta.url);
const RESULT_PATH = new URL('./hourly-result.json', import.meta.url);

const BASE = 'https://www.daangn.com';
const SEARCH = BASE + '/kr/buy-sell/';
const UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36';

const WINDOW_HOURS = 6;
const STATE_RETENTION_DAYS = 30;
const MAX_STATE_ITEMS = 5000;
const SEARCH_LIMIT = 80;
const DETAIL_LIMIT_PER_CITY = 80;

const QUERIES = [
  '노트북',
  '그램',
  '갤럭시북',
  'ThinkPad'
];

const CITIES = [
  { name: '군포시', regionNames: ['당정동', '산본2동'] },
  { name: '의왕시', regionNames: ['내손동', '포일동', '고천동', '오전동', '청계동'] },
  { name: '안양시', regionNames: ['갈산동', '관양1동', '비산1동', '석수1동', '호계동', '평촌동', '안양동'] },
  { name: '과천시', regionNames: ['원문동'] }
];

const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

function extractBalancedJson(input, startIndex, openChar) {
  const closeChar = openChar === '{' ? '}' : ']';
  let depth = 0;
  let inString = false;
  let escaped = false;
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
    imageUrl: raw.thumbnail ?? undefined,
    location: raw.locationName ?? raw.region?.name,
    regionPath: [raw.region?.name1, raw.region?.name2, raw.region?.name3].filter(Boolean).join(' ') || undefined,
    postedAt: firstTimestamp(raw, ['createdAt', 'created_at', 'publishedAt', 'published_at', 'dateCreated', 'datePublished']),
    boostedAt: firstTimestamp(raw, ['boostedAt', 'boosted_at', 'bumpedAt', 'bumped_at']),
    chatCount: raw.chatCount ?? 0,
    favoriteCount: raw.favoriteCount ?? 0,
    status: raw.status ?? 'Ongoing',
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
        imageUrl: Array.isArray(item.image) ? item.image[0] : item.image,
        status: 'Ongoing',
        sourceQuery,
        sourceRegion
      }));
  }
  return [];
}

async function getHtml(url, attempts = 2) {
  let lastError;
  for (let attempt = 1; attempt <= attempts; attempt++) {
    try {
      const response = await fetch(url, {
        signal: AbortSignal.timeout(10000),
        headers: {
          'User-Agent': UA,
          'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
          'Accept-Language': 'ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7'
        }
      });
      if (!response.ok) throw new Error('HTTP ' + response.status + ' ' + response.statusText);
      return await response.text();
    } catch (error) {
      lastError = error;
      if (attempt < attempts) await sleep(700);
    }
  }
  throw lastError;
}

function normalizeRegionResponse(data, city) {
  const rows = Array.isArray(data?.locations) ? data.locations : [];
  const all = rows
    .filter(r => r && Number(r.depth) === 3)
    .filter(r => r.name1 === '경기도')
    .filter(r => typeof r.name2 === 'string' && (r.name2 === city.name || r.name2.startsWith(city.name + ' ')))
    .filter(r => /^\d{1,8}$/.test(String(r.id)))
    .map(r => ({
      id: String(r.id),
      name: String(r.name3 || r.name || '').trim(),
      slug: String(r.name3 || r.name || '').trim() + '-' + String(r.id)
    }))
    .filter(r => r.name);

  const byName = new Map(all.map(r => [r.name, r]));
  const selected = city.regionNames.map(name => byName.get(name)).filter(Boolean);
  if (selected.length) return selected;

  // If a neighborhood was renamed, fall back to a small deterministic subset
  // instead of silently returning zero listings for the entire city.
  return all.slice(0, Math.min(3, all.length));
}

async function resolveCityRegions(city) {
  const url = new URL(BASE + '/kr/api/v1/regions/keyword');
  url.searchParams.set('keyword', '경기도 ' + city.name);
  const text = await getHtml(url);
  const data = JSON.parse(text);
  const regions = normalizeRegionResponse(data, city);
  if (!regions.length) throw new Error('No Daangn regions resolved for ' + city.name);
  return regions;
}

function parseRouteSearch(data, query, region) {
  if (data?.region?.id != null && String(data.region.id) !== String(region.id)) {
    throw new Error('Region mismatch: requested ' + region.id + ', got ' + data.region.id);
  }
  const rows = data?.allPage?.fleamarketArticles;
  if (!Array.isArray(rows)) return [];
  return rows.map(item => normalizeArticle(item, query, region.slug)).slice(0, SEARCH_LIMIT);
}

async function searchOne(query, region) {
  // Prefer Daangn's route-data response. It currently contains createdAt/boostedAt,
  // while the public HTML may fall back to JSON-LD that omits postedAt.
  try {
    const routeUrl = new URL(BASE + '/kr/buy-sell/all/');
    routeUrl.searchParams.set('search', query);
    routeUrl.searchParams.set('in', region.slug);
    routeUrl.searchParams.set('_data', 'routes/kr.buy-sell._index');
    const text = await getHtml(routeUrl);
    const data = JSON.parse(text);
    const rows = parseRouteSearch(data, query, region);
    if (rows.length) return rows;
  } catch {}

  const url = new URL(SEARCH);
  url.searchParams.set('search', query);
  url.searchParams.set('in', region.slug);
  const html = await getHtml(url);
  return parseSearch(html, query, region.slug).slice(0, SEARCH_LIMIT);
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
        sellerName: product.user?.nickname,
        mannerTemperature: product.user?.score,
        location: product.locationName ?? product.region?.name ?? item.location,
        regionPath: [product.region?.name1, product.region?.name2, product.region?.name3].filter(Boolean).join(' ') || item.regionPath,
        status: product.status ?? item.status,
        price: priceNum(product.price ?? item.price),
        postedAt: firstTimestamp(product, ['createdAt', 'created_at', 'publishedAt', 'published_at', 'dateCreated', 'datePublished']) ?? item.postedAt,
        boostedAt: firstTimestamp(product, ['boostedAt', 'boosted_at', 'bumpedAt', 'bumped_at']) ?? item.boostedAt
      };
    }

    const ld = parseLdJson(html).find(x => x?.['@type'] === 'Product');
    if (ld) {
      return {
        ...item,
        description: ld.description ?? '',
        price: priceNum(ld.offers?.price ?? item.price)
      };
    }
    return item;
  } catch (error) {
    return { ...item, detailError: String(error) };
  }
}

function itemKey(cityName, item) {
  return cityName + '|' + (item.id !== 'unknown' ? item.id : item.url);
}

function placeText(item) {
  return ((item.regionPath ?? '') + ' ' + (item.location ?? '')).trim();
}

function definitelyOtherTargetCity(item, currentCity) {
  const place = placeText(item);
  if (!place) return false;
  return CITIES.some(city => city.name !== currentCity && place.includes(city.name));
}

function isRecentByPostedAt(item, nowMs) {
  if (!item.postedAt) return false;
  const postedMs = new Date(item.postedAt).getTime();
  if (!Number.isFinite(postedMs)) return false;
  return postedMs >= nowMs - WINDOW_HOURS * 60 * 60 * 1000 && postedMs <= nowMs + 5 * 60 * 1000;
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

  const storageGB = storageRaw
    ? (/1\s*TB|1024/i.test(storageRaw) ? 1024 : 512)
    : null;

  return {
    ok: Boolean(ram && storageGB && !mixedBad),
    ramGB: ram ? Number(ram) : null,
    storageGB,
    mixedBad,
    ramTypeConfirmed: Boolean(labeledRam),
    storageTypeConfirmed: Boolean(labeledStorage),
    confidence: labeledRam && labeledStorage ? 'explicit' : 'needs_verification'
  };
}

function extractCpu(text) {
  const compact = String(text ?? '').replace(/\s+/g, ' ');
  return compact.match(/\b(?:Intel\s*)?Core\s*Ultra\s*[3579]\s*\d{3}[A-Z]*\b/i)?.[0]
    ?? compact.match(/\bi[3579]-?\d{4,5}[A-Z]{0,2}\b/i)?.[0]
    ?? compact.match(/\bRyzen\s*[3579]\s*\d{4}[A-Z]{0,3}\b/i)?.[0]
    ?? compact.match(/\bApple\s*M[1-4](?:\s*(?:Pro|Max|Ultra))?\b/i)?.[0]
    ?? null;
}

function trimText(value, max = 1800) {
  const text = String(value ?? '').trim();
  return text.length <= max ? text : text.slice(0, max) + '…';
}

async function readState() {
  try {
    const parsed = JSON.parse(await fs.readFile(STATE_PATH, 'utf8'));
    if (!parsed || typeof parsed !== 'object') throw new Error('invalid state');
    if (!parsed.items || typeof parsed.items !== 'object') parsed.items = {};
    return parsed;
  } catch {
    return { version: 1, updatedAt: null, items: {} };
  }
}

function clone(value) {
  return JSON.parse(JSON.stringify(value));
}

function pruneState(state, nowMs) {
  const cutoff = nowMs - STATE_RETENTION_DAYS * 24 * 60 * 60 * 1000;
  const entries = Object.entries(state.items ?? {}).filter(([, value]) => {
    const seen = new Date(value.lastSeenAt ?? 0).getTime();
    return Number.isFinite(seen) && seen >= cutoff;
  });

  entries.sort((a, b) => new Date(b[1].lastSeenAt).getTime() - new Date(a[1].lastSeenAt).getTime());
  state.items = Object.fromEntries(entries.slice(0, MAX_STATE_ITEMS));
}

async function main() {
  const startedAt = new Date();
  const nowMs = startedAt.getTime();
  const originalState = await readState();
  const nextState = clone(originalState);

  const candidates = [];
  const cityRuns = [];
  const warnings = [];
  let hardFailure = false;

  for (const city of CITIES) {
    const cityStarted = Date.now();
    const found = new Map();
    let attempted = 0;
    let succeeded = 0;
    const errors = [];

    let resolvedRegions = [];
    try {
      resolvedRegions = await resolveCityRegions(city);
    } catch (error) {
      errors.push({ regionSlug: 'region-resolution', query: '-', error: String(error) });
    }

    for (const region of resolvedRegions) {
      for (const query of QUERIES) {
        attempted++;
        try {
          const items = await searchOne(query, region);
          succeeded++;
          for (const item of items) {
            const key = item.id !== 'unknown' ? item.id : item.url;
            if (!found.has(key)) {
              found.set(key, item);
            } else {
              const prev = found.get(key);
              prev.sourceQuery = Array.from(new Set([].concat(prev.sourceQuery ?? [], item.sourceQuery ?? [])));
              prev.sourceRegion = Array.from(new Set([].concat(prev.sourceRegion ?? [], item.sourceRegion ?? [])));
              if (!prev.postedAt && item.postedAt) prev.postedAt = item.postedAt;
              if (!prev.boostedAt && item.boostedAt) prev.boostedAt = item.boostedAt;
            }
          }
        } catch (error) {
          errors.push({ regionSlug: region.slug, query, error: String(error) });
        }
        await sleep(140);
      }
    }

    const successRatio = attempted ? succeeded / attempted : 0;
    if (successRatio < 0.75) hardFailure = true;
    if (errors.length) warnings.push(city.name + ': search errors ' + errors.length + '/' + attempted);

    const stage1 = [];
    for (const item of found.values()) {
      if (item.status !== 'Ongoing') continue;
      if (definitelyOtherTargetCity(item, city.name)) continue;
      if (item.price && (item.price < 100000 || item.price > 1500000)) continue;

      const key = itemKey(city.name, item);
      const previous = originalState.items?.[key];
      const unseen = !previous;
      const isNew = unseen && isRecentByPostedAt(item, nowMs);
      const needsPostedAtCheck = unseen && !item.postedAt;
      const isPriceDrop = Boolean(
        previous
        && item.price > 0
        && Number(previous.lastPrice) > 0
        && item.price < Number(previous.lastPrice)
      );

      if (isNew || isPriceDrop || needsPostedAtCheck) {
        stage1.push({
          ...item,
          changeType: isPriceDrop ? 'price_drop' : (isNew ? 'new' : 'newness_check'),
          previousPrice: isPriceDrop ? Number(previous.lastPrice) : null
        });
      }

      const deferState = isNew || isPriceDrop || needsPostedAtCheck;
      if (!deferState) {
        nextState.items[key] = {
          id: item.id,
          title: item.title,
          url: item.url,
          lastPrice: item.price || previous?.lastPrice || null,
          postedAt: item.postedAt ?? previous?.postedAt ?? null,
          lastSeenAt: startedAt.toISOString(),
          city: city.name
        };
      }
    }

    let detailedCount = 0;
    for (const baseItem of stage1.slice(0, DETAIL_LIMIT_PER_CITY)) {
      const item = await detailOne(baseItem);
      detailedCount++;
      await sleep(220);

      if (item.detailError) {
        warnings.push(city.name + ': detail failed ' + item.id);
        continue;
      }
      if (item.status !== 'Ongoing') continue;
      if (definitelyOtherTargetCity(item, city.name)) continue;

      let changeType = item.changeType;
      if (changeType === 'newness_check') {
        if (!item.postedAt) {
          warnings.push(city.name + ': postedAt unavailable after detail ' + item.id);
          continue;
        }
        if (!isRecentByPostedAt(item, nowMs)) {
          const baselineKey = itemKey(city.name, item);
          nextState.items[baselineKey] = {
            id: item.id,
            title: item.title,
            url: item.url,
            lastPrice: Number(item.price) || null,
            postedAt: item.postedAt,
            lastSeenAt: startedAt.toISOString(),
            city: city.name
          };
          continue;
        }
        changeType = 'new';
      } else if (changeType === 'new' && !isRecentByPostedAt(item, nowMs)) {
        changeType = 'newness_check';
        if (!item.postedAt) continue;
        const baselineKey = itemKey(city.name, item);
        nextState.items[baselineKey] = {
          id: item.id,
          title: item.title,
          url: item.url,
          lastPrice: Number(item.price) || null,
          postedAt: item.postedAt,
          lastSeenAt: startedAt.toISOString(),
          city: city.name
        };
        continue;
      }

      const currentPrice = Number(item.price) || 0;
      const stateKey = itemKey(city.name, item);
      const previousState = nextState.items[stateKey] ?? originalState.items?.[stateKey] ?? {};
      nextState.items[stateKey] = {
        ...previousState,
        id: item.id,
        title: item.title,
        url: item.url,
        lastPrice: currentPrice || previousState.lastPrice || null,
        postedAt: item.postedAt ?? previousState.postedAt ?? null,
        lastSeenAt: startedAt.toISOString(),
        city: city.name
      };

      const specs = analyzeSpecs(item);
      if (!specs.ok) continue;

      const text = (item.title ?? '') + '\n' + (item.description ?? '');
      const previousPrice = changeType === 'price_drop' ? Number(item.previousPrice) || null : null;

      candidates.push({
        id: item.id,
        changeType,
        previousPrice,
        price: currentPrice,
        priceDropPercent: previousPrice && currentPrice
          ? Number((((previousPrice - currentPrice) / previousPrice) * 100).toFixed(1))
          : null,
        city: city.name,
        location: item.location ?? null,
        regionPath: item.regionPath ?? null,
        title: item.title,
        modelHint: item.title,
        cpuHint: extractCpu(text),
        ramGB: specs.ramGB,
        storageGB: specs.storageGB,
        specConfidence: specs.confidence,
        ramTypeConfirmed: specs.ramTypeConfirmed,
        storageTypeConfirmed: specs.storageTypeConfirmed,
        postedAt: item.postedAt ?? null,
        boostedAt: item.boostedAt ?? null,
        status: item.status,
        description: trimText(item.description),
        url: item.url,
        favoriteCount: item.favoriteCount ?? 0,
        chatCount: item.chatCount ?? 0,
        sellerName: item.sellerName ?? null,
        mannerTemperature: item.mannerTemperature ?? null
      });

    }

    cityRuns.push({
      city: city.name,
      regions: resolvedRegions.map(region => region.slug),
      attempted,
      succeeded,
      errors: errors.length,
      uniqueListings: found.size,
      changedCandidates: stage1.length,
      detailedCandidates: detailedCount,
      durationMs: Date.now() - cityStarted
    });
  }

  const uniqueCandidates = [];
  const seenCandidateIds = new Set();
  for (const candidate of candidates) {
    const key = candidate.id !== 'unknown' ? candidate.id : candidate.url;
    if (seenCandidateIds.has(key)) continue;
    seenCandidateIds.add(key);
    uniqueCandidates.push(candidate);
  }

  if (!hardFailure) {
    nextState.updatedAt = startedAt.toISOString();
    pruneState(nextState, nowMs);
    await fs.writeFile(STATE_PATH, JSON.stringify(nextState, null, 2) + '\n');
  }

  const result = {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    expectedScheduleMinute: 16,
    scanWindowHours: WINDOW_HOURS,
    searchOrder: CITIES.map(city => city.name),
    health: {
      status: hardFailure ? 'failed' : 'ok',
      stateCommitted: !hardFailure,
      warnings
    },
    rules: {
      newnessField: 'postedAt',
      boostedAtCreatesNewCandidate: false,
      onlyStatus: 'Ongoing',
      ramGB: [16, 32],
      storageGB: [512, 1024],
      mixed256SsdPlusHdd1TbAllowed: false
    },
    stats: {
      cities: cityRuns,
      finalCandidateCount: uniqueCandidates.length,
      stateItemCount: hardFailure
        ? Object.keys(originalState.items ?? {}).length
        : Object.keys(nextState.items ?? {}).length,
      durationMs: Date.now() - nowMs
    },
    candidates: uniqueCandidates
  };

  await fs.writeFile(RESULT_PATH, JSON.stringify(result, null, 2) + '\n');
  console.log(JSON.stringify({ health: result.health.status, candidates: uniqueCandidates.length, durationMs: result.stats.durationMs }));

  if (hardFailure) process.exitCode = 2;
}

main().catch(async error => {
  const result = {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    expectedScheduleMinute: 16,
    health: {
      status: 'failed',
      stateCommitted: false,
      warnings: [String(error)]
    },
    candidates: []
  };
  try {
    await fs.writeFile(RESULT_PATH, JSON.stringify(result, null, 2) + '\n');
  } catch {}
  console.error(error);
  process.exitCode = 1;
});
