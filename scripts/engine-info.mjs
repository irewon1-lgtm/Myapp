/** Build-only release information. The reader never imports this module. */
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {pathToFileURL} from 'node:url';
import {SPEC_PATHS,canonical,escape,writeSpec,verifySpec} from './current-spec.mjs';
import {serviceWorker as worker} from './offline-worker.mjs';
export const INFO_PATHS=['/engine-info.json','/engine-info.html','/engine-info','/engine-info/','/llms.txt','/version.json',...SPEC_PATHS];
const read=file=>JSON.parse(fs.readFileSync(file,'utf8'));
export const sha256=value=>createHash('sha256').update(value).digest('hex');
const need=(condition,message)=>{if(!condition)throw Error('Release metadata: '+message);};
export const releaseId=version=>`${version.version}-${version.engineVersion}-${version.sourceDigest.slice(0,16)}`;
const equal=(a,b,label)=>need(canonical(a)===canonical(b),label+' mismatch');
export function walk(dir){
  if(!fs.existsSync(dir))return [];
  return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(dir,e.name)):e.isFile()?[path.join(dir,e.name)]:[]);
}
export function sourceDigest(root){
  const excluded=rel=>['public/sw.js','tests/ci-report.json',...INFO_PATHS.map(p=>'public'+p)].includes(rel)||/(?:^|\/)(?:node_modules|__pycache__|[^/]*results)(?:\/|$)/.test(rel)||(/\.(?:png|jpe?g|pdf|zip|log|pyc)$/.test(rel)&&rel.startsWith('tests/'));
  const files=[...['public','netlify','scripts','engine','tests','.github/workflows'].flatMap(n=>walk(path.join(root,n))),...['package.json','package-lock.json','netlify.toml','index.html','AGENTS.md','CHATBOOK_ENGINE.md'].map(n=>path.join(root,n)).filter(f=>fs.existsSync(f))];
  const hash=createHash('sha256');
  for(const file of [...new Set(files)].sort()){
    const rel=path.relative(root,file).replaceAll('\\','/');if(excluded(rel))continue;
    hash.update(rel);hash.update('\0');hash.update(fs.readFileSync(file));hash.update('\0');
  }
  return hash.digest('hex');
}
export async function engineState(publicDir){
  const file=path.join(publicDir,'engine/learning.mjs');
  const module=await import(pathToFileURL(file).href+'?release='+sha256(fs.readFileSync(file)));
  const defaults=read(path.join(publicDir,'engine/defaults.json')),policy=read(path.join(publicDir,'engine/learning-policy.json'));
  equal(defaults,module.DEFAULTS,'runtime/defaults.json');equal(policy.version,module.VERSION,'runtime/policy version');equal(policy.entrypoint,'learning.mjs','policy entrypoint');
  for(const[key,value]of Object.entries(module.POLICY))equal(policy[key],value,'policy '+key);
  need(/^\d+\.\d+\.\d+(?:[-+][\w.-]+)?$/.test(module.VERSION),'invalid engine version');
  return {version:module.VERSION,profile:module.PROFILE,entrypoint:'/engine/learning.mjs',policy:module.POLICY,defaults:module.DEFAULTS,exports:Object.keys(module).sort()};
}
function assets(publicDir){
  const files=[...walk(path.join(publicDir,'engine')),...['core.js','views.js','reader.js','exports.js','app.js','library.js','content/catalog.json'].map(n=>path.join(publicDir,n)).filter(f=>fs.existsSync(f))];
  return Object.fromEntries(files.sort().map(f=>['/'+path.relative(publicDir,f).replaceAll('\\','/'),sha256(fs.readFileSync(f))]));
}
export async function createInfo(root,version,evidence){
  const engine=await engineState(path.join(root,'public'));
  equal(version.version,read(path.join(root,'package.json')).version,'app version');equal(version.engineVersion,engine.version,'engine version');equal(version.releaseId,releaseId(version),'release ID');
  need(/^[a-f0-9]{64}$/.test(version.sourceDigest),'invalid source digest');equal(version.sourceDigest,sourceDigest(root),'actual source digest');equal(evidence.sourceDigest,version.sourceDigest,'test evidence source');
  need(evidence.command==='npm test'&&evidence.status==='passed'&&evidence.exitCode===0&&Number.isFinite(Date.parse(evidence.checkedAt)),'missing fresh successful test evidence');
  return {schemaVersion:1,kind:'read-only-release-information',generatedAt:version.builtAt,
    app:{name:'챗북',version:version.version,homepage:'https://chatbook-library-20260923.netlify.app'},
    release:{id:version.releaseId,sourceDigest:version.sourceDigest,sourceSha:version.sourceSha,sourceBranch:'chatbook-app-20260923',buildRunId:version.runId||null,deployId:version.deployId||null},engine,
    authoring:{current:'/engine-spec.json',readable:'/engine-spec.html',runtimeDependency:false},
    verification:{buildTests:evidence,productionEndToEnd:{status:'not-run-by-this-build'},semanticAccuracy:{status:'not-certified-by-structural-tests'},physicalGalaxy:{status:'not-tested-by-this-build'},longVideoEndToEnd:{status:'not-tested-by-this-build'}},
    limitations:['공개 안내서는 소스와 검사 기록의 요약이며 실제 영상 품질이나 무오류를 보증하지 않습니다.','영상 길이·동시성 등 defaults는 설정값이며 해당 길이의 실제 영상 성공 기록이 아닙니다.','이 안내 생성기는 영상 분석·번역을 실행하지 않습니다. URL 입력만으로 실행 가능한 서버 기능 여부는 별도 API·운영 검증이 필요합니다.','제작 도구는 승인된 작업 환경에서 사용합니다. 공개 웹페이지가 제작 권한을 주지 않습니다.','YouTube 원본 프레임 확보, 원음 전체 대조, 독자의 이해도는 이 빌드 검사에 포함되지 않습니다.'],
    maintenance:{generated:true,command:'npm run build',checkCommand:'node scripts/verify-release.mjs',runtimeDependency:false,updateRule:'Runtime policy/defaults, current authoring rules and one example are generated with fresh tests in the same build. Edit the source policy, not generated files.',rollbackRule:'Publish or roll back the complete tested bundle. The currently published spec is authoritative, not the numerically highest historical version.',sourceRepository:'https://github.com/irewon1-lgtm/Myapp/tree/chatbook-app-20260923',sourceGuide:'https://github.com/irewon1-lgtm/Myapp/blob/chatbook-app-20260923/AGENTS.md'},assetSha256:assets(path.join(root,'public'))};
}
export function infoHtml(info){
  return `<!doctype html><html lang="ko"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>챗북 엔진 정보</title><link rel="alternate" type="application/json" href="/engine-info.json"><style>body{font-family:system-ui,sans-serif;max-width:850px;margin:40px auto;padding:0 20px;line-height:1.8;overflow-wrap:anywhere}pre{white-space:pre-wrap;border:1px solid;padding:16px}a{color:inherit}small{display:block}</style></head><body data-release-id="${escape(info.release.id)}"><nav><a href="/">챗북</a> · <a href="/engine-spec.html">현재 교재 제작 기준·대표 예시</a> · <a href="/engine-info.json">배포 JSON</a></nav><h1>챗북 엔진 정보</h1><p>앱 ${escape(info.app.version)} · 교재 제작 엔진 ${escape(info.engine.version)}</p><p>다른 방에서 교재를 만들 때는 <a href="/engine-spec.json">현재 제작 규격</a>을 먼저 읽습니다. 이 페이지는 배포 정보이며 생성 API나 관리 권한이 아닙니다.</p><h2>현재 배포 묶음</h2><p>${escape(info.release.id)}</p><small>생성 시각: ${escape(info.generatedAt)} · 소스 SHA: ${escape(info.release.sourceSha)}</small><h2>실행한 빌드 검사</h2><p>npm test: ${escape(info.verification.buildTests.status)} · ${escape(info.verification.buildTests.checkedAt)}</p><h2>확인 범위</h2><ul>${info.limitations.map(x=>`<li>${escape(x)}</li>`).join('')}</ul><h2>실행 코드의 정책과 설정</h2><pre>${escape(JSON.stringify({policy:info.engine.policy,defaults:info.engine.defaults},null,2))}</pre><h2>유지보수</h2><p><a href="${escape(info.maintenance.sourceGuide)}">소스 작업 규칙</a> · <a href="${escape(info.maintenance.sourceRepository)}">소스 저장소</a></p><p>안내 파일을 읽지 못해도 서재·리더·퀴즈는 계속 사용할 수 있습니다.</p></body></html>`;
}
export function writeInfo(dist,info){
  fs.writeFileSync(path.join(dist,'engine-info.json'),JSON.stringify(info,null,2)+'\n');
  fs.writeFileSync(path.join(dist,'engine-info.html'),infoHtml(info));
  const spec=writeSpec(dist,info);
  fs.writeFileSync(path.join(dist,'llms.txt'),`# Chatbook\n\nRelease: ${info.release.id}\nAuthoring digest: ${spec.contentDigest}\n\n- [CURRENT authoring rules and representative prose](/engine-spec.json): Read this one response before a new book. Do not merge historical rules. Save its digest and snapshot for resume.\n- [Readable current rules](/engine-spec.html)\n- [Release and actual verification scope](/engine-info.json)\n- [Build identity](/version.json)\n- [Maintenance source](${info.maintenance.sourceGuide})\n\nIf current rules cannot be read, disclose it; do not fabricate a latest version. Publication requires authorized source/deployment access. No metadata is required to read existing books.\n`);
  const index=path.join(dist,'index.html');let html=fs.readFileSync(index,'utf8');
  // Remove only this generator's old tags; preserve unrelated head/footer content.
  html=html.replace(/<meta name="chatbook-release"[^>]*>/g,'').replace(/<link rel="alternate"[^>]*href="\/engine-(?:info|spec)\.json"[^>]*>/g,'').replace(/\s*<a href="\/engine-(?:info|spec)\.html">(?:현재 엔진 정보 및 확인 범위|현재 교재 제작 기준·대표 예시)<\/a>/g,'');
  html=html.replace('</head>',`<meta name="chatbook-release" content="${escape(info.release.id)}"><link rel="alternate" type="application/json" title="챗북 엔진 정보" href="/engine-info.json"><link rel="alternate" type="application/json" title="챗북 현재 교재 제작 기준" href="/engine-spec.json"></head>`);
  html=html.replace('</noscript>',' <a href="/engine-spec.html">현재 교재 제작 기준·대표 예시</a> <a href="/engine-info.html">현재 엔진 정보 및 확인 범위</a></noscript>');fs.writeFileSync(index,html);
  const headers=path.join(dist,'_headers');let old=fs.existsSync(headers)?fs.readFileSync(headers,'utf8'):'';
  old=old.replace(/\n?# BEGIN CHATBOOK METADATA\n[\s\S]*?# END CHATBOOK METADATA\n?/g,'');
  // Migrate the former append-only generator without touching unrelated header blocks.
  for(const p of INFO_PATHS){const literal=p.replace(/[.*+?^${}()|[\]\\]/g,'\\$&');old=old.replace(new RegExp('^'+literal+'\\n  Cache-Control: no-cache, no-store, must-revalidate\\n  X-Content-Type-Options: nosniff\\n?','gm'),'');}
  old=old.trimEnd();
  fs.writeFileSync(headers,(old?old+'\n\n':'')+'# BEGIN CHATBOOK METADATA\n'+INFO_PATHS.map(p=>`${p}\n  Cache-Control: no-cache, no-store, must-revalidate\n  X-Content-Type-Options: nosniff\n`).join('\n')+'# END CHATBOOK METADATA\n');
}
export async function verifyRelease(dist){
  const version=read(path.join(dist,'version.json')),info=read(path.join(dist,'engine-info.json'));
  need(info.schemaVersion===1,'schema unsupported');equal(version.releaseId,releaseId(version),'version identity');equal(info.release.id,version.releaseId,'app/info identity');equal(info.release.sourceDigest,version.sourceDigest,'app/info digest');equal(info.app.version,version.version,'app/info version');equal(info.engine.version,version.engineVersion,'app/info engine version');equal(info.release.sourceSha,version.sourceSha,'source SHA');equal(info.generatedAt,version.builtAt,'build timestamp');
  equal(info.engine,await engineState(dist),'published engine');equal(info.assetSha256,assets(dist),'published files');equal(info.verification.buildTests.sourceDigest,version.sourceDigest,'test evidence digest');need(info.verification.buildTests.status==='passed'&&info.verification.buildTests.exitCode===0,'build tests did not pass');
  need(fs.readFileSync(path.join(dist,'index.html'),'utf8').includes(`name="chatbook-release" content="${version.releaseId}"`),'reader identity missing');need(fs.readFileSync(path.join(dist,'engine-info.html'),'utf8').includes(`data-release-id="${version.releaseId}"`),'human-readable identity mismatch');need(fs.readFileSync(path.join(dist,'llms.txt'),'utf8').includes(version.releaseId),'discovery identity mismatch');
  const specDigest=verifySpec(dist,info);
  return {status:'passed',releaseId:version.releaseId,assetCount:Object.keys(info.assetSha256).length,specDigest};
}
export const serviceWorker=(digest,urls)=>worker(digest,urls,INFO_PATHS);
