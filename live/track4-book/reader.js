/* TRACK4_CONTINUOUS_WEB_V1 — source-faithful book plus isolated small web exercises. */
(function installTrack4Web(){
  const originalAction=doAction,originalPractice=practice;
  const page=()=>chapters[current.c]?.pages[current.p];
  const isTrack4=()=>chapters[current.c]?.track===4&&!!page()?.book;
  let active=null,frame=null,timer=null,serial=0,lines=[],lastError='',running=false,finished=false;
  const closingScript='<'+'/script>';
  const policy="<meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'; script-src 'unsafe-inline' 'unsafe-eval' blob:; worker-src blob:; style-src 'unsafe-inline'; img-src data:; connect-src 'none'; base-uri 'none'; form-action 'none'\">";
  function sampleMarkup(sample){
    const language={html:'HTML',css:'CSS',javascript:'JavaScript',json:'JSON'}[sample.language]||sample.language;
    const detail=sample.file?sample.file+' · 전체 파일':sample.mode==='html'?'완전한 HTML 예제':sample.mode==='console'?'JavaScript 콘솔 예제':'부분 코드 · 본문 문맥에 연결';
    const action=sample.mode!=='reference'?'<button data-action="web-sample" data-sample="'+sample.id+'">'+(sample.mode==='html'?'화면 실습':'실제 실행')+'</button>':'';
    const code=sample.code.split('\n').map((s,i)=>'<span class="book-code-line" id="'+sample.anchor+'-line'+i+'" data-book-anchor="'+sample.anchor+'-line'+i+'" data-code-line="'+i+'">'+esc(s)+'</span>').join('');
    return '<div class="book-code" id="'+sample.anchor+'" data-book-anchor="'+sample.anchor+'"><div class="book-code-head"><span>'+esc(language+' · '+detail)+'</span><div class="book-code-actions"><button data-action="copy-code" data-code="'+encodeURIComponent(sample.code)+'">복사</button>'+action+'</div></div><pre class="book-pre"><code>'+code+'</code></pre></div>';
  }
  function stop(text){
    clearTimeout(timer);timer=null;
    if(frame){try{frame.contentWindow.postMessage({type:'track4-stop'},'*')}catch(e){}frame.remove();frame=null;}
    running=false;
    if(text){lastError=text;finished=true;paint();}
  }
  function ensureSample(){
    if(!isTrack4())return null;
    const samples=page().book.samples;
    if(!active||!samples.some(s=>s.id===active.id))active=samples.find(s=>s.mode!=='reference')||samples[0]||null;
    return active;
  }
  function codeValue(){return active?(state.bookPractice[active.id]??active.code):'';}
  function openSample(id){
    const found=window.Track3Book.sampleById(id);if(!found||found.c.track!==4)return;
    saveReaderPosition();stop();active=found.s;lines=[];lastError='';finished=false;readerMenuOpen=false;route='practice';render();
  }
  function paint(){
    const output=document.getElementById('web-console');if(output)output.textContent=(lastError?lastError+'\n':'')+lines.join('\n')+(finished&&!lastError&&!lines.length&&active?.mode!=='html'?'(콘솔 출력 없이 종료)':'');
    const status=document.getElementById('web-run-status');if(status)status.textContent=running?'실행 중':lastError?'실행 오류 또는 중지':finished?(active?.mode==='html'?'예제 화면 준비 완료':'실제 실행 결과'):active?.mode==='html'?'실행하면 아래에 독립된 예제 화면이 표시됩니다.':'실행하면 실제 콘솔 출력이 표시됩니다.';
    const button=document.getElementById('web-execute');if(button)button.disabled=running||!active||active.mode==='reference';
    const stopButton=document.getElementById('web-stop');if(stopButton)stopButton.hidden=!running;
  }
  const workerCode=`
let sent=0;
function emit(kind,text){if(sent++>300){postMessage({kind:'error',text:'출력 제한을 넘겨 실행을 중지했습니다.'});close();return;}postMessage({kind,text:String(text).slice(0,32000)});}
function show(value){if(typeof value==='string')return value;if(value===undefined)return 'undefined';try{return JSON.stringify(value)}catch(e){return String(value)}}
console.log=(...args)=>emit('log',args.map(show).join(' '));
console.error=(...args)=>emit('log',args.map(show).join(' '));
console.warn=(...args)=>emit('log',args.map(show).join(' '));
onmessage=async function(e){try{const value=(0,eval)(e.data.code);if(value&&typeof value.then==='function')await value;await new Promise(r=>setTimeout(r,100));emit('done','');}catch(error){emit('error',error.name+': '+error.message);}};
`;
  function consoleDocument(code,token){
    const script=`
const token=${JSON.stringify(token)};
const send=(kind,text)=>parent.postMessage({source:'track4-web',token,kind,text},'*');
let worker;
try{
 const url=URL.createObjectURL(new Blob([${JSON.stringify(workerCode)}],{type:'text/javascript'}));
 worker=new Worker(url);URL.revokeObjectURL(url);
 worker.onmessage=e=>send(e.data.kind,e.data.text);
 worker.onerror=e=>{send('error',e.message||'JavaScript Worker 오류');e.preventDefault();};
 worker.postMessage({code:${JSON.stringify(code).replace(/</g,'\\u003c')}});
}catch(e){send('error',e.name+': '+e.message);}
addEventListener('message',e=>{if(e.source===parent&&e.data?.type==='track4-stop'&&worker)worker.terminate();});
`;
    return '<!doctype html><html><head>'+policy+'</head><body><script>'+script+closingScript+'</body></html>';
  }
  function htmlDocument(code,token){
    const script=`
const send=(kind,text)=>parent.postMessage({source:'track4-web',token:${JSON.stringify(token)},kind,text:String(text)},'*');
addEventListener('error',e=>{send('error',e.message);e.preventDefault();});
addEventListener('unhandledrejection',e=>{send('error',e.reason?.message||e.reason);e.preventDefault();});
console.log=(...x)=>send('log',x.map(v=>typeof v==='string'?v:JSON.stringify(v)).join(' '));
addEventListener('DOMContentLoaded',()=>requestAnimationFrame(()=>requestAnimationFrame(()=>send('html-ready',''))));
`;
    const head=policy+'<meta name="viewport" content="width=device-width, initial-scale=1"><script>'+script+closingScript;
    return /<head(?:\s[^>]*)?>/i.test(code)?code.replace(/<head(?:\s[^>]*)?>/i,match=>match+head):'<!doctype html><html><head>'+head+'</head><body>'+code+'</body></html>';
  }
  function run(){
    ensureSample();if(!active||active.mode==='reference')return;
    const area=document.getElementById('web-code-input');if(!area)return;
    const code=area.value;state.bookPractice[active.id]=code;persist();stop();
    lines=[];lastError='';finished=false;running=true;serial++;const token=String(serial);paint();
    frame=document.createElement('iframe');frame.title='기존 앱과 분리된 웹 예제 실행';frame.className='web-example-frame';
    frame.setAttribute('sandbox',active.mode==='html'?'allow-scripts allow-forms':'allow-scripts');
    frame.setAttribute('referrerpolicy','no-referrer');
    if(active.mode==='console')frame.classList.add('console-frame');
    const mount=document.getElementById('web-frame-mount');if(!mount){running=false;return}
    mount.replaceChildren(frame);
    frame.srcdoc=active.mode==='console'?consoleDocument(code,token):htmlDocument(code,token);
    timer=setTimeout(()=>stop(active.mode==='console'?'실행 제한 시간을 넘겨 중지했습니다. 무한 반복이나 끝나지 않는 작업을 확인하세요.':'예제 화면을 시작하지 못했습니다.'),active.mode==='console'?3000:5000);
  }
  function receive(event){
    if(!frame||event.source!==frame.contentWindow)return;
    const m=event.data;if(!m||m.source!=='track4-web'||m.token!==String(serial))return;
    if(m.kind==='log'){
      if(lines.join('\n').length>64000||lines.length>300){stop('출력 한도를 넘겨 중지했습니다.');return}
      lines.push(String(m.text).slice(0,32000));paint();return;
    }
    if(m.kind==='error'){lastError=String(m.text).slice(0,16000);running=false;finished=true;clearTimeout(timer);if(active?.mode==='console')stop();paint();return;}
    if(m.kind==='done'){finished=true;running=false;stop();paint();return;}
    if(m.kind==='html-ready'){clearTimeout(timer);const shown=frame;shown.scrollIntoView({block:'center',behavior:'instant'});requestAnimationFrame(()=>requestAnimationFrame(()=>{if(frame!==shown)return;running=false;finished=true;paint();}));}
  }
  if(typeof window.addEventListener==='function')window.addEventListener('message',receive);
  practice=function(){
    if(!isTrack4())return originalPractice();
    ensureSample();
    const p=page();const samples=p.book.samples;
    const choices=samples.map(s=>'<option value="'+s.id+'" '+(active?.id===s.id?'selected':'')+'>'+esc((s.file||s.language)+' · '+s.title)+'</option>').join('');
    const reference=active?.mode==='reference';
    return '<div class="screen web-practice"><main class="shell web-practice-shell"><div class="top-row"><button class="back-btn" data-action="continue">←</button><h1>웹 교재 실습</h1></div><p>'+esc(p.title)+'</p><label for="web-sample-select">이 장의 코드</label><select id="web-sample-select">'+choices+'</select>'+
    '<p class="web-scope">'+(reference?'이 코드는 본문 문맥에 연결하는 부분 코드 또는 여러 파일 프로젝트입니다. 단독 실행으로 오해하지 않도록 실행 버튼을 비활성화했습니다.':active?.mode==='console'?'실제 JavaScript를 별도 Worker에서 실행합니다. DOM·저장소·네트워크가 없는 짧은 계산 예제용입니다. 일반 함수의 비동기 예약은 짧은 관찰 구간만 포함됩니다.':'실제 HTML·JavaScript 예제를 앱과 분리된 화면에서 실행합니다. 외부 통신·기기 저장소·다운로드는 허용하지 않습니다. 전체 프로젝트 검증과는 다릅니다.')+'</p>'+
    '<label for="web-code-input">코드 수정</label><textarea id="web-code-input" spellcheck="false" rows="14">'+esc(codeValue())+'</textarea><div class="actions"><button id="web-execute" data-action="web-execute" '+(reference?'disabled':'')+'>'+(active?.mode==='html'?'화면 실행':'JavaScript 실행')+'</button><button data-action="web-reset">원문으로</button><button id="web-stop" data-action="web-stop" hidden>실행 중지</button></div><p id="web-run-status" role="status"></p><div id="web-frame-mount"></div><pre id="web-console" aria-live="polite"></pre>'+
    (active?.expectedOutput!==null&&active?.expectedOutput!==undefined?'<h2>원문 코드의 예상 출력</h2><pre class="web-expected">'+esc(active.expectedOutput)+'</pre><p>수정한 코드의 실제 결과와 구분해서 읽으세요.</p>':'')+
    '<button class="small-btn" data-action="summary">이 장의 본문 목차</button></main>'+bottomNav()+'</div>';
  };
  doAction=function(action,el){
    if(action==='web-sample'){openSample(el.dataset.sample);return}
    if(action==='web-execute'){run();return}
    if(action==='web-stop'){stop('사용자가 실행을 중지했습니다.');return}
    if(action==='web-reset'){stop();if(active)delete state.bookPractice[active.id];persist();lines=[];lastError='';finished=false;render();return}
    if(isTrack4()&&action==='practice'){ensureSample();if(active)openSample(active.id);else originalAction('summary',{});return}
    if(frame&&['home','books','track','track-current','continue','saved','settings','open','chapter','book-jump'].includes(action))stop();
    originalAction(action,el);
  };
  if(document.body&&typeof document.body.addEventListener==='function'){
    document.body.addEventListener('input',event=>{
      if(event.target.id==='web-code-input'&&active){state.bookPractice[active.id]=event.target.value;persist();event.stopPropagation();}
    });
    document.body.addEventListener('change',event=>{
      if(event.target.id==='web-sample-select'){const chosen=page()?.book?.samples.find(s=>s.id===event.target.value);if(chosen)openSample(chosen.id);event.stopPropagation();}
    });
  }
  try{
    const n=Number(new URLSearchParams(location.search).get('track4'));
    if(n>=1&&n<=12){const c=chapters.findIndex(x=>x.track===4&&x.chapter===n);if(c>=0){current={c,p:0};selectedTrack=4;state.location=page().id;pendingTargetId=state.location;previewRoute='reader'}}
  }catch(e){}
  window.Track4Web={sampleMarkup,run,stop,openSample,sourceCounts:()=>COURSE.track4BookAudit};
})();
