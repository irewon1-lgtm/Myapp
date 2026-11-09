/* VIBE_GLOSSARY_V1 — isolated Track 12 reader, no legacy page rewriting. */
(function(){
  'use strict';
  const inheritedLessonSection=lessonSection;
  const inheritedKindLabel=kindLabel;
  const inheritedPageText=pageText;
  const inheritedReader=reader;
  const inheritedDoAction=doAction;

  function block(title,body,cssClass){
    return '<section class="vibe-card '+(cssClass||'')+'"><h3>'+esc(title)+'</h3><p>'+txt(body)+'</p></section>';
  }
  lessonSection=function(page,c,index){
    if(page.kind!=='vibe-term')return inheritedLessonSection(page,c,index);
    const t=page.vibe;
    if(!t||c.track!==12)throw new Error('Invalid Track 12 term content');
    let out='<section class="lesson-section vibe-lesson" data-logical-page-id="'+esc(page.id)+'">'+
      '<span class="logical-anchor" data-page-id="'+esc(page.id)+'" data-page-index="'+index+'" aria-hidden="true"></span>'+
      '<p class="vibe-intro">용어 '+t.number+' / 200 · 이번에는 1~50번을 학습합니다.</p>';
    out+=block('① 쉬운 뜻',t.explain,'vibe-meaning');
    out+=block('② 실제 앱 개발에서는',t.example,'vibe-example');
    out+=block('③ 비슷한 용어와 차이',t.contrast,'vibe-contrast');
    out+='<section class="vibe-card vibe-quiz"><h3>④ AI의 잘못된 판단 찾기</h3><p>'+txt(t.wrong)+'</p>'+
      '<details class="vibe-answer"><summary>정답·이유 펼쳐 보기</summary><p>'+txt(t.answer)+'</p></details></section>';
    out+='<section class="vibe-card vibe-copy"><h3>⑤ Codex에 이렇게 지시하세요</h3>'+
      '<p class="vibe-hint">실제 앱에 맞게 바꿔 써도 됩니다. 버튼을 누르면 문장이 복사됩니다.</p>'+
      '<p class="vibe-prompt">'+txt(t.prompt)+'</p>'+
      '<button type="button" class="vibe-copy-button" data-action="copy-code" data-code="'+encodeURIComponent(t.prompt)+'">▣ 지시문 복사</button>'+
      '</section>';
    if(index===c.pages.length-1){
      out+='<div class="chapter-end"><div class="actions">'+
        '<button class="reader-action" data-action="complete">'+(state.complete.includes(c.id)?'완료 표시 취소':'완료로 표시')+'</button>'+
        '<button class="reader-action" data-action="track-current">목차로</button></div></div>';
    }
    return out+'</section>';
  };
  kindLabel=function(kind){return kind==='vibe-term'?'실전 용어':inheritedKindLabel(kind)};
  pageText=function(page){
    const base=inheritedPageText(page);
    return page.vibe ? base+' '+Object.values(page.vibe).join(' ') : base;
  };
  reader=function(){
    const output=inheritedReader();
    if(chapters[current.c].track!==12)return output;
    return output.replace(
      /<button data-action="practice">[\\s\\S]*?<\\/button>/,
      '<button data-action="vibe-copy" aria-label="AI 지시문 복사">▣<span>지시문 복사</span></button>'
    );
  };
  doAction=function(action,el){
    if(action==='vibe-copy'&&chapters[current.c].track===12){
      const page=chapters[current.c].pages[current.p];
      return inheritedDoAction('copy-code',{dataset:{code:encodeURIComponent(page.vibe.prompt)}});
    }
    return inheritedDoAction(action,el);
  };
})();
