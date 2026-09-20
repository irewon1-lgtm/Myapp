const fs=require('fs'),vm=require('vm'),assert=require('assert'),path=require('path');
const root=path.resolve(__dirname,'..');
const course=JSON.parse(fs.readFileSync(path.join(root,'live/content.json'),'utf8'));
const html=fs.readFileSync(path.join(root,'live/reader.html'),'utf8');
const script=html.match(/<script>([\s\S]*?)<\/script>/)[1];
assert(html.includes('UNIFORM_READER_GRID_V6'),'uniform reader grid marker missing');
assert(html.includes('NO_GAP_FULL_PAGE_V7'),'no-gap full-page reader marker missing');
assert(html.includes('--reader-body-size:18px'),'reader body font must stay fixed at 18px');
assert(html.includes('--reader-body-line:1.68'),'reader line-height contract missing');
assert(html.includes('--reader-paragraph-gap:0px'),'reader paragraph gap must be zero');
assert(html.includes('--reader-line-tolerance:3'),'three-line balance tolerance contract missing');
assert(html.includes("fit.dataset.lineTolerance='3'"),'runtime line tolerance marker missing');
assert(html.includes("fit.style.columnFill='balance'"),'balanced chapter pagination missing');
assert(html.includes("fit.dataset.pageMode='balanced-chapter'"),'balanced chapter mode missing');
assert(html.includes('orphans:3')&&html.includes('widows:3'),'paragraph orphan/widow protection missing');
assert(!html.includes('scale=Math.min(1,avail/'),'scale-to-fit must never return');
assert(!html.includes('justify-content:space-evenly'),'sparse-page stretching must never return');
assert(!html.includes('transform=scale<'),'per-page text scaling must never return');

function fixture(legacy){const events={},storage={};const elements={};const node=id=>elements[id]||(elements[id]={innerHTML:'',textContent:'',hidden:true,open:false,scrollTop:0,style:{setProperty(){}},addEventListener(){},showModal(){this.open=true},close(){this.open=false}});node('course').textContent=JSON.stringify(course);const bridge=legacy?{getState:()=>JSON.stringify({}),getLegacy:()=>JSON.stringify(legacy),saveState:s=>storage.saved=s,getStatus:()=> 'TEST',ready(){}}:undefined;const ctx={document:{getElementById:node,documentElement:{style:{setProperty(){}}},body:{classList:{toggle(){}}},addEventListener:(n,f)=>events[n]=f},window:{RoadmapNative:bridge},localStorage:{getItem:k=>storage[k]||null,setItem:(k,v)=>storage[k]=v},console,setTimeout:()=>0,clearTimeout(){},navigator:{},innerWidth:360};vm.createContext(ctx);vm.runInContext(script,ctx);return {ctx,events,elements,storage,run:s=>vm.runInContext(s,ctx)}}
const f=fixture();assert(f.elements.app.innerHTML.includes('한 장씩'));
for(const c of course.chapters)for(const p of c.pages){f.run(`openPage(${JSON.stringify(p.id)})`);assert(f.elements.app.innerHTML.includes('id="paper"'));assert(!f.elements.app.innerHTML.includes('undefined'))}
f.run('openPage("t1-c1-p1");doAction("bookmark",{});openPage("t2-c1-p1");doAction("bookmark",{})');assert.equal(f.run('state.bookmarks.length'),2);
f.run('openPage("t2-c1-p1");doAction("note",{})');f.events.input({target:{id:'note',value:'트랙2 전용 메모'}});assert.equal(f.run('state.notes["t2-c1-p1"]'),'트랙2 전용 메모');assert.equal(f.run('state.notes["t1-c1-p1"]'),undefined);
f.run('openPage("t1-c8-p13");move(1)');assert.equal(f.run('state.location'),'t2-c1-p1');f.run('move(-1)');assert.equal(f.run('state.location'),'t1-c8-p13');
f.run('openPage("t2-c1-p11");move(1)');assert.equal(f.run('chapters[current.c].pages[current.p].kind'),'solution');
assert(f.run('results("NameError")').includes('NameError'));assert(f.run('results("<script>")').includes('검색됨'));
const before=f.run('JSON.stringify(state)');f.run('window.importProgress("bad json")');assert.equal(f.run('JSON.stringify(state)'),before);
for(const route of ['home','books','track','reader','search','saved','settings']){f.run(`route=${JSON.stringify(route)};render()`);assert(f.elements.app.innerHTML.length>100)}
const m=fixture({current_track:1,current_chapter:2,current_page:4,text_scale:1.2,notes_json:'{"0":"예전 메모"}',bookmarks:['0']});assert.equal(m.run('state.location'),'t2-c3-p5');assert.equal(m.run('state.font'),22);assert.equal(m.run('state.legacy.notes_json'),' {"0":"예전 메모"}'.trim());
const report={kind:'JavaScript unit tests with DOM stubs, NOT a rendered browser or Android device test',pages:course.chapters.reduce((n,c)=>n+c.pages.length,0),checks:['all pages generate markup','cross-track bookmark isolation','cross-track note isolation','track boundary navigation','solutions immediately follow exercises','full-text search','invalid import preserves state','all routes render markup','legacy position and notes read-only migration','uniform 18px reader body type contract','uniform 1.68 line-height contract','zero paragraph gap contract','balanced chapter pagination contract','three-line balance tolerance marker','no scale-to-fit','no sparse-page stretching'],status:'PASS'};
fs.mkdirSync(path.join(root,'review'),{recursive:true});
fs.writeFileSync(path.join(root,'review/reader-test-results.json'),JSON.stringify(report,null,2));console.log(report);
