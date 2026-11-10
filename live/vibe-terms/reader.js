/* VIBE_BOOK_DARK_V2 — approved book mockup #4, 1 term = 1 vertical reading page.
 * Only Track 12 is replaced. Source text, page IDs and all other tracks are untouched.
 * No physical slide/page break inside a term. Native APK signing is unchanged.
 */
(function(){
  'use strict';
  const baseReader=reader;
  const baseDoAction=doAction;
  const baseRender=render;
  const basePageText=pageText;
  const baseSaveReaderPosition=saveReaderPosition;
  const TRACK=12;
  const termPages=chapters.filter(c=>c.track===TRACK).flatMap(c=>c.pages);
  let scrollWriteTimer=null;
  let entryDirection=0;
  let suppressEdgeTapUntil=0;

  function isVibe(){
    return route==='reader'&&chapters[current.c]&&chapters[current.c].track===TRACK;
  }
  function currentTerm(){
    return chapters[current.c].pages[current.p];
  }
  function termIndex(){
    return termPages.findIndex(p=>p.id===currentTerm().id);
  }
  function ensureScroll(){
    if(!state.vibeBookScroll||typeof state.vibeBookScroll!=='object'||Array.isArray(state.vibeBookScroll))
      state.vibeBookScroll={};
  }
  function saveScroll(){
    if(!isVibe())return;
    const viewport=document.getElementById('vibe-book-viewport');
    if(!viewport)return;
    ensureScroll();
    state.vibeBookScroll[currentTerm().id]=Math.round(viewport.scrollTop);
    persist();
  }
  function goTerm(delta){
    if(!isVibe())return;
    if(scrollWriteTimer!==null)clearTimeout(scrollWriteTimer);
    scrollWriteTimer=null;
    saveScroll();
    const index=termIndex();
    const target=termPages[index+delta];
    if(target){entryDirection=delta;openPage(target.id);}
  }

  /* VIBE_BOOK_SWIPE_V3 — horizontal page-turn gestures, vertical text scrolling.
   * Native touch events are used for Android WebView; mouse pointer events are
   * an optional desktop fallback. Do not intercept links, notes or copy buttons.
   */
  function installGestures(viewport){
    let gesture=null;
    let lastTouchEnd=0;
    const MOVE_RATIO=1.25;
    const blocked=target=>target&&target.closest&&
      target.closest('button,a,input,textarea,select,summary,details,[contenteditable="true"]');
    const page=()=>viewport.querySelector('.vibe-book-page');

    function reset(){
      const article=page();
      if(article){
        article.classList.remove('vibe-swipe-dragging');
        article.style.removeProperty('--vibe-drag-x');
      }
      gesture=null;
    }
    function start(x,y,source,target,time){
      if(gesture||blocked(target))return false;
      gesture={x,y,source,time,axis:null,lastX:x,lastY:y};
      return true;
    }
    function move(x,y,event){
      if(!gesture)return;
      const dx=x-gesture.x,dy=y-gesture.y;
      gesture.lastX=x;
      gesture.lastY=y;
      if(!gesture.axis){
        if(Math.max(Math.abs(dx),Math.abs(dy))<9)return;
        gesture.axis=Math.abs(dx)>Math.abs(dy)*MOVE_RATIO?'horizontal':
                     Math.abs(dy)>Math.abs(dx)*MOVE_RATIO?'vertical':null;
      }
      if(gesture.axis==='horizontal'){
        if(event&&event.cancelable)event.preventDefault();
        const article=page();
        if(article){
          article.classList.add('vibe-swipe-dragging');
          article.style.setProperty('--vibe-drag-x',
            Math.max(-62,Math.min(62,dx*0.42))+'px');
        }
      }
    }
    function finish(x,y,time){
      if(!gesture)return;
      const dx=x-gesture.x,dy=y-gesture.y;
      const axis=gesture.axis;
      const ms=Math.max(1,time-gesture.time);
      const width=Math.max(260,viewport.clientWidth||360);
      const threshold=Math.max(38,Math.min(78,width*0.12));
      const deliberate=axis==='horizontal'&&Math.abs(dx)>=threshold;
      const flick=axis==='horizontal'&&ms<=360&&Math.abs(dx)>=30;
      const horizontal=Math.abs(dx)>Math.abs(dy)*MOVE_RATIO;
      reset();
      // Native touch can emit a synthetic click after a drag; never turn two words.
      if(Math.max(Math.abs(dx),Math.abs(dy))>12)
        suppressEdgeTapUntil=Date.now()+600;
      if(horizontal&&(deliberate||flick))goTerm(dx<0?1:-1);
    }
    viewport.addEventListener('touchstart',event=>{
      if(!event.touches||event.touches.length!==1){reset();return;}
      const p=event.touches[0];
      start(p.clientX,p.clientY,'touch',event.target,Date.now());
    },{passive:true});
    viewport.addEventListener('touchmove',event=>{
      if(!gesture||gesture.source!=='touch')return;
      if(!event.touches||event.touches.length!==1){reset();return;}
      const p=event.touches[0];
      move(p.clientX,p.clientY,event);
    },{passive:false});
    viewport.addEventListener('touchend',event=>{
      if(!gesture||gesture.source!=='touch')return;
      const p=event.changedTouches&&event.changedTouches[0];
      if(p)finish(p.clientX,p.clientY,Date.now());
      else reset();
      lastTouchEnd=Date.now();
    },{passive:true});
    viewport.addEventListener('touchcancel',reset,{passive:true});

    viewport.addEventListener('pointerdown',event=>{
      if(event.pointerType!=='mouse'||event.button!==0||Date.now()-lastTouchEnd<500)return;
      if(start(event.clientX,event.clientY,'mouse',event.target,Date.now())
        &&viewport.setPointerCapture){
        try{viewport.setPointerCapture(event.pointerId)}catch(_){}
      }
    });
    viewport.addEventListener('pointermove',event=>{
      if(gesture&&gesture.source==='mouse'){
        if((event.buttons&1)===0){reset();return;}
        move(event.clientX,event.clientY,event);
      }
    });
    viewport.addEventListener('pointerup',event=>{
      if(gesture&&gesture.source==='mouse')finish(event.clientX,event.clientY,Date.now());
    });
    viewport.addEventListener('pointercancel',reset);
  }


  /* VIBE_EDGE_TAP_V4 — restore the original reader's left/right edge taps.
   * Use event delegation instead of overlays: text, vertical scroll and
   * links/buttons remain interactive. Only the reading viewport responds.
   */
  function installEdgeTaps(viewport){
    viewport.addEventListener('click',event=>{
      if(!isVibe()||event.defaultPrevented||Date.now()<suppressEdgeTapUntil)return;
      // The edge action is for an actual click/tap, not keyboard activation.
      if(event.detail===0 || (event.button!==undefined&&event.button!==0))return;
      const target=event.target;
      if(target&&target.closest&&target.closest(
        'button,a,input,textarea,select,summary,details,[contenteditable="true"]'
      ))return;
      const bounds=viewport.getBoundingClientRect();
      if(!bounds.width||!bounds.height)return;
      const x=event.clientX-bounds.left;
      const y=event.clientY-bounds.top;
      if(x<0||x>bounds.width||y<0||y>bounds.height)return;
      const edgeWidth=Math.min(56,Math.max(38,bounds.width*0.12));
      if(x<=edgeWidth)goTerm(-1);
      else if(x>=bounds.width-edgeWidth)goTerm(1);
    });
  }

  function section(number,label,body,classname){
    return '<section class="vibe-book-section '+classname+'" aria-label="'+esc(label)+'">'+
      '<h2><span class="vibe-book-numeral">'+number+'.</span> '+esc(label)+'</h2>'+
      '<p>'+txt(body)+'</p></section>';
  }

  reader=function(){
    if(chapters[current.c].track!==TRACK)return baseReader();
    const chapter=chapters[current.c],page=currentTerm(),term=page.vibe;
    if(!term||term.number<1||term.number>50)throw new Error('Track 12 vocabulary data missing');
    const index=termIndex();
    if(index<0)throw new Error('Track 12 term is not indexed');
    const previous=index>0?termPages[index-1]:null;
    const next=index<termPages.length-1?termPages[index+1]:null;
    const saved=state.bookmarks.includes(page.id);
    const font=Math.max(16,Math.min(22,Number(state.vibeBookFont)||18));
    return '<div class="reader-shell vibe-book-shell dark '+(native?'native-reader':'')+'" style="--vibe-book-font:'+font+'px" data-vibe-term="'+term.number+'">'+
      '<header class="vibe-book-top">'+
        '<button type="button" data-action="track-current" aria-label="트랙 목차로 돌아가기" class="vibe-book-back">‹</button>'+
        '<span class="vibe-book-app-name">바이브 코딩 필수 용어 200</span>'+
        '<button type="button" data-action="vibe-font" aria-label="글자 크기 변경" class="vibe-book-font">Aa</button>'+
        '<button type="button" data-action="bookmark" aria-label="북마크" aria-pressed="'+(saved?'true':'false')+'" class="vibe-book-bookmark">'+(saved?'★':'☆')+'</button>'+
      '</header>'+
      '<main id="vibe-book-viewport" class="vibe-book-viewport" tabindex="0" aria-label="'+esc(term.term)+' 한 페이지 본문">'+
        '<article class="vibe-book-page'+(entryDirection>0?' vibe-slide-in-next':entryDirection<0?' vibe-slide-in-previous':'')+'" data-page-id="'+esc(page.id)+'">'+
          '<div class="vibe-book-meta"><span>제'+chapter.chapter+'장 · '+esc(chapter.title)+'</span><span>용어 '+term.number+' / 200</span></div>'+
          '<div class="vibe-book-title-block"><h1>'+esc(term.term)+'</h1><p class="vibe-book-english">('+esc(term.english)+')</p></div>'+
          section(1,'쉬운 설명',term.explain,'vibe-book-meaning')+
          section(2,'실제 앱 개발 예시',term.example,'vibe-book-example')+
          section(3,'비슷한 용어와 차이',term.contrast,'vibe-book-contrast')+
          '<section class="vibe-book-section vibe-book-judgement" aria-label="AI 오류 판별 문제">'+
            '<h2><span class="vibe-book-numeral">4.</span> AI 오류 판단 문제</h2>'+
            '<p class="vibe-book-question"><b>Q.</b> '+txt(term.wrong)+'</p>'+
            '<p class="vibe-book-answer"><strong>정답·해설</strong> '+txt(term.answer)+'</p>'+
          '</section>'+
          '<section class="vibe-book-section vibe-book-codex" aria-label="Codex 지시문 예시">'+
            '<h2><span class="vibe-book-numeral">5.</span> Codex 지시문 예시</h2>'+
            '<p class="vibe-book-prompt">'+txt(term.prompt)+'</p>'+
            '<button type="button" class="vibe-book-copy" data-action="vibe-copy" data-code="'+encodeURIComponent(term.prompt)+'">▣ 지시문 복사</button>'+
          '</section>'+
          '<button type="button" class="vibe-book-note" data-action="note">✎ 이 용어에 메모</button>'+
          '<div class="vibe-book-tail" aria-hidden="true">· · ·</div>'+
        '</article>'+
      '</main>'+
      '<footer class="vibe-book-footer" aria-label="용어 넘기기">'+
        '<button type="button" data-action="vibe-prev" '+(previous?'':'disabled')+' aria-label="이전 용어">‹ <span>이전 용어</span></button>'+
        '<span class="vibe-book-count">'+(index+1)+' / '+termPages.length+'</span>'+
        '<button type="button" data-action="vibe-next" '+(next?'':'disabled')+' aria-label="다음 용어"><span>다음 용어</span> ›</button>'+
      '</footer>'+
    '</div>';
  };

  pageText=function(page){
    const old=basePageText(page);
    return page.vibe ? old+' '+Object.values(page.vibe).join(' ') : old;
  };

  saveReaderPosition=function(){
    if(!isVibe())return baseSaveReaderPosition();
    const chapter=chapters[current.c],id=currentTerm().id;
    state.readerPositions[chapter.id]={logicalId:id,physical:0,total:1,ratio:0};
    saveScroll();
    persist();
  };

  render=function(){
    baseRender();
    entryDirection=0;
    if(!isVibe())return;
    const viewport=document.getElementById('vibe-book-viewport');
    if(!viewport)return;
    ensureScroll();
    const id=currentTerm().id;
    const original=Math.max(0,Number(state.vibeBookScroll[id])||0);
    viewport.scrollTop=original;
    viewport.addEventListener('scroll',function(){
      if(!isVibe()||id!==currentTerm().id)return;
      if(scrollWriteTimer!==null)clearTimeout(scrollWriteTimer);
      scrollWriteTimer=setTimeout(function(){
        scrollWriteTimer=null;
        saveScroll();
      },260);
    },{passive:true});
    installGestures(viewport);
    installEdgeTaps(viewport);
    if(document.body&&document.body.dataset){
      document.body.dataset.vibeReady='1';
      document.body.dataset.vibeTerm=String(currentTerm().vibe.number);
    }
  };

  doAction=function(action,el){
    if(isVibe()){
      if(action==='vibe-prev'){goTerm(-1);return;}
      if(action==='vibe-next'){goTerm(1);return;}
      if(action==='prev'){goTerm(-1);return;}
      if(action==='next'){goTerm(1);return;}
      if(action==='vibe-copy'){
        return baseDoAction('copy-code',{dataset:{code:el.dataset.code||encodeURIComponent(currentTerm().vibe.prompt)}});
      }
      if(action==='vibe-font'){
        if(scrollWriteTimer!==null)clearTimeout(scrollWriteTimer);
        scrollWriteTimer=null;
        saveScroll();
        const size=Number(state.vibeBookFont)||18;
        state.vibeBookFont=size>=22?18:size+2;
        persist();
        render();
        return;
      }
      if(action==='bookmark'){
        const id=currentTerm().id;
        const has=state.bookmarks.includes(id);
        state.bookmarks=has?state.bookmarks.filter(x=>x!==id):state.bookmarks.concat(id);
        persist();
        const button=document.querySelector('.vibe-book-bookmark');
        if(button){button.textContent=has?'☆':'★';button.setAttribute('aria-pressed',has?'false':'true');}
        toast(has?'북마크를 해제했습니다':'북마크에 저장했습니다');
        return;
      }
      if(action==='track-current'||action==='home'||action==='books'||action==='saved'||action==='settings'||action==='search'){
        if(scrollWriteTimer!==null)clearTimeout(scrollWriteTimer);
        scrollWriteTimer=null;
        saveScroll();
      }
    }
    return baseDoAction(action,el);
  };
})();