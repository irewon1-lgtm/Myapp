const fs=require('fs'),vm=require('vm'),assert=require('assert'),path=require('path');
const root=path.resolve(__dirname,'..');
const course=JSON.parse(fs.readFileSync(path.join(root,'live/content.json'),'utf8'));
const html=fs.readFileSync(path.join(root,'live/reader.html'),'utf8');
assert(html.includes('LIBRARY_REBUILD_V8'),'clean rebuild marker missing');
assert(html.includes('--readerBody:18px'),'reader font contract missing');
assert(html.includes('--readerLine:1.68'),'reader line-height contract missing');
assert(html.includes('break-before:column')&&html.includes('-webkit-column-break-before:always'),'forced logical-page top break contract missing');
assert(html.includes('<section class="lesson-section"'),'logical page wrapper missing');
assert(html.includes('overflow-y:hidden'),'reader vertical scroll must be disabled');
assert(html.includes("flow.style.columnFill='auto'"),'sequential full-page pagination missing');
assert(html.includes('edge-zone edge-prev')&&html.includes('edge-zone edge-next'),'edge tap navigation missing');
assert(html.includes('TITLE_TOP_PAGE_V1'),'title-top pagination marker missing');
assert(!html.includes("return ribbon+c.pages.map((p,i)=>lessonSection(p,c,i)).join('')"),'chapter ribbon must not sit above page titles');
assert(html.includes('section-marker type-'),'content-type section marker missing');
assert(html.includes("glossary:'용어'")&&html.includes("practice:'실습'")&&html.includes("solution:'풀이'")&&html.includes("summary:'정리'"),'content-type label mapping missing');
assert(html.includes('solution-banner'),'solution visual hierarchy missing');
assert(html.includes('--navSafe:calc(28px + env(safe-area-inset-bottom))'),'Android bottom safe area missing');
assert(!html.includes('scale=Math.min(1,avail/'),'scale-to-fit must not return');
assert(!html.includes('space-evenly'),'sparse filler stretching must not return');
assert(!html.includes('PREMIUM_MINIMAL_V3')&&!html.includes('UNIFORM_READER_GRID_V6')&&!html.includes('NO_GAP_FULL_PAGE_V7'),'legacy CSS layers must not return');
const ids=[...course.chapters.flatMap(c=>c.pages.map(p=>p.id))];
assert.equal(ids.length,new Set(ids).size,'duplicate page ids');
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
    'sequential top-to-bottom horizontal pagination','forced new physical page for every logical lesson title',
    'edge tap navigation','chapter hierarchy ribbon','content-type markers','solution hierarchy banner',
    'Android bottom safe area'
  ],
  status:'PASS'
};
fs.mkdirSync(path.join(root,'review'),{recursive:true});
fs.writeFileSync(path.join(root,'review/reader-test-results.json'),JSON.stringify(report,null,2));
console.log(report);
