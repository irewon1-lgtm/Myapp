/** Build-only authoring handoff. One current source, no runtime fetch or model call. */
import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
export const SPEC_PATHS=['/engine-spec.json','/engine-spec.html','/engine-spec','/engine-spec/'];
export const canonical=value=>JSON.stringify(value,(_,v)=>v&&typeof v==='object'&&!Array.isArray(v)?Object.fromEntries(Object.entries(v).sort(([a],[b])=>a.localeCompare(b))):v);
export const fingerprint=value=>createHash('sha256').update(canonical(value)).digest('hex');
const requireValue=(ok,message)=>{if(!ok)throw Error('Authoring specification: '+message);};
export const escape=value=>String(value).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
export function authoringContent(engine,document){
  const a=document.authoring;
  requireValue(a&&typeof a==='object','current authoring source missing');
  requireValue(typeof a.purpose==='string'&&a.purpose.trim(),'purpose missing');
  requireValue(Array.isArray(a.rules)&&a.rules.length&&a.rules.every(x=>typeof x==='string'&&x.trim()),'rules invalid');
  requireValue(new Set(a.rules).size===a.rules.length,'duplicate rules');
  requireValue(a.example&&typeof a.example.title==='string'&&Array.isArray(a.example.blocks)&&a.example.blocks.length,'example missing');
  requireValue(a.example.blocks.every(b=>['paragraph','code','output'].includes(b.type)&&typeof b.text==='string'&&b.text.trim()),'example block invalid');
  // Explicit allowlist: no revision history, draft examples, private notes or arbitrary fields.
  return {engineVersion:engine.version,profile:engine.profile,policy:engine.policy,purpose:a.purpose,rules:[...a.rules],
    example:{title:a.example.title,provenance:a.example.provenance||'Editorial example; not a verbatim video transcript.',blocks:a.example.blocks.map(({type,text})=>({type,text}))}};
}
export function createSpec(engine,document,release){
  const content=authoringContent(engine,document);
  return {schemaVersion:1,kind:'current-chatbook-authoring-spec',contentDigest:fingerprint(content),
    release:{id:release.id,sourceDigest:release.sourceDigest},...content,
    use:{start:'작업 시작 시 이 현재본을 실제로 읽고 engineVersion·contentDigest·규격 사본을 작업 기록에 저장한다. 과거 규격을 합쳐 쓰지 않는다.',
      resume:'진행 중인 책은 저장한 규격 사본으로 이어간다. 공통 기준 변경은 다음 새 책부터 적용하고, 사용자가 요청할 때만 진행 중인 책을 변경한다.',
      override:'이번 책만의 요청은 해당 책에만 적용한다. 공통 기준을 바꾸라는 요청이 있을 때만 원본 정책을 수정한다.',
      unavailable:'현재본을 읽지 못하면 최신 기준 확인 불가라고 알린다. 임의 규격을 최신판이라고 주장하지 않는다. 기존 책과 초안을 삭제하지 않는다.',
      maintenance:'public/engine/learning-policy.json의 authoring만 차이 편집한다. 폐기 규칙·옛 예시는 현재본에서 제거하고 Git 이력은 기본 제작 입력에 넣지 않는다. 문체 변경 시 예시도 함께 검토한다.',
      registration:'교재 작성과 앱 등록은 별개다. 등록할 때만 승인된 저장소에서 현재 데이터 계약을 확인하고 기존 책 ID·메모를 보존한다. 공개 규격은 수정·배포 권한을 주지 않는다.'},
    references:{releaseInformation:'/engine-info.json',sourceGuide:'https://github.com/irewon1-lgtm/Myapp/blob/chatbook-app-20260923/AGENTS.md',registrationContract:'/engine/learning.mjs'},
    limitations:['구조 검사와 해시는 번역·문장 품질·영상 접근 성공이나 AI의 규격 준수를 보증하지 않는다.','영상 분석은 구간별로 처리하지만 최종 본문은 장·절 단위로 연결한다. 이 공개 문서는 영상 생성 API가 아니다.']};
}
export function specHtml(s){
  const blocks=s.example.blocks.map(b=>b.type==='paragraph'?`<p>${escape(b.text)}</p>`:`<pre${b.type==='output'?' aria-label="실행 결과"':''}><code>${escape(b.text)}</code></pre>`).join('\n');
  return `<!doctype html><html lang="ko"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>챗북 현재 교재 제작 기준</title><link rel="alternate" type="application/json" href="/engine-spec.json"><style>body{font-family:system-ui,sans-serif;max-width:800px;margin:32px auto;padding:0 20px;line-height:1.85;overflow-wrap:anywhere}pre{white-space:pre-wrap;border:1px solid #bbb;padding:16px;border-radius:8px}a{color:inherit}small{display:block}li{margin-bottom:12px}@media(prefers-color-scheme:dark){body{background:#17171a;color:#eee}}</style></head><body data-spec-digest="${s.contentDigest}" data-release-id="${escape(s.release.id)}"><nav><a href="/">챗북</a> · <a href="/engine-spec.json">AI용 현재 기준</a> · <a href="/engine-info.html">배포·검사 정보</a></nav><h1>현재 교재 제작 기준</h1><p>엔진 ${escape(s.engineVersion)} · 기준 ${s.contentDigest.slice(0,12)}</p><p>${escape(s.purpose)}</p><p>다른 방에서는 이 현재본을 읽고 아래 규칙과 예시를 함께 적용합니다. 이력·옛 예시는 여기에 누적하지 않습니다.</p><h2>집필 기준</h2><ol>${s.rules.map(x=>`<li>${escape(x)}</li>`).join('')}</ol><h2>${escape(s.example.title)}</h2><small>${escape(s.example.provenance)}</small>${blocks}<h2>사용과 수정</h2>${Object.values(s.use).map(x=>`<p>${escape(x)}</p>`).join('')}<h2>확인 범위</h2>${s.limitations.map(x=>`<p>${escape(x)}</p>`).join('')}<small>배포 ${escape(s.release.id)} · 소스 지문 ${escape(s.release.sourceDigest)}</small></body></html>`;
}
export function writeSpec(dist,info){
  const document=JSON.parse(fs.readFileSync(path.join(dist,'engine/learning-policy.json'),'utf8'));
  const spec=createSpec(info.engine,document,info.release);
  fs.writeFileSync(path.join(dist,'engine-spec.json'),JSON.stringify(spec)+'\n');
  fs.writeFileSync(path.join(dist,'engine-spec.html'),specHtml(spec));
  return spec;
}
export function verifySpec(dist,info){
  const actual=JSON.parse(fs.readFileSync(path.join(dist,'engine-spec.json'),'utf8'));
  const expected=createSpec(info.engine,JSON.parse(fs.readFileSync(path.join(dist,'engine/learning-policy.json'),'utf8')),info.release);
  requireValue(canonical(actual)===canonical(expected),'current source/spec mismatch');
  requireValue(fs.readFileSync(path.join(dist,'engine-spec.html'),'utf8')===specHtml(expected),'HTML/spec mismatch');
  return actual.contentDigest;
}
