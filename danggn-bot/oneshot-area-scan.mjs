import fs from 'node:fs/promises';

const BASE = 'https://www.daangn.com';
const SEARCH = BASE + '/kr/buy-sell/';
const UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36';
const WINDOW_HOURS = 48;
const SEARCH_LIMIT = 80;
const FAST_MODE = process.env.FAST_MODE === '1';
const DETAIL_LIMIT_PER_REGION = FAST_MODE ? 6 : 10;
const QUERIES = ['노트북', '그램', '갤럭시북', 'ThinkPad'];

const TARGETS = {
  seongnam: { name: '성남시', province: '경기도' },
  gwangjin: { name: '광진구', province: '서울특별시' },
  songpa: { name: '송파구', province: '서울특별시' },
  gangnam: { name: '강남구', province: '서울특별시' },
  seocho: { name: '서초구', province: '서울특별시' },
  gangdong: { name: '강동구', province: '서울특별시' }
};

const REGION_SLUGS = {
  "gangnam": [
    "역삼동-6035",
    "대치동-6032",
    "청담동-386",
    "논현동-6031",
    "압구정동-385",
    "삼성동-6034",
    "역삼1동-392",
    "신사동-382",
    "역삼2동-393",
    "개포동-6030",
    "도곡동-6033",
    "논현1동-383",
    "대치1동-389",
    "자곡동-6038",
    "삼성2동-388",
    "일원동-6037",
    "대치4동-391",
    "삼성1동-387",
    "개포1동-396",
    "도곡1동-394",
    "개포4동-398",
    "대치2동-390",
    "세곡동-399",
    "수서동-403",
    "논현2동-384",
    "개포2동-397",
    "개포3동-402",
    "일원본동-400",
    "도곡2동-395",
    "일원1동-401",
    "율현동-6036"
  ],
  "gwangjin": [
    "화양동-72",
    "자양동-6060",
    "구의동-6059",
    "광장동-79",
    "군자동-73",
    "중곡동-6061",
    "자양제4동-83",
    "자양제1동-80",
    "구의제1동-84",
    "자양제3동-82",
    "능동-78",
    "구의제3동-86",
    "중곡제4동-77",
    "구의제2동-85",
    "중곡제1동-74",
    "자양제2동-81",
    "중곡제2동-75",
    "중곡제3동-76"
  ],
  "seocho": [
    "서초동-6128",
    "서초4동-366",
    "잠원동-367",
    "반포동-6126",
    "방배동-6127",
    "양재동-6130",
    "서초3동-365",
    "서초1동-363",
    "서초2동-364",
    "반포1동-369",
    "반포본동-368",
    "방배본동-373",
    "우면동-6132",
    "방배4동-377",
    "양재2동-379",
    "양재1동-378",
    "반포4동-372",
    "반포2동-370",
    "반포3동-371",
    "방배2동-375",
    "방배1동-374",
    "내곡동-380",
    "방배3동-376",
    "신원동-6129",
    "원지동-6133",
    "염곡동-6131"
  ],
  "seongnam": [
    "정자동-1339",
    "백현동-1352",
    "삼평동-1351",
    "성남동-1323",
    "서현동-4502",
    "판교동-1350",
    "구미동-1355",
    "야탑동-4505",
    "위례동-3971",
    "금곡동-1353",
    "분당동-1335",
    "서현1동-1343",
    "창곡동-4517",
    "운중동-1356",
    "이매동-4507",
    "수내동-4504",
    "금광동-4520",
    "태평동-4518",
    "정자1동-1340",
    "구미1동-1354",
    "야탑1동-1347",
    "수내1동-1336",
    "하대원동-1332",
    "복정동-1318",
    "신흥동-4514",
    "상대원동-4521",
    "중앙동-1324",
    "신흥2동-1307",
    "도촌동-1333",
    "대장동-4500",
    "야탑3동-1349",
    "신흥1동-1306",
    "상대원1동-1329",
    "단대동-1315",
    "이매2동-1346",
    "서현2동-1344",
    "은행2동-1328",
    "태평2동-1310",
    "태평1동-1309",
    "금광2동-1326",
    "여수동-4522",
    "고등동-1320",
    "이매1동-1345",
    "수진동-4513",
    "정자2동-1341",
    "정자3동-1342",
    "야탑2동-1348",
    "수내3동-1338",
    "수진1동-1313",
    "금광1동-1325",
    "태평3동-1311",
    "태평4동-1312",
    "신흥3동-1308",
    "상대원3동-1331",
    "수진2동-1314",
    "양지동-1317",
    "수내2동-1337",
    "은행1동-1327",
    "산성동-1316",
    "은행동-4523",
    "상대원2동-1330",
    "시흥동-1321",
    "심곡동-4515",
    "궁내동-4499",
    "금토동-4509",
    "율동-4506",
    "신촌동-1319",
    "사송동-4511",
    "오야동-4516",
    "상적동-4512",
    "동원동-4501",
    "갈현동-4519",
    "석운동-4503",
    "하산운동-4508",
    "둔전동-4510"
  ],
  "gangdong": [
    "천호동-6044",
    "길동-448",
    "상일동-434",
    "성내동-6042",
    "강일동-433",
    "암사동-6043",
    "둔촌동-6040",
    "고덕동-6039",
    "명일동-6041",
    "천호제1동-442",
    "천호제3동-444",
    "천호제2동-443",
    "암사제1동-439",
    "성내제2동-446",
    "명일제1동-435",
    "성내제3동-447",
    "고덕제1동-437",
    "성내제1동-445",
    "둔촌제2동-450",
    "암사제3동-441",
    "고덕제2동-438",
    "명일제2동-436",
    "둔촌제1동-449",
    "암사제2동-440"
  ],
  "songpa": [
    "문정동-6184",
    "잠실동-6188",
    "석촌동-417",
    "위례동-425",
    "오금동-414",
    "잠실본동-426",
    "삼전동-418",
    "잠실2동-427",
    "잠실4동-429",
    "송파동-6186",
    "방이동-6185",
    "가락동-6181",
    "잠실3동-428",
    "가락본동-419",
    "장지동-424",
    "방이2동-412",
    "가락1동-420",
    "거여동-6182",
    "잠실6동-430",
    "가락2동-421",
    "풍납동-6189",
    "방이1동-411",
    "문정1동-422",
    "송파1동-415",
    "마천동-6183",
    "신천동-6187",
    "오륜동-413",
    "문정2동-423",
    "풍납2동-406",
    "송파2동-416",
    "거여2동-408",
    "잠실7동-431",
    "마천1동-409",
    "거여1동-407",
    "풍납1동-405",
    "마천2동-410"
  ]
};
const FAST_REGION_SLUGS = {
  seongnam: ['정자동-1339', '판교동-1350', '야탑동-4505', '성남동-1323', '신흥동-4514', '위례동-3971'],
  gwangjin: ['자양동-6060', '구의동-6059', '중곡동-6061'],
  songpa: ['잠실동-6188', '문정동-6184', '오금동-414'],
  gangnam: ['역삼동-6035', '대치동-6032', '개포동-6030'],
  seocho: ['서초동-6128', '반포동-6126', '방배동-6127', '양재동-6130'],
  gangdong: ['천호동-6044', '고덕동-6039', '둔촌동-6040']
};

const slug = process.env.TARGET_SLUG;
const target = TARGETS[slug];
if (!target) throw new Error('Unknown TARGET_SLUG: ' + slug);

const chunkIndex = Number.parseInt(process.env.CHUNK_INDEX ?? '0', 10);
const chunkCount = Number.parseInt(process.env.CHUNK_COUNT ?? '1', 10);
if (!Number.isInteger(chunkIndex) || !Number.isInteger(chunkCount) || chunkCount < 1 || chunkIndex < 0 || chunkIndex >= chunkCount) {
  throw new Error('Invalid chunk settings: ' + JSON.stringify({ chunkIndex, chunkCount }));
}

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

async function getText(url, attempts = 4) {
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
      if (!response.ok) {
        const error = new Error('HTTP ' + response.status + ' ' + response.statusText);
        error.status = response.status;
        throw error;
      }
      return await response.text();
    } catch (e) {
      lastError = e;
      if (attempt < attempts) {
        const status = Number(e?.status) || 0;
        const delay = (status === 429 || status === 403) ? 1500 * attempt : 400 * attempt;
        await new Promise(r => setTimeout(r, delay));
      }
    }
  }
  throw lastError;
}

async function resolveRegions() {
  const slugs = (FAST_MODE ? FAST_REGION_SLUGS[slug] : REGION_SLUGS[slug]) ?? [];
  return slugs.map(value => {
    const m = String(value).match(/^(.*)-(\d+)$/);
    if (!m) throw new Error('Invalid fixed region slug: ' + value);
    return { slug: value, name: m[1], id: m[2] };
  });
}

async function searchOne(query, region) {
  try {
    const routeUrl = new URL(BASE + '/kr/buy-sell/all/');
    routeUrl.searchParams.set('in', region.slug);
    routeUrl.searchParams.set('search', query);
    routeUrl.searchParams.set('only_on_sale', 'true');
    routeUrl.searchParams.set('_data', 'routes/kr.buy-sell._index');
    const text = await getText(routeUrl);
    if (text.trim()) {
      const data = JSON.parse(text);
      const rows = data?.allPage?.fleamarketArticles;
      if (Array.isArray(rows)) {
        return rows.map(item => normalizeArticle(item, query, region.slug)).slice(0, SEARCH_LIMIT);
      }
    }
  } catch {}

  const url = new URL(SEARCH);
  url.searchParams.set('search', query);
  url.searchParams.set('in', region.slug);
  url.searchParams.set('only_on_sale', 'true');
  const html = await getText(url);
  return parseSearch(html, query, region.slug).slice(0, SEARCH_LIMIT);
}

async function detailOne(item) {
  try {
    const routeUrl = item.url.replace(/\/$/, '') + '/?_data=routes%2Fkr.buy-sell.%24buy_sell_id';
    const routeText = await getText(routeUrl);
    if (routeText.trim()) {
      const data = JSON.parse(routeText);
      const product = data?.product ?? data?.article ?? data;
      if (product && typeof product === 'object') {
        return {
          ...item,
          description: product.content ?? product.description ?? '',
          location: product.locationName ?? product.region?.name ?? item.location,
          regionPath: [product.region?.name1, product.region?.name2, product.region?.name3].filter(Boolean).join(' ') || item.regionPath,
          status: product.status ?? item.status,
          price: priceNum(product.price ?? item.price),
          postedAt: firstTimestamp(product, ['createdAt','created_at','publishedAt','published_at','dateCreated','datePublished']) ?? item.postedAt,
          boostedAt: firstTimestamp(product, ['boostedAt','boosted_at','bumpedAt','bumped_at']) ?? item.boostedAt
        };
      }
    }
  } catch {}

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
const allRegions = await resolveRegions();
if (!allRegions.length) throw new Error('No regions resolved for ' + target.name);

const regions = allRegions.filter((_, index) => index % chunkCount === chunkIndex);
if (!regions.length) throw new Error('No regions assigned to chunk ' + chunkIndex + '/' + chunkCount + ' for ' + target.name);

const tasks = [];
for (const region of regions) for (const query of QUERIES) tasks.push({ region, query });

const searched = await mapLimit(tasks, 1, async ({region, query}) => {
  try {
    const rows = await searchOne(query, region);
    await new Promise(r => setTimeout(r, 300));
    return rows;
  } catch (e) {
    await new Promise(r => setTimeout(r, 600));
    return [{ _searchError: String(e), sourceRegion: region.slug, sourceQuery: query }];
  }
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

const detailBase = [...found.values()]
  .filter(item => item.status === 'Ongoing')
  .filter(item => !item.price || (item.price >= 100000 && item.price <= 1500000))
  .filter(item => !item.postedAt || isRecent(item, nowMs))
  .filter(item => {
    const p = ((item.regionPath ?? '') + ' ' + (item.location ?? '')).trim();
    return !p || p.includes(target.name);
  });

const knownRecent = detailBase
  .filter(item => item.postedAt && isRecent(item, nowMs));

const unknownByRegion = new Map();
for (const item of detailBase.filter(item => !item.postedAt)) {
  const key = String(item.sourceRegion ?? 'unknown');
  if (!unknownByRegion.has(key)) unknownByRegion.set(key, []);
  const bucket = unknownByRegion.get(key);
  if (bucket.length < DETAIL_LIMIT_PER_REGION) bucket.push(item);
}

const detailQueue = [];
const seenDetail = new Set();
for (const item of [...knownRecent, ...[...unknownByRegion.values()].flat()]) {
  const key = item.id !== 'unknown' ? item.id : item.url;
  if (seenDetail.has(key)) continue;
  seenDetail.add(key);
  detailQueue.push(item);
}

const detailed = await mapLimit(detailQueue, 2, detailOne);
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
  chunkIndex,
  chunkCount,
  totalRegionCount: allRegions.length,
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

const outPath = 'oneshot-result-' + slug + '-' + chunkIndex + '.json';
await fs.writeFile(outPath, JSON.stringify(result, null, 2) + '\n');
console.log('RESULT_SUMMARY=' + JSON.stringify({
  target: target.name,
  chunkIndex,
  chunkCount,
  totalRegionCount: result.totalRegionCount,
  regionCount: result.regionCount,
  searchTaskCount: result.searchTaskCount,
  searchErrorCount: result.searchErrorCount,
  uniqueListingCount: result.uniqueListingCount,
  recentListingCount: result.recentListingCount,
  candidateCount: result.candidateCount
}));
console.log('RESULT_JSON=' + JSON.stringify(result));
