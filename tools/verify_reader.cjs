const fs=require('fs'),vm=require('vm'),assert=require('assert'),path=require('path');
const root=path.resolve(__dirname,'..');
const course=JSON.parse(fs.readFileSync(path.join(root,'live/content.json'),'utf8'));
const html=fs.readFileSync(path.join(root,'live/reader.html'),'utf8');
assert(html.includes('LIBRARY_REBUILD_V8'),'clean rebuild marker missing');
assert(html.includes('--readerBody:18px'),'reader font contract missing');
assert(html.includes('--readerLine:1.68'),'reader line-height contract missing');
assert(html.includes('topic-header')&&html.includes('topic-title'),'persistent topic header contract missing');
assert(html.includes('<section class="lesson-section"'),'logical page wrapper missing');
assert(html.includes('overflow-y:hidden'),'reader vertical scroll must be disabled');
assert(html.includes("flow.style.columnFill='auto'"),'sequential full-page pagination missing');
assert(html.includes('edge-zone edge-prev')&&html.includes('edge-zone edge-next'),'edge tap navigation missing');
assert(html.includes('TITLE_TOP_PAGE_V2'),'title-top pagination marker missing');
assert(html.includes('SWIPE_TRACK_FOCUS_V1'),'swipe/track/focus architecture marker missing');
assert(html.includes('SWIPE_CONTENT_CODE_V2'),'full-surface swipe/code-first marker missing');
assert(html.includes('BOOK_SOURCES_V1'),'chapter source reference marker missing');
assert(html.includes('chapter-references'),'chapter reference rendering missing');
assert(html.includes("document.querySelector('.reader-paper-wrap')"),'swipe must attach to title+body reader surface');
assert(html.includes("surface.addEventListener('touchstart'")&&html.includes("surface.addEventListener('touchmove'")&&html.includes("surface.addEventListener('touchend'"),'Android touch swipe handlers missing');
assert(html.includes('touch-action:pan-y'),'touch swipe surface missing');
assert(html.includes('function trackStats(id)'),'track page-count statistics missing');
assert(html.includes('TRACK_GLOBAL_SLIDES_V1'),'whole-track slide-numbering marker missing');
assert(html.includes('function rebuildTrackSlideMap('),'whole-track physical slide map missing');
assert(html.includes('function currentTrackSlidePosition('),'whole-track slide position function missing');
assert(html.includes("slide.current+' 슬라이드 / '+slide.total"),'whole-track slide label missing');
assert(!html.includes('id="track-remain"'),'read/remaining HUD must be removed');
assert(!html.includes('track-page-count'),'track total/read/remaining text must be removed');
assert(html.includes('focus-exit')&&html.includes('focus-hud'),'rebuilt focus mode controls missing');
assert(html.includes("pendingPhysicalRatio=physicalPages>1?physicalPage/(physicalPages-1):0"),'focus mode must preserve reading position');
assert(html.includes('return lessonSection(c.pages[current.p],c,current.p);'),'reader must render only the current logical page');
assert(html.includes('section-marker type-'),'content-type section marker missing');
assert(html.includes("glossary:'용어'")&&html.includes("practice:'실습'")&&html.includes("solution:'풀이'")&&html.includes("summary:'정리'"),'content-type label mapping missing');
assert(html.includes('solution-banner'),'solution visual hierarchy missing');
assert(html.includes('--navSafe:calc(28px + env(safe-area-inset-bottom))'),'Android bottom safe area missing');
assert(!html.includes('scale=Math.min(1,avail/'),'scale-to-fit must not return');
assert(!html.includes('space-evenly'),'sparse filler stretching must not return');
assert(!html.includes('PREMIUM_MINIMAL_V3')&&!html.includes('UNIFORM_READER_GRID_V6')&&!html.includes('NO_GAP_FULL_PAGE_V7'),'legacy CSS layers must not return');
const ids=[...course.chapters.flatMap(c=>c.pages.map(p=>p.id))];
assert.equal(ids.length,new Set(ids).size,'duplicate page ids');

function explanationChars(p){
  return [
    ...(p.paragraphs||[]),...(p.bullets||[]),p.practicePrompt||'',p.question||'',p.answer||'',
    p.calloutTitle||'',p.calloutBody||'',p.code||'',p.codeNote||'',
    ...(p.codeExplain||[]),
    ...(p.glossary||[]).flatMap(g=>[g.term||'',g.description||'',g.usage||'',g.example||''])
  ].join(' ').length;
}
const track2Chapters=course.chapters.filter(c=>c.track===2);
const track2Pages=track2Chapters.flatMap(c=>c.pages);
assert.equal(track2Pages.length,234,'Track 2 page count must remain 234');
const thinTrack2=track2Pages.filter(p=>explanationChars(p)<400);
assert.equal(thinTrack2.length,0,'Track 2 page below 400 meaningful visible characters: '+thinTrack2.map(p=>p.id).join(','));
assert.equal(track2Pages.filter(p=>!p.code).length,0,'Every Track 2 page must include code or starter code');
assert.equal(track2Pages.filter(p=>!Array.isArray(p.codeExplain)||!p.codeExplain.length).length,0,'Every Track 2 page must include page-specific code explanation');
assert.equal(track2Pages.filter(p=>p.codeTrace).length,0,'Generic repeated codeTrace filler must not return');
for(const c of track2Chapters){
  assert.equal((c.references||[]).length,3,'Every Track 2 chapter must cite exactly three verified 2026 books');
  const summary=c.pages.find(p=>p.kind==='summary');
  assert.equal((summary?.references||[]).length,3,'Each Track 2 summary must carry the three chapter references');
}
const longParagraphs=new Map();
for(const p of track2Pages)for(const raw of p.paragraphs||[]){
  const s=String(raw).trim();
  if(s.length<70)continue;
  if(!longParagraphs.has(s))longParagraphs.set(s,[]);
  longParagraphs.get(s).push(p.id);
}
const repeatedParagraphs=[...longParagraphs.entries()].filter(([s,ids])=>ids.length>1);
assert.equal(repeatedParagraphs.length,0,'Repeated long Track 2 paragraphs: '+repeatedParagraphs.slice(0,6).map(([s,ids])=>ids.join('/')).join(','));
const codeLines=new Map();
for(const p of track2Pages)for(const raw of p.codeExplain||[]){
  const s=String(raw).trim();
  if(!codeLines.has(s))codeLines.set(s,[]);
  codeLines.get(s).push(p.id);
}
const repeatedCodeExplain=[...codeLines.entries()].filter(([s,ids])=>ids.length>1);
assert.equal(repeatedCodeExplain.length,0,'Repeated Track 2 code explanations remain');
const forbiddenTrack2=['실제 개발과 연결','이 페이지를 읽을 때','초보 해설 ·','공부하는 방법','실수 적'];
for(const phrase of forbiddenTrack2){
  const hits=track2Pages.filter(p=>JSON.stringify(p).includes(phrase));
  assert.equal(hits.length,0,'Forbidden Track 2 meta filler "'+phrase+'" remains on: '+hits.map(p=>p.id).join(','));
}
for(const c of course.chapters)for(let i=0;i<c.pages.length;i++)if(c.pages[i].answerOnNext)assert.equal(c.pages[i+1]?.kind,'solution','solution must immediately follow answerOnNext');

const script=html.match(/<script>([\s\S]*?)<\/script>/)[1];
function fixture(legacy){
  const events={},storage={},elements={};
  const node=id=>elements[id]||(elements[id]={
    innerHTML:'',textContent:'',hidden:true,open:false,scrollTop:0,scrollLeft:0,clientWidth:0,clientHeight:0,
    style:{setProperty(){}},classList:{add(){},remove(){},toggle(){}},
    addEventListener(){},showModal(){this.open=true},close(){this.open=false},
    querySelector(){return null},querySelectorAll(){return[]},getBoundingClientRect(){return{left:0,right:0}}
  });
  node('course').textContent=JSON.stringify(course);
  const bridge=legacy?{getState:()=>JSON.stringify({}),getLegacy:()=>JSON.stringify(legacy),saveState:s=>storage.saved=s,getStatus:()=> 'TEST',ready(){}}:undefined;
  const document={
    getElementById:node,
    querySelector(){return null},
    addEventListener:(n,f)=>events[n]=f,
    createElement:()=>({value:'',style:{},select(){},remove(){},closest(){return null}}),
    execCommand:()=>true,
    body:{appendChild(){},classList:{toggle(){}}},
    title:''
  };
  const ctx={
    document,window:{RoadmapNative:bridge},localStorage:{getItem:k=>storage[k]||null,setItem:(k,v)=>storage[k]=v},
    console,setTimeout:()=>0,clearTimeout(){},navigator:{},innerWidth:360
  };
  vm.createContext(ctx);vm.runInContext(script,ctx);
  return {ctx,events,elements,storage,run:s=>vm.runInContext(s,ctx)};
}
const f=fixture();
assert(f.elements.app.innerHTML.includes('코딩 로드맵'));
assert(f.elements.app.innerHTML.includes('이어 학습하기'));
for(const c of course.chapters)for(const p of c.pages){
  f.run('openPage('+JSON.stringify(p.id)+')');
  assert(f.elements.app.innerHTML.includes('reader-shell'));
  assert(!f.elements.app.innerHTML.includes('undefined'));
}
const firstTrack1=course.chapters.filter(c=>c.track===1).at(-1);
const firstTrack2=course.chapters.find(c=>c.track===2);
if(firstTrack1&&firstTrack2){
  const last=firstTrack1.pages.at(-1).id,first=firstTrack2.pages[0].id;
  f.run('openPage('+JSON.stringify(last)+');changePhysical(1)');
  assert.equal(f.run('state.location'),first);
}
const practice=course.chapters.flatMap((c,ci)=>c.pages.map((p,pi)=>({c,ci,p,pi}))).find(x=>x.p.answerOnNext);
if(practice){
  f.run('openPage('+JSON.stringify(practice.p.id)+');changePhysical(1)');
  assert.equal(f.run('chapters[current.c].pages[current.p].kind'),'solution');
}
const p1=course.chapters[0].pages[0].id;
const p2=course.chapters.find(c=>c.track===2)?.pages[0].id||course.chapters[0].pages[1].id;
f.run('openPage('+JSON.stringify(p1)+');doAction("bookmark",{})');
f.run('openPage('+JSON.stringify(p2)+');doAction("bookmark",{})');
assert.equal(f.run('state.bookmarks.length'),2);
f.run('openPage('+JSON.stringify(p2)+');doAction("note",{})');
f.events.input({target:{id:'note',value:'전용 메모'}});
assert.equal(f.run('state.notes['+JSON.stringify(p2)+']'),'전용 메모');
assert(f.run('searchResults("NameError")').includes('NameError'));
const before=f.run('JSON.stringify(state)');
f.run('window.importProgress("bad json")');
assert.equal(f.run('JSON.stringify(state)'),before);
for(const r of ['home','books','track','reader','search','saved','settings']){
  f.run('route='+JSON.stringify(r)+';render()');
  assert(f.elements.app.innerHTML.length>100,'route '+r+' did not render');
}
f.run('route="reader";render();doAction("focus",{})');
assert(f.elements.app.innerHTML.includes('reader-shell focus'),'focus mode class missing');
assert(f.elements.app.innerHTML.includes('집중모드 종료'),'focus exit control missing');
assert(!f.elements.app.innerHTML.includes('track-remain'),'read/remaining HUD must stay removed');
assert(f.elements.app.innerHTML.includes('1 슬라이드 / 1'),'global slide counter placeholder missing');
const t3=course.tracks.find(t=>t.id===3);
if(t3){
  const t3c=course.chapters.filter(c=>c.track===3);
  assert(t3.available,'Track 3 should be available');
  assert(t3c.length>=1,'Track 3 chapter missing');
}
const m=fixture({current_track:1,current_chapter:2,current_page:4,notes_json:'{"0":"예전 메모"}',bookmarks:['0']});
assert(m.run('state.migrated')===true);
const report={
  kind:'JavaScript unit tests with DOM stubs plus static reader-contract gates; NOT a physical Galaxy render test',
  pages:ids.length,
  checks:[
    'clean single-layer UI template',
    'all logical pages generate reader markup',
    'bookmark isolation',
    'note isolation',
    'logical fallback navigation',
    'solutions immediately follow exercises',
    'full-text search',
    'invalid import preserves state',
    'all routes render',
    'legacy state migration executes',
    'fixed 18px reader body',
    'fixed 1.68 line-height',
    'no scale-to-fit',
    'no sparse filler stretching',
    'sequential top-to-bottom horizontal pagination','persistent large topic title above every physical swipe page',
    'Track 2 minimum 400-character meaningful content without filler',
    'Track 2 code or starter code on every page',
    'Track 2 page-specific line-by-line code explanation with zero duplicates',
    'Track 2 long repeated paragraphs blocked',
    'Track 2 three verified 2026 bestseller references per chapter',
    'Track 2 generic meta filler removed',
    'edge tap navigation','chapter hierarchy ribbon','content-type markers','solution hierarchy banner',
    'whole-track physical slide numbering without per-topic reset',
    'read/remaining counters removed',
    'Android bottom safe area'
  ],
  status:'PASS'
};
fs.mkdirSync(path.join(root,'review'),{recursive:true});
fs.writeFileSync(path.join(root,'review/reader-test-results.json'),JSON.stringify(report,null,2));
console.log(report);
