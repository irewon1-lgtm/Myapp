/* TRACK3_CONTINUOUS_BOOK_V1: full chapters, not reconstructed lesson cards. */
(function installContinuousBook(){
  const original={locate,lessonSection,reader,layoutReader,saveReaderPosition,openPage,saved,pageText,summaryText,doAction,practicePageSource,practiceResultMarkup,practice,runPythonPractice,trackPage,progressForTrack,importProgress:window.importProgress};
  const bookPage=()=>chapters[current.c]?.pages[current.p];
  const isBook=()=>!!bookPage()?.book;
  const objectField=k=>{if(!state[k]||typeof state[k]!=='object'||Array.isArray(state[k]))state[k]={}};
  for(const k of ['bookMarks','bookPositions','bookAnswers','bookPractice','bookProgress'])objectField(k);
  const rendered=new Map();
  let pendingSpot=null,pendingAnchor=null,activeSample=null,noteKey='',answerKey='';
  let worker=null,workerReady=false,queuedCode=null,runSerial=0,runTimer=null,loadTimer=null,runFinished=false;

  locate=function(id){
    const direct=original.locate(id);if(direct)return direct;
    const target=state.bookMarks[id]?.pageId||COURSE.bookAliases?.[id]||String(id||'').split('@')[0];
    return target!==id?original.locate(target):null;
  };
  pageText=function(page){return page.book?page.book.text:original.pageText(page)};
  summaryText=function(page){return page.book?(page.paragraphs?.[0]||page.title):original.summaryText(page)};

  function sampleById(id){
    for(const c of chapters)for(const p of c.pages)if(p.book){const s=p.book.samples.find(x=>x.id===id);if(s)return {c,p,s}}
    return null;
  }
  function sampleMarkup(sample){
    const label=sample.fragment?'부분 코드 · 실행 전 보완 필요':sample.expectedError?'예외를 관찰하는 예제':sample.capstoneTests?'종합 검사 · 함수 정의가 함께 실행됩니다':'Python 예제';
    const actions='<button data-action="copy-code" data-code="'+encodeURIComponent(sample.code)+'">복사</button>'+(sample.runCode?'<button data-action="book-run" data-sample="'+sample.id+'">실습에서 실행</button>':'');
    const code=sample.code.split('\n').map((line,i)=>'<span class="book-code-line" id="'+sample.anchor+'-line'+i+'" data-book-anchor="'+sample.anchor+'-line'+i+'" data-code-line="'+i+'">'+highlightPythonLine(line)+'</span>').join('');
    return '<div class="book-code" id="'+sample.anchor+'" data-book-anchor="'+sample.anchor+'"><div class="book-code-head"><span>'+esc(label)+'</span><div class="book-code-actions">'+actions+'</div></div><pre class="book-pre"><code>'+code+'</code></pre></div>';
  }
  function bookHTML(page){
    const key=page.id+':'+page.book.sourceSha256;
    if(rendered.has(key))return rendered.get(key);
    let value=page.book.html;
    for(const sample of page.book.samples)value=value.replace('<!--BOOK_CODE:'+sample.id+'-->',sampleMarkup(sample));
    rendered.set(key,value);return value;
  }
  lessonSection=function(page,c,index){
    if(!page.book)return original.lessonSection(page,c,index);
    return '<section class="lesson-section book-chapter" data-logical-page-id="'+page.id+'">'+bookHTML(page)+'<div class="book-end"><button data-action="complete">'+(state.complete.includes(c.id)?'완료 표시 취소':'이 장 완료 표시')+'</button><button data-action="track-current">9장 목차로</button></div></section>';
  };
  reader=function(){
    let value=original.reader();if(!isBook())return value;
    value=value.replace('class="reader-shell ','class="reader-shell book-mode ');
    value=value.replace('<button class="reader-aa"','<button data-action="book-note" aria-label="현재 위치에 메모">✎</button><button class="reader-aa"');
    value=value.replace('<span>요약</span>','<span>이 장 목차</span>');
    return value;
  };
  trackPage=function(){
    let value=original.trackPage();if(selectedTrack!==3)return value;
    value=value.replace(/1개 학습 페이지/g,'설명·예제·연습·풀이가 이어지는 본문');
    value=value.replace(/<small>\d+ \/ \d+ 페이지 학습<\/small>/,'<small>9장 · 마지막으로 읽은 위치 기준</small>');
    return value;
  };
  progressForTrack=function(id){
    if(id!==3)return original.progressForTrack(id);
    const books=chapters.filter(c=>c.track===3);
    if(!books.length)return 0;
    return Math.round(100*books.reduce((n,c)=>n+(state.complete.includes(c.id)?1:Math.min(1,Number(state.bookProgress[c.pages[0].id])||0)),0)/books.length);
  };

  function spot(){
    const p=bookPage(),paper=document.getElementById('paper'),flow=document.getElementById('flow');
    if(!p?.book)return null;
    let anchor=p.book.blocks[0]?.id||'',best=Infinity;
    if(paper?.getBoundingClientRect&&flow?.querySelectorAll&&paper.clientWidth){
      const pr=paper.getBoundingClientRect();
      for(const el of flow.querySelectorAll('[data-book-anchor]')){
        for(const rect of el.getClientRects()){
          if(rect.right<=pr.left+1||rect.left>=pr.right-1||rect.bottom<=pr.top||rect.top>=pr.bottom)continue;
          const score=Math.max(0,rect.top-pr.top);
          if(score<best||(score===best&&el.dataset.codeLine!==undefined)){best=score;anchor=el.dataset.bookAnchor}
        }
      }
    }
    const rootAnchor=anchor.replace(/-line\d+$/,'');
    const block=p.book.blocks.find(b=>b.id===rootAnchor);
    return {pageId:p.id,anchor,physical:physicalPage,total:physicalPages,ratio:physicalPages>1?physicalPage/(physicalPages-1):0,width:paper?.clientWidth||0,height:paper?.clientHeight||0,font:state.readerFont,source:p.book.sourceSha256,title:block?.section||p.title};
  }
  function physicalAt(anchor){
    const paper=document.getElementById('paper'),flow=document.getElementById('flow');
    if(!paper?.clientWidth||!flow?.querySelector)return 0;
    const el=flow.querySelector('[data-book-anchor="'+String(anchor||'').replace(/[^a-zA-Z0-9-]/g,'')+'"]');
    if(!el?.getClientRects)return null;
    const pr=paper.getBoundingClientRect();
    const rect=Array.from(el.getClientRects())[0];
    return rect?Math.max(0,Math.floor((rect.left-pr.left+paper.scrollLeft)/paper.clientWidth+.001)):null;
  }
  function restoreSpot(saved){
    const paper=document.getElementById('paper');
    if(saved.source===bookPage().book.sourceSha256&&saved.width===paper.clientWidth&&saved.height===paper.clientHeight&&saved.font===state.readerFont&&saved.total===physicalPages)return saved.physical;
    const anchorPage=physicalAt(saved.anchor);
    return anchorPage===null?Math.round((saved.ratio||0)*Math.max(0,physicalPages-1)):anchorPage;
  }
  saveReaderPosition=function(){
    if(!isBook()||route!=='reader')return original.saveReaderPosition();
    const p=bookPage(),saved=spot();if(!saved)return;
    state.bookPositions[p.id]=saved;
    state.readerPositions[chapters[current.c].id]={logicalId:p.id,physical:physicalPage,total:physicalPages,ratio:saved.ratio};
    state.bookProgress[p.id]=Math.max(Number(state.bookProgress[p.id])||0,(physicalPage+1)/Math.max(1,physicalPages));
    const key=p.id+'@'+saved.anchor;
    const star=document.querySelector('[data-action="bookmark"]');if(star)star.textContent=state.bookmarks.includes(key)?'★':'☆';
    persist();
  };
  openPage=function(id,last,resumeExact){
    const pos=locate(id),p=pos?chapters[pos.c].pages[pos.p]:null;
    if(!p?.book){pendingSpot=null;return original.openPage(id,last,resumeExact)}
    pendingSpot=state.bookMarks[id]||(resumeExact?state.bookPositions[p.id]:null)||null;
    pendingAnchor=null;
    if(COURSE.legacyTrack3?.[id])toast('이전 판 기록은 보존했습니다. 개정된 장의 처음을 엽니다.');
    original.openPage(p.id,last,false);
  };

  function buildBookSlideMap(width,height){
    const key=['book-v1',COURSE.version,width,height,state.readerFont].join(':');
    if(trackSlideMap.ready&&trackSlideMap.key===key)return;
    const host=document.createElement('div');
    host.className='reader-shell book-mode '+(state.readerDark?'dark':'light');
    host.style.cssText='position:fixed;left:-30000px;top:0;visibility:hidden;pointer-events:none;display:block;width:'+width+'px;height:'+height+'px;--readerBody:'+state.readerFont+'px';
    document.body.appendChild(host);
    let total=0;const entries=[],byId={};
    for(const c of chapters.filter(x=>x.track===3)){
      const p=c.pages[0];
      host.innerHTML='<div class="reader-flow">'+lessonSection(p,c,0).replace(/ id="[^"]*"/g,'')+'</div>';
      const flow=host.firstElementChild;
      flow.style.cssText='width:'+width+'px;height:'+height+'px;column-width:'+width+'px;column-gap:0;column-fill:auto;column-count:auto';
      const count=Math.max(1,Math.ceil(flow.scrollWidth/width-.001));
      const entry={id:p.id,start:total,count};entries.push(entry);byId[p.id]=entry;total+=count;
    }
    host.remove();trackSlideMap={track:3,key,ready:true,entries,byId,total};
  }
  layoutReader=function(){
    if(!isBook())return original.layoutReader();
    const paper=document.getElementById('paper'),flow=document.getElementById('flow');
    if(!paper?.clientWidth||!paper.clientHeight||!flow)return;
    const width=paper.clientWidth,height=paper.clientHeight;
    const lastKnown=state.bookPositions[bookPage().id];
    flow.style.width=width+'px';flow.style.height=height+'px';flow.style.columnWidth=width+'px';flow.style.columnCount='auto';flow.style.columnGap='0px';flow.style.columnFill='auto';paper.scrollLeft=0;
    const finish=()=>{
      physicalPages=Math.max(1,Math.ceil(flow.scrollWidth/width-.001));
      let target=0;
      if(pendingAnchor)target=physicalAt(pendingAnchor)||0;
      else if(pendingPhysicalPage!==null)target=Number(pendingPhysicalPage)||0;
      else if(pendingSpot)target=restoreSpot(pendingSpot);
      else if(pendingLast)target=physicalPages-1;
      else if(pendingPhysicalRatio!==null&&lastKnown)target=restoreSpot(lastKnown);
      else if(pendingPhysicalRatio!==null)target=Math.round(pendingPhysicalRatio*(physicalPages-1));
      else if(!pendingTargetId&&lastKnown)target=restoreSpot(lastKnown);
      physicalPage=Math.max(0,Math.min(physicalPages-1,target));
      pendingAnchor=null;pendingSpot=null;pendingPhysicalPage=null;pendingPhysicalRatio=null;pendingSavedPosition=null;pendingTargetId='';pendingLast=false;
      paper.scrollLeft=physicalPage*width;
      buildBookSlideMap(width,height);reconcileCurrentTrackSlideCount();
      state.location=bookPage().id;
      saveReaderPosition();updateHud();finishPageTurn();
      try{
        const footer=document.querySelector('.reader-bottom').getBoundingClientRect();
        Object.assign(document.body.dataset,{qaReady:'1',qaBook:'continuous-v1',qaPages:String(physicalPages),qaFont:getComputedStyle(flow).fontSize,qaOverflowY:getComputedStyle(paper).overflowY,qaFit:flow.scrollHeight<=height+2?'1':'0',qaFooterVisible:footer.bottom<=innerHeight+1?'1':'0',qaTitleTop:'1',qaTrackSlideCurrent:String(currentTrackSlidePosition().current),qaTrackSlideTotal:String(trackSlideMap.total)});
      }catch(e){}
    };
    if(typeof requestAnimationFrame==='function')requestAnimationFrame(finish);else finish();
  };

  function recordCard(id,note){
    const legacy=COURSE.legacyTrack3?.[id],mark=state.bookMarks[id],loc=locate(id);
    if(!legacy&&!loc)return '<div class="saved-card"><span class="saved-copy"><small>위치 정보가 없는 이전 기록</small><p>'+txt(note||id)+'</p></span></div>';
    const p=loc?chapters[loc.c].pages[loc.p]:null;
    const label=legacy?'이전 판 · '+legacy.chapter+'장 · 기록 보존':mark?'개정 교재 · '+(mark.physical+1)+'쪽 저장 위치':p?.book?'개정 교재':('Chapter '+chapters[loc.c].chapter);
    const action=legacy?'book-legacy':mark?'book-open-mark':'open';
    const title=legacy?.title||mark?.title||p?.title||id;
    return '<button class="saved-card" data-action="'+action+'" data-id="'+esc(id)+'"><span class="saved-mark">'+(note?'✎':'▮')+'</span><span class="saved-copy"><small>'+esc(label)+'</small><strong>'+esc(title)+'</strong><p>'+esc(note|| (legacy?'이전 본문과 메모를 그대로 확인합니다.':mark?'저장한 본문 위치를 엽니다.':summaryText(p)))+'</p></span></button>';
  }
  saved=function(){
    const marks=state.bookmarks.map(id=>recordCard(id,'')).join('');
    const notes=Object.entries(state.notes).filter(([,v])=>String(v).trim()).map(([id,n])=>recordCard(id,String(n))).join('');
    const answers=Object.entries(state.bookAnswers).filter(([,v])=>String(v).trim()).map(([id,n])=>'<button class="saved-card" data-action="book-answer" data-exercise="'+esc(id)+'" data-title="'+esc(id.replace('book-exercise-','연습 '))+'"><span class="saved-mark">✎</span><span class="saved-copy"><small>연습문제에 적은 내 답</small><p>'+esc(n)+'</p></span></button>').join('');
    const cards=(savedFilter==='bookmark'?marks:savedFilter==='note'?notes+answers:marks+notes+answers)||'<p class="empty">저장한 내용이 없습니다.</p>';
    return '<div class="screen"><main class="shell"><div class="top-row"><button class="back-btn" data-action="home">←</button><h1>북마크와 메모</h1></div><div class="chips">'+['all','bookmark','note'].map((x,i)=>'<button class="chip '+(savedFilter===x?'active':'')+'" data-action="saved-filter" data-filter="'+x+'">'+['전체','북마크','메모·내 답'][i]+'</button>').join('')+'</div><div class="saved-list">'+cards+'</div></main>'+bottomNav()+'</div>';
  };
  function showToc(){
    const p=bookPage();
    modal('이 장의 목차','<div class="book-toc">'+p.book.sections.map(s=>'<button data-action="book-jump" data-anchor="'+s.id+'">'+esc(s.title)+'</button>').join('')+'</div>');
  }
  function nearestSample(){
    const p=bookPage();if(!p?.book)return null;
    const here=spot()?.anchor||'';
    const usable=p.book.samples.filter(s=>s.runCode);
    return usable.filter(s=>s.anchor<=here).at(-1)||usable[0]||null;
  }
  function openPractice(sampleId){
    const found=sampleById(sampleId);if(!found||!found.s.runCode)return;
    saveReaderPosition();activeSample=found.s;practiceOutput='';practiceError='';runFinished=false;readerMenuOpen=false;route='practice';render();
  }
  practicePageSource=function(){
    if(!isBook())return original.practicePageSource();
    activeSample=activeSample&&bookPage().book.samples.some(s=>s.id===activeSample.id)?activeSample:nearestSample();
    return activeSample?(state.bookPractice[activeSample.id]??activeSample.runCode):'';
  };
  practiceResultMarkup=function(){
    if(!isBook())return original.practiceResultMarkup();
    if(practiceRunning)return '<div class="practice-loading">Python 실행 중</div><button class="book-practice-stop" data-action="book-stop">실행 중지</button>';
    const result=practiceError?'<div class="practice-console error"><div class="console-head">실제 오류</div><pre>'+esc(practiceError)+'</pre></div>':runFinished?'<div class="practice-console"><div class="console-head">실제 표준 출력</div><pre>'+esc(practiceOutput||'(출력 없이 정상 종료)')+'</pre></div>':'<p>위 코드를 실행하면 실제 결과가 표시됩니다.</p>';
    const expected=activeSample?.expectedOutput;
    return result+(expected!==null&&expected!==undefined?'<div class="book-practice-explanation"><strong>교재 원문 코드의 예상 출력</strong><pre>'+esc(expected)+'</pre><p>코드를 수정했다면 결과도 달라질 수 있습니다. 풀이 설명은 읽던 본문으로 돌아가 이어 읽습니다.</p></div>':'');
  };
  practice=function(){
    let value=original.practice();if(!isBook())return value;
    return value.replace('예제를 수정하고 실행하면서 Python이 실제로 어떻게 동작하는지 확인하세요.','현재 교재에서 선택한 실제 예제입니다. 실행은 별도 작업 공간에서 이루어지며 중지할 수 있습니다.').replace('<h1>코드를 직접 실행해보세요</h1>','<h1>'+esc(activeSample?.title||'교재 실습')+'</h1>');
  };
  function showRunResult(){const box=document.getElementById('practice-result');if(box)box.innerHTML=practiceResultMarkup()}
  function stopRun(message){
    clearTimeout(runTimer);clearTimeout(loadTimer);
    if(worker)worker.terminate();worker=null;workerReady=false;queuedCode=null;practiceRunning=false;
    if(message)practiceError=message;showRunResult();
  }
  runPythonPractice=async function(){
    if(!isBook())return original.runPythonPractice();
    const area=document.getElementById('practice-code');if(!area||!activeSample||practiceRunning)return;
    const code=area.value;state.bookPractice[activeSample.id]=code;persist();
    practiceRunning=true;practiceOutput='';practiceError='';runFinished=false;runSerial++;
    queuedCode={type:'run',id:runSerial,code};showRunResult();
    try{
      if(!worker){
        const workerSource=`let py;async function start(){try{importScripts('https://cdn.jsdelivr.net/pyodide/v0.27.7/full/pyodide.js');py=await loadPyodide({indexURL:'https://cdn.jsdelivr.net/pyodide/v0.27.7/full/'});postMessage({type:'ready'});}catch(e){postMessage({type:'boot-error',text:String(e)});}}onmessage=async e=>{const m=e.data;if(m.type!=='run'||!py)return;postMessage({type:'running',id:m.id});py.setStdout({batched:text=>postMessage({type:'stdout',id:m.id,text})});py.setStderr({batched:text=>postMessage({type:'stderr',id:m.id,text})});try{await py.runPythonAsync('exec(compile('+JSON.stringify(m.code)+', "<교재 실습>", "exec"), {"__name__": "__main__"})');postMessage({type:'done',id:m.id});}catch(e){postMessage({type:'error',id:m.id,text:String(e)});}};start();`;
        const url=URL.createObjectURL(new Blob([workerSource],{type:'text/javascript'}));
        worker=new Worker(url);URL.revokeObjectURL(url);
        loadTimer=setTimeout(()=>stopRun('Python 엔진을 불러오지 못했습니다. 인터넷 연결을 확인한 뒤 다시 실행하세요.'),90000);
        worker.onmessage=e=>{
          const m=e.data;
          if(m.type==='ready'){clearTimeout(loadTimer);workerReady=true;if(queuedCode){worker.postMessage(queuedCode);queuedCode=null}return}
          if(m.type==='boot-error'){stopRun('Python 엔진 로드 실패: '+m.text);return}
          if(m.id!==runSerial)return;
          if(m.type==='running'){runTimer=setTimeout(()=>stopRun('실행을 중지했습니다. 반복문이 끝나는지 확인하세요.'),8000);return}
          if(m.type==='stdout'){practiceOutput+=(practiceOutput?'\n':'')+m.text;return}
          if(m.type==='stderr'){practiceError+=(practiceError?'\n':'')+m.text;return}
          if(m.type==='error')practiceError=m.text;
          if(m.type==='error'||m.type==='done'){clearTimeout(runTimer);practiceRunning=false;runFinished=true;showRunResult()}
        };
        worker.onerror=e=>stopRun('Python 실행 환경 오류: '+(e.message||'지원되지 않는 실행 환경'));
      }
      if(workerReady&&queuedCode){worker.postMessage(queuedCode);queuedCode=null}
    }catch(e){stopRun('Python 실행을 시작하지 못했습니다: '+String(e.message||e))}
  };

  doAction=function(action,el){
    if(action==='book-legacy'){
      const old=COURSE.legacyTrack3?.[el.dataset.id];if(!old)return;
      modal('이전 판 기록','<span class="book-record-label">개정 본문과 혼동되지 않도록 원래 기록을 보존했습니다.</span><h3>'+esc(old.title)+'</h3><div class="book-legacy-text">'+txt(old.text)+'</div><h3>저장된 메모</h3><p>'+txt(state.notes[el.dataset.id]||'(메모 없음)')+'</p><button class="small-btn" data-action="open" data-id="'+COURSE.bookAliases[el.dataset.id]+'">개정된 장 처음으로</button>');return;
    }
    if(action==='book-open-mark'){openPage(el.dataset.id);return}
    if(action==='book-answer'){
      answerKey=el.dataset.exercise;modal(el.dataset.title||'연습문제의 내 답','<p>답은 자동 저장됩니다. 본문을 이어 읽으면 풀이를 만날 수 있습니다.</p><textarea id="book-answer-input" rows="9">'+esc(state.bookAnswers[answerKey]||'')+'</textarea>');return;
    }
    if(action==='book-stop'){stopRun('사용자가 실행을 중지했습니다.');return}
    if(isBook()){
      if(practiceRunning&&['home','books','track-current','continue','settings','saved'].includes(action))stopRun('화면 이동으로 실행을 중지했습니다.');
      if(action==='book-toc'||action==='summary'){showToc();return}
      if(action==='book-jump'){saveReaderPosition();pendingAnchor=el.dataset.anchor;pendingTargetId=bookPage().id;route='reader';render();return}
      if(action==='book-run'){openPractice(el.dataset.sample);return}
      if(action==='practice'){const s=nearestSample();if(s)openPractice(s.id);else toast('이 위치에는 실행할 코드가 없습니다');return}
      if(action==='practice-reset'){if(activeSample)delete state.bookPractice[activeSample.id];persist();runFinished=false;practiceOutput='';practiceError='';render();return}
      if(action==='bookmark'||action==='book-note'||action==='note'){
        const saved=spot();if(!saved)return;
        const key=saved.pageId+'@'+saved.anchor;state.bookMarks[key]=saved;
        if(action==='bookmark'){
          state.bookmarks=state.bookmarks.includes(key)?state.bookmarks.filter(x=>x!==key):state.bookmarks.concat(key);
          persist();saveReaderPosition();toast(state.bookmarks.includes(key)?'현재 본문 위치를 저장했습니다':'북마크를 해제했습니다');return;
        }
        noteKey=key;modal('현재 본문에 메모','<p>'+esc(saved.title)+'</p><textarea id="book-note-input" rows="9">'+esc(state.notes[key]||'')+'</textarea>');persist();return;
      }
      if(action==='complete'){pendingSpot=spot();}
    }
    original.doAction(action,el);
  };
  window.bookInput=function(e){
    if(e.target.id==='book-note-input'){state.notes[noteKey]=e.target.value;persist();return true}
    if(e.target.id==='book-answer-input'){state.bookAnswers[answerKey]=e.target.value;persist();return true}
    if(e.target.id==='practice-code'&&isBook()&&activeSample){state.bookPractice[activeSample.id]=e.target.value;persist();return true}
    return false;
  };
  window.importProgress=function(raw){
    let incoming;try{incoming=JSON.parse(raw)}catch(e){return original.importProgress(raw)}
    original.importProgress(raw);
    if(incoming.app!=='codingroadmap-live-v1'||incoming.state?.schema!==1||!Array.isArray(incoming.state.bookmarks)||!incoming.state.notes)return;
    for(const k of ['bookMarks','bookPositions','bookAnswers','bookPractice','bookProgress']){
      const value=incoming.state[k];if(value&&typeof value==='object'&&!Array.isArray(value))state[k]=Object.assign({},value,state[k]);
    }
    persist();render();
  };
  const restored=locate(state.location);
  if(restored){
    current=restored;selectedTrack=chapters[current.c].track;
    if(COURSE.legacyTrack3?.[state.location]){
      state.previousTrack3Location=state.location;state.location=bookPage().id;pendingTargetId=state.location;persist();
    }
  }
  try{
    const params=new URLSearchParams(location.search),chapter=Number(params.get('book'));
    if(chapter>=1&&chapter<=9){const c=chapters.findIndex(x=>x.track===3&&x.chapter===chapter);if(c>=0){current={c,p:0};selectedTrack=3;state.location=bookPage().id;pendingTargetId=state.location;previewRoute='reader'}}
  }catch(e){}
  window.Track3Book={version:'continuous-v1',spot,physicalAt,bookHTML,sampleById,sourceCounts:()=>COURSE.track3BookAudit,stopRun};
})();
