/** Build-only public release information. Never imported by the reader. No network/API calls. */
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {pathToFileURL} from 'node:url';

export const INFO_PATHS = ['/engine-info.json','/engine-info.html','/llms.txt','/version.json'];
const canonical = value => JSON.stringify(value, (_, x) => x && !Array.isArray(x) && typeof x === 'object' ? Object.fromEntries(Object.entries(x).sort(([a],[b])=>a.localeCompare(b))) : x);
const read = file => JSON.parse(fs.readFileSync(file,'utf8'));
export const sha256 = value => createHash('sha256').update(value).digest('hex');
const need = (condition, message) => {if(!condition)throw Error('Release metadata: '+message);};
export const releaseId = version => `${version.version}-${version.engineVersion}-${version.sourceDigest.slice(0,16)}`;
const equal = (a,b,label) => need(canonical(a)===canonical(b),label+' mismatch');
export function walk(dir) {
  if(!fs.existsSync(dir))return [];
  return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(dir,e.name)):e.isFile()?[path.join(dir,e.name)]:[]);
}
export function sourceDigest(root) {
  const excluded = rel => ['public/sw.js','public/version.json','public/engine-info.json','public/engine-info.html','public/llms.txt','tests/ci-report.json'].includes(rel) || /(?:^|\/)(?:node_modules|__pycache__|[^/]*results)(?:\/|$)/.test(rel) || /\.(?:png|jpe?g|pdf|zip|log|pyc)$/.test(rel) && rel.startsWith('tests/');
  const files=[...['public','netlify','scripts','engine','tests'].flatMap(n=>walk(path.join(root,n))),...['package.json','package-lock.json','netlify.toml','index.html','AGENTS.md','CHATBOOK_ENGINE.md'].map(n=>path.join(root,n)).filter(f=>fs.existsSync(f))];
  const hash=createHash('sha256');
  for(const file of [...new Set(files)].sort()){
    const rel=path.relative(root,file).replaceAll('\\','/');if(excluded(rel))continue;
    hash.update(rel);hash.update('\0');hash.update(fs.readFileSync(file));hash.update('\0');
  }
  return hash.digest('hex');
}
export async function engineState(publicDir) {
  const file=path.join(publicDir,'engine/learning.mjs');
  const module=await import(pathToFileURL(file).href+'?release='+sha256(fs.readFileSync(file)));
  const defaults=read(path.join(publicDir,'engine/defaults.json'));
  const policy=read(path.join(publicDir,'engine/learning-policy.json'));
  equal(defaults,module.DEFAULTS,'runtime/defaults.json');
  equal(policy.version,module.VERSION,'runtime/policy version');
  equal(policy.entrypoint,'learning.mjs','policy entrypoint');
  for(const [key,value] of Object.entries(module.POLICY))equal(policy[key],value,'policy '+key);
  need(/^\d+\.\d+\.\d+(?:[-+][\w.-]+)?$/.test(module.VERSION),'invalid engine version');
  return {version:module.VERSION,profile:module.PROFILE,entrypoint:'/engine/learning.mjs',policy:module.POLICY,defaults:module.DEFAULTS,exports:Object.keys(module).sort()};
}
function assets(publicDir) {
  const files=[...walk(path.join(publicDir,'engine')),...['core.js','views.js','reader.js','exports.js','app.js','library.js','content/catalog.json'].map(n=>path.join(publicDir,n)).filter(f=>fs.existsSync(f))];
  return Object.fromEntries(files.sort().map(f=>['/'+path.relative(publicDir,f).replaceAll('\\','/'),sha256(fs.readFileSync(f))]));
}
export async function createInfo(root,version,evidence) {
  const engine=await engineState(path.join(root,'public'));
  equal(version.version,read(path.join(root,'package.json')).version,'app version');
  equal(version.engineVersion,engine.version,'engine version');
  equal(version.releaseId,releaseId(version),'release ID');
  need(/^[a-f0-9]{64}$/.test(version.sourceDigest),'invalid source digest');
  equal(version.sourceDigest,sourceDigest(root),'actual source digest');
  equal(evidence.sourceDigest,version.sourceDigest,'test evidence source');
  need(evidence.command==='npm test'&&evidence.status==='passed'&&evidence.exitCode===0&&Number.isFinite(Date.parse(evidence.checkedAt)),'missing fresh successful test evidence');
  return {schemaVersion:1,kind:'read-only-release-information',generatedAt:version.builtAt,
    app:{name:'챗북',version:version.version,homepage:'https://chatbook-library-20260923.netlify.app'},
    release:{id:version.releaseId,sourceDigest:version.sourceDigest,sourceSha:version.sourceSha,sourceBranch:'chatbook-app-20260923',buildRunId:version.runId||null,deployId:version.deployId||null},
    engine,
    verification:{buildTests:evidence,productionEndToEnd:{status:'not-run-by-this-build'},semanticAccuracy:{status:'not-certified-by-structural-tests'},physicalGalaxy:{status:'not-tested-by-this-build'},longVideoEndToEnd:{status:'not-tested-by-this-build'}},
    limitations:[
      '공개 안내서는 소스와 검사 기록의 요약이며 실제 영상 품질이나 무오류를 보증하지 않습니다.',
      '영상 길이·동시성 등 defaults는 설정값이며 해당 길이의 실제 영상 성공 기록이 아닙니다.',
      '이 안내 생성기는 영상 분석·번역을 실행하지 않습니다. URL 입력만으로 실행 가능한 서버 기능 여부는 별도 API·운영 검증이 필요합니다.',
      'engine/runner.mjs 등 제작 도구는 승인된 작업 환경에서 사용합니다. 공개 웹페이지가 제작 권한을 주지 않습니다.',
      'YouTube 원본 프레임 확보, 원음 전체 대조, 독자의 이해도는 이 빌드 검사에 포함되지 않습니다.'
    ],
    maintenance:{generated:true,command:'npm run build',checkCommand:'node scripts/verify-release.mjs',runtimeDependency:false,updateRule:'Actual runtime defaults/policy and fresh tests are read on every build. Keep policy JSON in sync with the implementation; mismatches fail the build.',rollbackRule:'Publish the app and all generated metadata from the same build. Older releases predating this feature may have no metadata.',sourceRepository:'https://github.com/irewon1-lgtm/Myapp/tree/chatbook-app-20260923',sourceGuide:'https://github.com/irewon1-lgtm/Myapp/blob/chatbook-app-20260923/AGENTS.md'},
    assetSha256:assets(path.join(root,'public'))};
}
const escape = value => String(value).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
export function infoHtml(info) {
  return `<!doctype html><html lang="ko"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>챗북 엔진 정보</title><link rel="alternate" type="application/json" href="/engine-info.json"><style>body{font-family:system-ui,sans-serif;max-width:850px;margin:40px auto;padding:0 20px;line-height:1.8;overflow-wrap:anywhere}pre{white-space:pre-wrap;border:1px solid;padding:16px}a{color:inherit}small{display:block}</style></head><body data-release-id="${escape(info.release.id)}"><nav><a href="/">챗북으로 돌아가기</a> · <a href="/engine-info.json">AI용 JSON</a> · <a href="/version.json">배포 기록</a></nav><h1>챗북 엔진 정보</h1><p>앱 ${escape(info.app.version)} · 교재 제작 엔진 ${escape(info.engine.version)}</p><p>이 페이지는 매 빌드에서 자동 생성됩니다. 서버 상태의 실시간 진단이나 수정 권한을 제공하는 관리 페이지가 아닙니다.</p><h2>현재 배포 묶음</h2><p>${escape(info.release.id)}</p><small>생성 시각: ${escape(info.generatedAt)} · 소스 SHA: ${escape(info.release.sourceSha)}</small><h2>실제로 실행한 빌드 검사</h2><p>npm test: ${escape(info.verification.buildTests.status)} · ${escape(info.verification.buildTests.checkedAt)}</p><h2>확인 범위와 한계</h2><ul>${info.limitations.map(x=>`<li>${escape(x)}</li>`).join('')}</ul><h2>현재 제작 정책과 설정</h2><p>아래 값은 실제 모듈에서 가져왔습니다. 최대 영상 길이는 지원 성공률의 보증이 아닙니다.</p><pre>${escape(JSON.stringify({policy:info.engine.policy,defaults:info.engine.defaults},null,2))}</pre><h2>유지보수</h2><p><a href="${escape(info.maintenance.sourceGuide)}">소스 작업 규칙</a> · <a href="${escape(info.maintenance.sourceRepository)}">소스 저장소</a></p><p>공개 정보에는 비밀키나 사용자 독서 기록을 포함하지 않습니다. 안내 파일을 읽지 못해도 서재·리더·퀴즈는 이를 필수 입력으로 사용하지 않습니다.</p></body></html>`;
}
export function writeInfo(dist,info) {
  fs.writeFileSync(path.join(dist,'engine-info.json'),JSON.stringify(info,null,2)+'\n');
  fs.writeFileSync(path.join(dist,'engine-info.html'),infoHtml(info));
  fs.writeFileSync(path.join(dist,'llms.txt'),`# Chatbook\n\nRelease: ${info.release.id}\n\n- [Current engine information](/engine-info.json): Generated on each build from runtime policy/defaults and fresh test results.\n- [Readable engine information](/engine-info.html)\n- [Build identity](/version.json): Compare releaseId and sourceDigest with engine-info.json.\n- [Maintenance source](${info.maintenance.sourceGuide})\n\nRead-only metadata is not proof of full-video quality, live service health, or edit authorization. Engine limits are configuration, not measured successful video duration. This metadata generator does not run video analysis or translation; inspect authorized API and production evidence separately.\n`);
  const index=path.join(dist,'index.html');let html=fs.readFileSync(index,'utf8');
  html=html.replace('</head>',`<meta name="chatbook-release" content="${escape(info.release.id)}"><link rel="alternate" type="application/json" title="챗북 엔진 정보" href="/engine-info.json"></head>`);
  html=html.replace('</noscript>',' <a href="/engine-info.html">현재 엔진 정보 및 확인 범위</a></noscript>');fs.writeFileSync(index,html);
  const headers=path.join(dist,'_headers'),old=fs.existsSync(headers)?fs.readFileSync(headers,'utf8'):'';
  fs.writeFileSync(headers,old+'\n'+INFO_PATHS.map(p=>`${p}\n  Cache-Control: no-cache, no-store, must-revalidate\n  X-Content-Type-Options: nosniff\n`).join('\n'));
}
export async function verifyRelease(dist) {
  const version=read(path.join(dist,'version.json')),info=read(path.join(dist,'engine-info.json'));
  need(info.schemaVersion===1,'schema unsupported');
  equal(version.releaseId,releaseId(version),'version identity');
  equal(info.release.id,version.releaseId,'app/info identity');
  equal(info.release.sourceDigest,version.sourceDigest,'app/info digest');
  equal(info.app.version,version.version,'app/info version');
  equal(info.engine.version,version.engineVersion,'app/info engine version');
  equal(info.release.sourceSha,version.sourceSha,'source SHA');
  equal(info.generatedAt,version.builtAt,'build timestamp');
  equal(info.engine,await engineState(dist),'published engine');
  equal(info.assetSha256,assets(dist),'published files');
  equal(info.verification.buildTests.sourceDigest,version.sourceDigest,'test evidence digest');
  need(info.verification.buildTests.status==='passed'&&info.verification.buildTests.exitCode===0,'build tests did not pass');
  need(fs.readFileSync(path.join(dist,'index.html'),'utf8').includes(`name="chatbook-release" content="${version.releaseId}"`),'reader identity missing');
  need(fs.readFileSync(path.join(dist,'engine-info.html'),'utf8').includes(`data-release-id="${version.releaseId}"`),'human-readable identity mismatch');
  need(fs.readFileSync(path.join(dist,'llms.txt'),'utf8').includes(version.releaseId),'discovery identity mismatch');
  return {status:'passed',releaseId:version.releaseId,assetCount:Object.keys(info.assetSha256).length};
}
export function serviceWorker(digest,urls) {
  const paths=urls.filter(p=>!INFO_PATHS.includes(new URL(p,'https://local/').pathname));
  return `const NAME='chatbook-offline-${digest.slice(0,12)}';const URLS=${JSON.stringify(paths)};const INFO=${JSON.stringify(INFO_PATHS)};self.addEventListener('install',e=>{e.waitUntil(caches.open(NAME).then(c=>c.addAll(URLS)).then(()=>self.skipWaiting()));});self.addEventListener('activate',e=>e.waitUntil((async()=>{for(const name of await caches.keys()){if(!name.startsWith('chatbook-offline-'))continue;const cache=await caches.open(name);for(const req of await cache.keys())if(INFO.includes(new URL(req.url).pathname))await cache.delete(req);}await self.clients.claim();})()));self.addEventListener('fetch',e=>{if(e.request.method!=='GET')return;const u=new URL(e.request.url);if(u.origin!==location.origin||u.pathname.includes('/api/')||INFO.includes(u.pathname))return;e.respondWith(fetch(e.request).then(r=>{if(r.ok){const clone=r.clone();caches.open(NAME).then(c=>c.put(e.request,clone));}return r;}).catch(async()=>await caches.match(e.request)||(e.request.mode==='navigate'?await caches.match('./index.html'):new Response('',{status:503}))));});self.addEventListener('notificationclick',e=>{e.notification.close();e.waitUntil(clients.openWindow(e.notification.data?.url||'./#review'));});`;
}
