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
const DETAIL_WORKERS = 2;
const DETAIL_BUDGET_PER_CITY_MS = 55000;
const SCAN_BUDGET_MS = 330000;
const NEWNESS_RETRY_DELAY_MS = 60 * 60 * 1000;
const REQUEST_GAP_MS = 1200;
let nextRequestAt = 0;
let requestCooldownUntil = 0;

const QUERIES = [
  '노트북',
  'RTX 노트북',
  'Ryzen AI Max',
  '맥북 Max'
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

async function getHtml(url, attempts = 2, pace = false) {
  let lastError;
  for (let attempt = 1; attempt <= attempts; attempt++) {
    try {
      if (Date.now() < requestCooldownUntil) throw new Error('HTTP 429 cooldown; retained for next scan');
      if (pace) {
        const requestAt = Math.max(Date.now(), nextRequestAt);
        nextRequestAt = requestAt + REQUEST_GAP_MS;
        await sleep(Math.max(0, requestAt - Date.now()));
      }
      if (Date.now() < requestCooldownUntil) throw new Error('HTTP 429 cooldown; retained for next scan');
      const response = await fetch(url, {
        signal: AbortSignal.timeout(10000),
        headers: {
          'User-Agent': UA,
          'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
          'Accept-Language': 'ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7'
        }
      });
      if (response.status === 429) requestCooldownUntil = Date.now() + 60 * 60 * 1000;
      if (!response.ok) throw new Error('HTTP ' + response.status + ' ' + response.statusText);
      return await response.text();
    } catch (error) {
      lastError = error;
      if (Date.now() < requestCooldownUntil) break;
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
    const html = await getHtml(item.url, 2, true);
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

function belongsToCityOrUnknown(item, currentCity) {
  const place = placeText(item);
  if (!place) return true;
  return place.includes(currentCity);
}

function belongsToCity(item, currentCity) {
  const place = placeText(item);
  return Boolean(place && place.includes(currentCity));
}

function isRecentByPostedAt(item, nowMs) {
  if (!item.postedAt) return false;
  const postedMs = new Date(item.postedAt).getTime();
  if (!Number.isFinite(postedMs)) return false;
  return postedMs >= nowMs - WINDOW_HOURS * 60 * 60 * 1000 && postedMs <= nowMs + 5 * 60 * 1000;
}

function capacityToGB(raw) {
  if (!raw) return null;
  const m = String(raw).match(/(\d+(?:\.\d+)?)\s*(TB|GB|G)\b/i);
  if (!m) return null;
  const value = Number(m[1]);
  if (!Number.isFinite(value)) return null;
  return /TB/i.test(m[2]) ? Math.round(value * 1024) : Math.round(value);
}

function analyzeHardware(item) {
  const text = ((item.title ?? '') + '\n' + (item.description ?? '')).replace(/\s+/g, ' ');
  const memoryUnit = '(?:GB|G|기가(?:바이트)?)';
  const memoryValues = '(16|24|32|36|48|64|96|128|192)';
  const ramLabel = '(?:\\bRAM\\b|램|렘|메모리|memory|system\\s*memory|통합\\s*메모리|unified\\s*memory)';
  const vramLabel = '(?:\\bVRAM\\b|브이램|비램|GPU\\s*메모리|그래픽\\s*(?:전용\\s*)?메모리|video\\s*memory|GDDR[67X]*)';

  // Parse VRAM first so strings such as "VRAM16GB" or "그래픽 메모리 16G"
  // can never be reinterpreted as ordinary system RAM.
  const vramForward = new RegExp(vramLabel + '\\s*(?:용량)?\\s*[:=\\-]?\\s*(8|10|12|16|20|24|32|48)\\s*' + memoryUnit + '?', 'i');
  const vramReverse = new RegExp('(8|10|12|16|20|24|32|48)\\s*' + memoryUnit + '?\\s*' + vramLabel, 'i');
  const vramMatch = text.match(vramForward) ?? text.match(vramReverse);
  const textWithoutVram = vramMatch ? text.replace(vramMatch[0], ' ') : text;

  const ramForward = new RegExp(ramLabel + '\\s*(?:용량)?\\s*[:=\\-]?\\s*' + memoryValues + '\\s*' + memoryUnit + '?', 'i');
  const ramReverse = new RegExp(memoryValues + '\\s*' + memoryUnit + '?\\s*' + ramLabel, 'i');
  const ramMatch = textWithoutVram.match(ramForward) ?? textWithoutVram.match(ramReverse);
  const ramGB = ramMatch ? Number(ramMatch[1]) || null : null;

  const storageRaw =
    text.match(/(?:SSD|NVMe|M\\.?2)[^.;,\\n]{0,32}?(256\\s*(?:GB|G)|512\\s*(?:GB|G)|\\d+(?:\\.\\d+)?\\s*TB|1024\\s*(?:GB|G)|2048\\s*(?:GB|G)|128\\s*(?:GB|G))/i)?.[1]
    ?? text.match(/(128\\s*(?:GB|G)|256\\s*(?:GB|G)|512\\s*(?:GB|G)|\\d+(?:\\.\\d+)?\\s*TB|1024\\s*(?:GB|G)|2048\\s*(?:GB|G))[^.;,\\n]{0,32}?(?:SSD|NVMe|M\\.?2)/i)?.[1]
    ?? null;
  const storageGB = capacityToGB(storageRaw);

  // Bare expressions such as "16기가", "32G", "64 GB" are preserved only
  // when they are not already explained by RAM/VRAM/storage labels.
  let ambiguousText = textWithoutVram;
  if (ramMatch) ambiguousText = ambiguousText.replace(ramMatch[0], ' ');
  ambiguousText = ambiguousText
    .replace(/(?:SSD|NVMe|M\\.?2)\\s*[:=\\-]?\\s*\\d+(?:\\.\\d+)?\\s*(?:TB|GB|G)/ig, ' ')
    .replace(/\\d+(?:\\.\\d+)?\\s*(?:TB|GB|G)\\s*(?:SSD|NVMe|M\\.?2)/ig, ' ');
  const ambiguousMemoryMatch = ambiguousText.match(/(?:^|[\\s/|,;:()])(?:용량\\s*)?(16|24|32|36|48|64|96|128|192)\\s*(GB|G|기가(?:바이트)?)(?=$|[\\s/|,;:()])/i);
  const ambiguousMemoryGB = ambiguousMemoryMatch ? Number(ambiguousMemoryMatch[1]) || null : null;

  const gpuHint =
    text.match(/\\bRTX\\s*(?:PRO\\s*)?\\d{4}(?:\\s*Ti)?(?:\\s*(?:Laptop|Mobile|Ada|Blackwell))?\\b/i)?.[0]
    ?? text.match(/\\bRTX\\s*A\\d{4}\\b/i)?.[0]
    ?? text.match(/\\bQuadro\\s*RTX\\s*\\d{4}\\b/i)?.[0]
    ?? text.match(/\\bRadeon\\s*[A-Z0-9 ]{2,18}\\b/i)?.[0]
    ?? text.match(/\\bApple\\s*M[1-5]\\s*(?:Pro|Max|Ultra)?\\b/i)?.[0]
    ?? text.match(/\\bM[1-5]\\s*(?:Pro|Max|Ultra)\\b/i)?.[0]
    ?? null;

  let vramGB = vramMatch ? Number(vramMatch[1]) || null : null;

  // Only map GPU models whose laptop/workstation VRAM is unambiguous enough
  // for this filter. Unknown or ambiguous models are left for ChatGPT review.
  if (/\\bRTX\\s*5090(?:\\s*Laptop)?\\b/i.test(text)) vramGB = Math.max(vramGB ?? 0, 24);
  if (/\\bRTX\\s*5080(?:\\s*Laptop)?\\b/i.test(text)) vramGB = Math.max(vramGB ?? 0, 16);
  if (/\\bRTX\\s*4090(?:\\s*Laptop)?\\b/i.test(text)) vramGB = Math.max(vramGB ?? 0, 16);
  if (/\\bRTX\\s*3080\\s*Ti(?:\\s*Laptop)?\\b/i.test(text)) vramGB = Math.max(vramGB ?? 0, 16);
  if (/\\bRTX\\s*A5000\\b/i.test(text)) vramGB = Math.max(vramGB ?? 0, 16);
  if (/\\bQuadro\\s*RTX\\s*5000\\b/i.test(text)) vramGB = Math.max(vramGB ?? 0, 16);
  if (/\\bRTX\\s*5000\\s*Ada\\b/i.test(text)) vramGB = Math.max(vramGB ?? 0, 16);
  if (/\\bRTX\\s*PRO\\s*5000\\s*Blackwell\\b/i.test(text)) vramGB = Math.max(vramGB ?? 0, 24);
  if (/\\bRTX\\s*PRO\\s*4000\\s*Blackwell\\b/i.test(text)) vramGB = Math.max(vramGB ?? 0, 16);

  const ramPass = (ramGB ?? 0) >= 32;
  const vramPass = (vramGB ?? 0) >= 16;
  const ryzenAiMaxShared64 = /\\bRyzen\\s*AI\\s*Max(?:\\+|\\s*Plus|\\s*Pro)?\\s*\\d{3}\\b/i.test(text)
    && Math.max(ramGB ?? 0, ambiguousMemoryGB ?? 0) >= 64;

  const confirmedPass = ramPass || vramPass || ryzenAiMaxShared64;
  const memoryAtLeast16 = Math.max(ramGB ?? 0, ambiguousMemoryGB ?? 0) >= 16;
  const needsReview = !confirmedPass && memoryAtLeast16;
  const ok = confirmedPass || needsReview;

  let hardwareReviewStatus = 'FAIL';
  let llmCapability = null;
  let llmReason = null;

  if (ramPass && vramPass) {
    hardwareReviewStatus = 'PASS_RAM_VRAM';
    llmCapability = 'HYBRID_WORK_LOCAL_LLM';
    llmReason = 'RAM 32GB 이상 + VRAM 16GB 이상: Codex/ChatGPT Work/개발 멀티태스킹과 로컬 LLM GPU 가속 모두 강점';
  } else if (vramPass) {
    hardwareReviewStatus = 'PASS_VRAM';
    llmCapability = 'LOCAL_LLM_GPU';
    llmReason = 'VRAM 16GB 이상이 직접 표기되었거나 확정 가능한 GPU 모델로 확인됨';
  } else if (ryzenAiMaxShared64) {
    hardwareReviewStatus = 'PASS_SHARED64';
    llmCapability = 'RYZEN_AI_MAX_SHARED';
    llmReason = 'Ryzen AI Max + 공유/통합메모리 64GB 이상 후보';
  } else if (ramPass) {
    hardwareReviewStatus = 'PASS_RAM';
    llmCapability = 'WORK_DEV_RAM32';
    llmReason = 'RAM 32GB 이상: Codex/ChatGPT Work/브라우저/개발 멀티태스킹 후보';
  } else if (needsReview) {
    hardwareReviewStatus = 'CHECK_MEMORY_GPU';
    llmCapability = 'SPEC_CHECK_REQUIRED';
    llmReason = '16GB 이상 메모리 표현은 확인됐지만 RAM/VRAM 또는 GPU 세부사양만으로 최종 성능 조건을 확정할 수 없어 2차 확인 필요';
  }

  return {
    ok,
    ramGB,
    ambiguousMemoryGB,
    storageGB,
    gpuHint,
    vramGB,
    hardwareReviewStatus,
    llmCapability,
    llmReason,
    ramEvidence: ramMatch?.[0] ?? null,
    vramEvidence: vramMatch?.[0] ?? null,
    ambiguousMemoryEvidence: ambiguousMemoryMatch?.[0]?.trim() ?? null,
    confidence: confirmedPass ? 'confirmed' : (needsReview ? 'needs_review' : 'insufficient')
  };
}

function extractCpu(text) {
  const compact = String(text ?? '').replace(/\s+/g, ' ');
  return compact.match(/\bRyzen\s*AI\s*Max(?:\+|\s*Plus|\s*Pro)?\s*\d{3}\b/i)?.[0]
    ?? compact.match(/\bRyzen\s*AI\s*[79]\s*(?:HX\s*)?\d{3}\b/i)?.[0]
    ?? compact.match(/\bRyzen\s*[79](?:\s*PRO)?\s*\d{4}[A-Z]{0,3}\b/i)?.[0]
    ?? compact.match(/\b(?:Intel\s*)?Core\s*Ultra\s*[79]\s*\d{3}[A-Z]*\b/i)?.[0]
    ?? compact.match(/\bi[79]-?\d{4,5}[A-Z]{0,2}\b/i)?.[0]
    ?? compact.match(/\bApple\s*M[1-5](?:\s*(?:Pro|Max|Ultra))?\b/i)?.[0]
    ?? compact.match(/\bM[1-5]\s*(?:Pro|Max|Ultra)\b/i)?.[0]
    ?? null;
}

function cpuMeetsBaseline(cpuHint) {
  if (!cpuHint) return false;
  const cpu = String(cpuHint).replace(/\s+/g, ' ').trim();

  if (/\bRyzen\s*AI\s*Max/i.test(cpu)) return true;
  if (/\bRyzen\s*AI\s*[79]\b/i.test(cpu)) return true;

  const amd = cpu.match(/\bRyzen\s*([79])(?:\s*PRO)?\s*(\d{4})([A-Z]{0,3})\b/i);
  if (amd) {
    const model = Number(amd[2]);
    if (model >= 8000) return true;
    if (model >= 7840 && model < 8000) return true;
    return false;
  }

  const ultra = cpu.match(/\bCore\s*Ultra\s*([79])\s*(\d{3})([A-Z]*)\b/i);
  if (ultra) {
    const model = Number(ultra[2]);
    const suffix = String(ultra[3] || '').toUpperCase();
    if (model >= 200) return true;
    return model >= 155 && /H|HX/.test(suffix);
  }

  const intel = cpu.match(/\bi([79])-?(\d{4,5})([A-Z]{0,2})\b/i);
  if (intel) {
    const model = Number(intel[2]);
    const suffix = String(intel[3] || '').toUpperCase();
    const generation = model >= 10000 ? Math.floor(model / 1000) : Math.floor(model / 1000);
    if (!/H|HX/.test(suffix)) return false;
    if (generation >= 13) return true;
    if (generation === 12 && model >= 12700) return true;
    return false;
  }

  const apple = cpu.match(/\b(?:Apple\s*)?M([1-5])\s*(Pro|Max|Ultra)?\b/i);
  if (apple) {
    const generation = Number(apple[1]);
    const tier = String(apple[2] || '').toLowerCase();
    if (generation >= 3) return true;
    if (generation >= 2 && /pro|max|ultra/.test(tier)) return true;
    if (generation === 1 && /max|ultra/.test(tier)) return true;
  }

  return false;
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

function resetAndExpireNewness(state, nowMs) {
  const reset = !state.newnessBacklogResetAt;
  for (const item of Object.values(state.items ?? {})) {
    if (!item?.pendingNewnessCheck && !item?.newnessProbeSkipped) continue;
    const firstSeenMs = new Date(item.firstSeenAt ?? 0).getTime();
    if (reset || !Number.isFinite(firstSeenMs)
      || nowMs - firstSeenMs > WINDOW_HOURS * 60 * 60 * 1000) {
      item.pendingNewnessCheck = false;
      item.newnessProbeSkipped = false;
      item.newnessDetailAttempts = 0;
      item.newnessUnknownExpired = true;
    }
  }
  if (reset) state.newnessBacklogResetAt = new Date(nowMs).toISOString();
}

async function detailBatch(queue, deadlineMs) {
  let cursor = 0;
  const results = [];
  await Promise.all(Array.from({ length: DETAIL_WORKERS }, async () => {
    while (cursor < queue.length && Date.now() < deadlineMs) {
      const baseItem = queue[cursor++];
      results.push(await detailOne(baseItem));
      await sleep(220);
    }
  }));
  return results;
}

function pruneState(state, nowMs) {
  const cutoff = nowMs - STATE_RETENTION_DAYS * 24 * 60 * 60 * 1000;
  const entries = Object.entries(state.items ?? {}).filter(([, value]) => {
    const seen = new Date(value.lastSeenAt ?? 0).getTime();
    return Number.isFinite(seen) && seen >= cutoff;
  });

  entries.sort((a, b) => new Date(b[1].lastSeenAt).getTime() - new Date(a[1].lastSeenAt).getTime());
  const pending = Object.entries(state.items ?? {}).filter(([, value]) =>
    value.pendingNewnessCheck || value.pendingDetailEvent);
  const pendingKeys = new Set(pending.map(([key]) => key));
  state.items = Object.fromEntries(pending.concat(
    entries.filter(([key]) => !pendingKeys.has(key)).slice(0, Math.max(0, MAX_STATE_ITEMS - pending.length))
  ));
}

async function main() {
  const startedAt = new Date();
  const nowMs = startedAt.getTime();
  const originalState = await readState();
  const nextState = clone(originalState);
  resetAndExpireNewness(nextState, nowMs);

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
    const startedIso = startedAt.toISOString();

    for (const item of found.values()) {
      if (item.status !== 'Ongoing') continue;

      const key = itemKey(city.name, item);
      const previous = nextState.items?.[key];

      if (!belongsToCityOrUnknown(item, city.name)) {
        if (previous) delete nextState.items[key];
        continue;
      }

      const unseen = !previous;
      const firstSeenAt = previous?.firstSeenAt ?? startedIso;
      const firstSeenMs = new Date(firstSeenAt).getTime();
      const referenceMs = Number.isFinite(firstSeenMs) ? firstSeenMs : nowMs;
      const itemWithKnownDate = {
        ...item,
        postedAt: item.postedAt ?? previous?.postedAt ?? null
      };

      const pendingBefore = Boolean(previous?.pendingNewnessCheck);
      const detailAttemptedBefore = Boolean(previous?.newnessDetailAttempted);
      const dateResolvedWhilePending = pendingBefore && Boolean(itemWithKnownDate.postedAt);
      const isNewNow = unseen && isRecentByPostedAt(itemWithKnownDate, nowMs);
      const wasNewWhenFirstSeen = dateResolvedWhilePending
        && isRecentByPostedAt(itemWithKnownDate, referenceMs);
      const isPriceDrop = Boolean(
        previous
        && item.price > 0
        && Number(previous.lastPrice) > 0
        && item.price < Number(previous.lastPrice)
      );

      let pendingNewnessCheck = pendingBefore;
      let newnessDetailAttempted = detailAttemptedBefore;
      let newnessDetailAttempts = Number(previous?.newnessDetailAttempts) || 0;
      let newnessUnknownExpired = Boolean(previous?.newnessUnknownExpired);

      if (dateResolvedWhilePending) {
        pendingNewnessCheck = false;
        newnessDetailAttempts = 0;
        newnessUnknownExpired = false;
      } else if (unseen && !itemWithKnownDate.postedAt) {
        pendingNewnessCheck = true;
        newnessDetailAttempted = false;
        newnessDetailAttempts = 0;
        newnessUnknownExpired = false;
      }

      let changeType = null;
      if (isPriceDrop) {
        changeType = 'price_drop';
      } else if (isNewNow || wasNewWhenFirstSeen) {
        changeType = 'new';
      } else if (unseen && !itemWithKnownDate.postedAt) {
        changeType = 'newness_check';
      } else if (pendingNewnessCheck && !itemWithKnownDate.postedAt) {
        changeType = 'newness_backlog';
      }

      const deferredEvent = previous?.pendingDetailEvent;
      if (!changeType && deferredEvent) changeType = deferredEvent.changeType;
      if (changeType) {
        stage1.push({
          ...itemWithKnownDate,
          changeType,
          previousPrice: isPriceDrop ? Number(previous.lastPrice) : (deferredEvent?.previousPrice ?? null),
          firstSeenAt
        });
      }

      nextState.items[key] = {
        ...previous,
        id: item.id,
        title: item.title,
        url: item.url,
        lastPrice: isPriceDrop ? previous.lastPrice : (item.price || previous?.lastPrice || null),
        postedAt: itemWithKnownDate.postedAt,
        firstSeenAt,
        pendingNewnessCheck,
        pendingDetailEvent: changeType === 'new' || changeType === 'price_drop'
          ? { changeType, previousPrice: isPriceDrop ? Number(previous.lastPrice) : (deferredEvent?.previousPrice ?? null) }
          : (previous?.pendingDetailEvent ?? null),
        newnessDetailAttempted,
        newnessDetailAttempts,
        newnessUnknownExpired,
        lastSeenAt: startedIso,
        city: city.name
      };
    }

    // Search disappearance must not remove a saved detail task.
    const queuedKeys = new Set(stage1.map(item => itemKey(city.name, item)));
    for (const [key, saved] of Object.entries(nextState.items)) {
      if (saved.city !== city.name || queuedKeys.has(key)) continue;
      if (!saved.pendingNewnessCheck && !saved.pendingDetailEvent) continue;
      stage1.push({
        ...saved,
        price: saved.lastPrice,
        status: 'Ongoing',
        changeType: saved.pendingDetailEvent?.changeType ?? 'newness_backlog',
        previousPrice: saved.pendingDetailEvent?.previousPrice ?? null
      });
    }
    // Fair retry order: unattempted tasks first, then the least recently tried.
    stage1.sort((a, b) => {
      const sa = nextState.items[itemKey(city.name, a)] ?? {};
      const sb = nextState.items[itemKey(city.name, b)] ?? {};
      return new Date(sa.detailLastAttemptAt ?? 0) - new Date(sb.detailLastAttemptAt ?? 0)
        || new Date(a.firstSeenAt ?? 0) - new Date(b.firstSeenAt ?? 0);
    });
    const eligible = stage1.filter(item => {
      const saved = nextState.items[itemKey(city.name, item)] ?? {};
      if (item.changeType === 'new' || item.changeType === 'price_drop') return true;
      const retryDelay = saved.newnessDetailAttempts >= 3 ? NEWNESS_RETRY_DELAY_MS : 0;
      return nowMs - new Date(saved.detailLastAttemptAt ?? 0).getTime() >= retryDelay;
    });
    const fresh = eligible.filter(item => item.changeType !== 'newness_backlog');
    const backlog = eligible.filter(item => item.changeType === 'newness_backlog');
    // New listings and price drops always precede recent unknown-date retries.
    const detailQueue = fresh.concat(backlog);
    // Bound runtime rather than dropping work at an arbitrary item count.
    const detailedItems = await detailBatch(detailQueue,
      Math.min(nowMs + SCAN_BUDGET_MS, Date.now() + DETAIL_BUDGET_PER_CITY_MS));
    let detailedCount = 0;
    for (const item of detailedItems) {
      detailedCount++;
      const stateKey = itemKey(city.name, item);
      nextState.items[stateKey].detailLastAttemptAt = new Date().toISOString();
      const stateBeforeDetail = nextState.items[stateKey] ?? {};

      if (item.detailError) {
        warnings.push(city.name + ': detail failed ' + item.id + ': ' + item.detailError);
        continue;
      }
      if (item.status !== 'Ongoing') {
        nextState.items[stateKey].pendingNewnessCheck = false;
        nextState.items[stateKey].pendingDetailEvent = null;
        nextState.items[stateKey].newnessProbeSkipped = false;
        continue;
      }
      if (!placeText(item)) {
        warnings.push(city.name + ': location unavailable after detail ' + item.id);
        continue;
      }
      if (!belongsToCity(item, city.name)) {
        delete nextState.items[stateKey];
        continue;
      }

      let changeType = item.changeType;
      const isNewnessProbe = changeType === 'newness_check' || changeType === 'newness_backlog';

      if (isNewnessProbe) {
        const firstSeenMs = new Date(item.firstSeenAt ?? stateBeforeDetail.firstSeenAt ?? startedIso).getTime();
        const referenceMs = Number.isFinite(firstSeenMs) ? firstSeenMs : nowMs;

        if (!item.postedAt) {
          const attempts = (Number(stateBeforeDetail.newnessDetailAttempts) || 0) + 1;
          const firstSeenAt = stateBeforeDetail.firstSeenAt ?? item.firstSeenAt ?? startedIso;
          nextState.items[stateKey] = {
            ...stateBeforeDetail,
            id: item.id,
            title: item.title,
            url: item.url,
            lastPrice: Number(item.price) || stateBeforeDetail.lastPrice || null,
            firstSeenAt,
            pendingNewnessCheck: true,
            newnessDetailAttempted: true,
            newnessDetailAttempts: attempts,
            newnessProbeSkipped: false,
            newnessUnknownExpired: false,
            lastSeenAt: startedIso,
            city: city.name
          };
          warnings.push(city.name + ': postedAt unavailable after detail ' + item.id + ' attempt ' + attempts);
          continue;
        }

        const recentAtFirstSeen = isRecentByPostedAt(item, referenceMs);
        nextState.items[stateKey] = {
          ...stateBeforeDetail,
          id: item.id,
          title: item.title,
          url: item.url,
          lastPrice: Number(item.price) || stateBeforeDetail.lastPrice || null,
          postedAt: item.postedAt,
          firstSeenAt: stateBeforeDetail.firstSeenAt ?? item.firstSeenAt ?? startedIso,
          pendingNewnessCheck: false,
          newnessDetailAttempted: true,
          newnessDetailAttempts: 0,
          newnessProbeSkipped: false,
          newnessUnknownExpired: false,
          lastSeenAt: startedIso,
          city: city.name
        };

        if (!recentAtFirstSeen) continue;
        changeType = 'new';
      } else if (changeType === 'new' && !isRecentByPostedAt(item, item.firstSeenAt ? new Date(item.firstSeenAt).getTime() : nowMs)) {
        // Search metadata said "new", but detail is authoritative when available.
        const baseline = nextState.items[stateKey] ?? {};
        nextState.items[stateKey] = {
          ...baseline,
          id: item.id,
          title: item.title,
          url: item.url,
          lastPrice: Number(item.price) || baseline.lastPrice || null,
          postedAt: item.postedAt ?? baseline.postedAt ?? null,
          pendingNewnessCheck: false,
          pendingDetailEvent: null,
          newnessDetailAttempted: true,
          lastSeenAt: startedIso,
          city: city.name
        };
        continue;
      }

      const currentPrice = Number(item.price) || 0;
      const previousState = nextState.items[stateKey] ?? {};
      nextState.items[stateKey] = {
        ...previousState,
        id: item.id,
        title: item.title,
        url: item.url,
        lastPrice: currentPrice || previousState.lastPrice || null,
        postedAt: item.postedAt ?? previousState.postedAt ?? null,
        firstSeenAt: previousState.firstSeenAt ?? item.firstSeenAt ?? startedIso,
        pendingNewnessCheck: false,
        pendingDetailEvent: null,
        newnessDetailAttempted: true,
        newnessUnknownExpired: false,
        lastSeenAt: startedIso,
        city: city.name
      };

      const specs = analyzeHardware(item);
      if (!specs.ok) continue;

      const text = (item.title ?? '') + '\n' + (item.description ?? '');
      const cpuHint = extractCpu(text);
      if (!cpuMeetsBaseline(cpuHint)) continue;
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
        cpuHint,
        cpuBaselinePass: true,
        gpuHint: specs.gpuHint,
        vramGB: specs.vramGB,
        ramGB: specs.ramGB,
        ambiguousMemoryGB: specs.ambiguousMemoryGB,
        hardwareReviewStatus: specs.hardwareReviewStatus,
        ramEvidence: specs.ramEvidence,
        vramEvidence: specs.vramEvidence,
        ambiguousMemoryEvidence: specs.ambiguousMemoryEvidence,
        storageGB: specs.storageGB,
        llmCapability: specs.llmCapability,
        llmReason: specs.llmReason,
        specConfidence: specs.confidence,
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
      deferredDetails: detailQueue.length - detailedCount,
      durationMs: Date.now() - cityStarted
    });
  }

  const waiting = Object.values(nextState.items).filter(item => item.pendingNewnessCheck || item.pendingDetailEvent);
  if (waiting.length) warnings.push('Detail queue retained for retry: ' + waiting.length);

  const uniqueCandidates = [];
  const seenCandidateIds = new Set();
  for (const candidate of candidates) {
    const key = candidate.id !== 'unknown' ? candidate.id : candidate.url;
    if (seenCandidateIds.has(key)) continue;
    seenCandidateIds.add(key);
    uniqueCandidates.push(candidate);
  }

  // Search failures do not invalidate independently completed detail tasks.
  {
    nextState.updatedAt = startedAt.toISOString();
    pruneState(nextState, nowMs);
    await fs.writeFile(STATE_PATH, JSON.stringify(nextState, null, 2) + '\n');
  }

  const result = {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    expectedScheduleMinutes: [7, 27, 47],
    scanWindowHours: WINDOW_HOURS,
    searchOrder: CITIES.map(city => city.name),
    health: {
      status: hardFailure ? 'failed' : 'ok',
      stateCommitted: true,
      warnings
    },
    rules: {
      newnessField: 'postedAt',
      boostedAtCreatesNewCandidate: false,
      onlyStatus: 'Ongoing',
      target: 'Codex + ChatGPT Work + coding + local LLM',
      priceGate: false,
      marketPriceCheck: 'deferred to ChatGPT; prefer >=10% below used-market average; insufficient market evidence => HOLD, do not auto-reject',
      storageGate: false,
      cpuBaseline: 'AMD Ryzen 7 PRO 7840U or better/equivalent',
      acceptedHardware: [
        'CONFIRMED: explicit RAM >=32GB OR explicit/model-confirmed VRAM >=16GB',
        'CONFIRMED: Ryzen AI Max with >=64GB shared/unified memory',
        'REVIEW: memory expression >=16GB (including RAM/램/렘/메모리/memory, 기가/G/GB variants or bare 16G/32기가) when RAM/VRAM cannot yet be distinguished'
      ],
      hardwareReviewStates: ['PASS_RAM', 'PASS_VRAM', 'PASS_RAM_VRAM', 'PASS_SHARED64', 'CHECK_MEMORY_GPU']
    },
    stats: {
      cities: cityRuns,
      finalCandidateCount: uniqueCandidates.length,
      stateItemCount: Object.keys(nextState.items ?? {}).length,
      pendingNewnessCount: Object.values(nextState.items ?? {}).filter(item => item?.pendingNewnessCheck).length,
      pendingUnattemptedCount: Object.values(nextState.items ?? {}).filter(item => item?.pendingNewnessCheck && !item?.newnessDetailAttempted).length,
      pendingAttemptedCount: Object.values(nextState.items ?? {}).filter(item => item?.pendingNewnessCheck && item?.newnessDetailAttempted).length,
      skippedUnknownProbeCount: Object.values(nextState.items ?? {}).filter(item => item?.newnessProbeSkipped).length,
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
    expectedScheduleMinutes: [7, 27, 47],
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
