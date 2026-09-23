import {VERSION,DEFAULTS,prepareSource,newJob,task,digest,bytes,publicationGate} from './learning.mjs?v=1.1.0';
const $=id=>document.getElementById(id);let job=null;
$('version').textContent=VERSION;document.body.dataset.engineReady='true';
try{const r=JSON.parse(localStorage.getItem('chatbook.records.v1')||'{}');if(r['pref:theme']?.v!=='dark'&&r['pref:theme']?.v!=='system')document.body.classList.add('light');}catch{}
$('theme').onclick=()=>document.body.classList.toggle('light');
const status=t=>$('message').textContent=t;
const fmt=n=>`${Math.floor(n/60)}:${String(Math.floor(n%60)).padStart(2,'0')}`;
function text(tag,content,className){const el=document.createElement(tag);el.textContent=content;if(className)el.className=className;return el;}
function display(){
 const r=$('result');r.hidden=false;r.replaceChildren();r.append(text('h3',job.source.title));
 const grid=document.createElement('div');grid.className='metric-grid';
 for(const[value,label]of[[fmt(job.source.durationSeconds),'원문 길이'],[job.source.chunks.length,'작업 구간'],[Object.keys(job.checkpoints).length,'완료 체크포인트']]){const d=document.createElement('div');d.className='metric';d.append(text('b',value),text('span',label));grid.append(d);}r.append(grid);
 r.append(text('p','집필 대기 · 원본이 보존된 준비 상태입니다. 완성된 전자책이 아닙니다.','muted'));
 for(const c of job.source.chunks){const d=text('div',`${c.id} · ${fmt(c.start)}–${fmt(c.end)}`,'chunk');d.append(text('span',`${c.sourceIds.length}개 원문 구간 · ${(c.textBytes/1024).toFixed(1)} KB`));r.append(d);}
 r.append(text('p',`원문 지문: ${job.source.rawHash.slice(0,18)}…`,'muted'));
 if(job.plan?.warnings?.length)r.append(text('p','긴 원문은 호출·재시도 한도를 먼저 점검합니다. 한도에 맞추려고 내용을 줄이지 않습니다.','muted'));
 if(job.source.warnings.length)r.append(text('p','자동자막과 시간대는 실제 음성·화면 대조가 필요합니다. 원문 ID 보존은 의미 검증을 대신하지 않습니다.','muted'));
 $('save').disabled=false;$('task').disabled=false;
 window.chatbookEngineJob=job;
}
async function prepare(input){status('원문 확인·구간 분할 중…');try{job=await newJob(await prepareSource(input));display();status('작업 준비 완료. 외부 AI 호출 0회 · 기존 서재 변경 없음.');}catch(e){status(`${e.code||'오류'} · ${e.message}`);}}
$('prepare').onclick=()=>prepare({videoId:$('video').value,transcript:$('transcript').value,durationSeconds:Number($('duration').value)||undefined});
$('source-file').onchange=async e=>{const f=e.target.files[0];if(f&&f.size<8*1024*1024)$('transcript').value=await f.text();else status('8 MB 이하의 전사 파일을 선택하세요.');};
async function existing(id){status('저장된 원문을 읽고 있습니다…');try{const u=`https://raw.githubusercontent.com/irewon1-lgtm/Myapp/chatbook-app-20260923/exact_source/${id}/transcript.md`;const r=await fetch(u,{cache:'no-store',signal:AbortSignal.timeout(12000)});if(!r.ok)throw Error(`원문 조회 HTTP ${r.status}`);await prepare({videoId:id,transcript:await r.text(),origin:'existing-chatbook-transcript'});}catch(e){status(e.message+' · 제목이나 추정 내용으로 대신 만들지 않았습니다.');}}
$('backend').onclick=()=>existing('0SGfDKMLdaI');$('frontend').onclick=()=>existing('Kl5EcYd3G2E');
function download(value,name){const url=URL.createObjectURL(new Blob([JSON.stringify(value,null,2)],{type:'application/json'}));const a=document.createElement('a');a.href=url;a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(url),30000);}
$('save').onclick=async()=>{if(job)download({hash:await digest(job),job},`chatbook-job-${job.id}.json`);};
$('job-file').onchange=async e=>{try{const f=e.target.files[0];if(!f)return;if(f.size>16*1024*1024)throw Error('작업 파일이 너무 큽니다.');const j=JSON.parse(await f.text());if(!j.job||j.hash!==await digest(j.job)||j.job.engineVersion!==VERSION)throw Error('작업 형식이나 해시가 맞지 않습니다.');job=j.job;display();status('원본과 완료 지점을 복원했습니다. 자동 AI 요청은 실행하지 않았습니다.');}catch(err){status(err.message);}};
$('task').onclick=()=>{try{for(const c of job.source.chunks)for(const s of['source','beginner','review'])if(!job.checkpoints[`${c.id}:${s}`]){download(task(job,c.id,s),`chatbook-${c.id}-${s}.json`);return;}status(publicationGate(job).ready?'검수 결과와 게시 승인을 확인해야 합니다.':'미해결 검토 항목이 있습니다.');}catch(e){status(e.message);}};
