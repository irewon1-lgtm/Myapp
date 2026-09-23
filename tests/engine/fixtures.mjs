/** Fixtures deliberately do not simulate actual model intelligence. */
export function syntheticTranscript(duration=780,step=30){
  const sentence='백엔드는 사용자의 요청을 받고 데이터를 읽거나 저장한 뒤 결과를 돌려줍니다. 서버와 화면은 서로 다른 역할을 맡으며 API는 데이터를 요청하는 창구입니다. 예를 들어 방명록에 글을 남기면 다음 방문자는 그 글을 볼 수 있습니다.';
  const events=[];for(let s=0;s<duration;s+=step)events.push({tStartMs:s*1000,dDurationMs:Math.min(step,duration-s)*1000,segs:[{utf8:sentence.repeat(3)}]});return JSON.stringify({events});
}
export function basisFor(packet){
 return {title:'Synthetic test chapter',units:packet.ownedSourceIds.map(id=>{const s=packet.SOURCE_PAYLOAD.find(x=>x.id===id);return{id:'u-'+id,kind:'definition',text:s.text,sourceRefs:[id],evidence:[{sourceId:id,quote:s.text.slice(0,30)}]};}),omissions:[],issues:[]};
}
export function draftFor(packet){return{title:'Synthetic test chapter',paragraphs:packet.LOCKED_BASIS.units.map((u,i)=>({id:'p'+i,kind:'source',text:u.text,unitIds:[u.id],sourceRefs:u.sourceRefs})),glossary:[],figures:[],questions:[],issues:[]};}
export function reviewFor(packet){return{reviewer:'test-reviewer',sourceHash:packet.sourceHash,basisHash:packet.basisHash,draftHash:packet.draftHash,fidelity:'approved',beginner:'approved',sourceAmbiguities:'approved',visualCompleteness:'approved',checkedUnitIds:packet.LOCKED_BASIS.units.map(u=>u.id),issues:[],resolvedFlags:[]};}
export const fixtureProvider={kind:'test-fixture',model:'deterministic-fixture-NOT-AN-AI',generate:async packet=>({actor:packet.stage==='review'?'test-reviewer':'test-author',model:'deterministic-fixture',artifact:packet.stage==='source'?basisFor(packet):packet.stage==='beginner'?draftFor(packet):reviewFor(packet)})};
