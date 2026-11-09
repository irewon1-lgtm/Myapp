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
    if(target)openPage(target.id);
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
        '<article class="vibe-book-page" data-page-id="'+esc(page.id)+'">'+
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
    document.body.dataset.vibeReady='1';
    document.body.dataset.vibeTerm=String(currentTerm().vibe.number);
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