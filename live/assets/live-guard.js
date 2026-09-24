/* Stable boot/recovery guard. Never clears user records, books, sync secrets or IndexedDB. */
(()=>{'use strict';
 const tag=document.currentScript;if(!tag?.dataset.commit)return;
 const commit=tag.dataset.commit,sequence=Number(tag.dataset.sequence),manifestSha=tag.dataset.manifest;
 const valid=x=>x==='bundled'||/^[a-f0-9]{40}$/.test(x||'');
 const read=(key,fallback)=>{try{return JSON.parse(localStorage.getItem(key))??fallback;}catch{return fallback;}};
 const write=(key,value)=>{try{localStorage.setItem(key,JSON.stringify(value));}catch{}};
 const last=read('chatbook.live.good.v1',null),blocked=read('chatbook.live.blocked.v1',{});
 let failed=false,healthy=false,timer,checking=false,lastCheck=0;
 const state=window.ChatbookLive={commit,sequence,manifestSha,pending:true,offlineReady:false};
 function backTo(target){
   if(!valid(target)||target===commit)return false;
   const u=new URL(location.href);u.pathname='/';u.search='';u.searchParams.set('cb_release',target);u.searchParams.set('cb_recovery','1');
   try{window.stop();}catch{}location.replace(u.href);return true;
 }
 function fail(reason){
   if(failed)return;failed=true;clearTimeout(timer);state.pending=true;
   if(commit!=='bundled'){blocked[commit]=Date.now();write('chatbook.live.blocked.v1',Object.fromEntries(Object.entries(blocked).slice(-8)));}
   const forced=new URL(location.href).searchParams.get('cb_recovery');
   if(!forced&&last&&valid(last.commit)&&last.commit!==commit&&backTo(last.commit))return;
   if(commit!=='bundled'&&backTo('bundled'))return;
   document.addEventListener('DOMContentLoaded',()=>{
     const box=document.createElement('section');box.setAttribute('role','alert');
     box.textContent='화면을 복구하지 못했습니다. 메모와 책은 삭제하지 않았습니다. 인터넷 연결을 확인하고 다시 열어 주세요.';
     box.style.cssText='position:fixed;inset:20px;z-index:99999;padding:24px;background:#fff;color:#222;line-height:1.8';document.body.append(box);
   },{once:true});
   console.warn('Chatbook protected recovery:',reason);
 }
 state.fail=fail;
 if(blocked[commit]&&commit!=='bundled'&&!new URL(location.href).searchParams.has('cb_retry')){fail('previously rejected release');return;}
 if(last&&sequence<last.sequence&&!new URL(location.href).searchParams.has('cb_release')){backTo(last.commit);return;}
 window.addEventListener('error',e=>{if(!healthy&&(e.message||e.target?.tagName==='SCRIPT'))fail('script startup failure');},true);
 window.addEventListener('unhandledrejection',()=>{if(!healthy)fail('startup promise failure');});
 // A hidden/background tab is not declared broken just because mobile timers are throttled.
 function watchdog(){if(healthy||failed)return;if(document.hidden){timer=setTimeout(watchdog,2000);return;}fail('startup timeout');}
 timer=setTimeout(watchdog,25000);
 state.ready=async()=>{
   if(failed||healthy)return;const C=window.CB;
   if(!C?.catalog?.books?.length||!document.querySelector('#app')?.children.length||typeof C.read!=='function'||typeof C.Reader?.mount!=='function'){fail('reader contract');return;}
   await new Promise(resolve=>setTimeout(resolve,250));if(failed)return;
   healthy=true;state.pending=false;clearTimeout(timer);
   const good={commit,sequence,manifestSha};if(commit!=='bundled'&&(!last||sequence>=last.sequence))write('chatbook.live.good.v1',good);
   C.save?.();if(C.pref?.('sync',false)&&C.secret?.())C.sync().catch(()=>{});
   if('serviceWorker'in navigator){
     try{await navigator.serviceWorker.register('/live-sw.js',{scope:'/'});const registration=await navigator.serviceWorker.ready;
       const send=()=>{(navigator.serviceWorker.controller||registration.active)?.postMessage({type:'CHATBOOK_HEALTHY',...good});};
       send();navigator.serviceWorker.addEventListener('controllerchange',send,{once:true});
     }catch{C.toast?.('현재 화면은 사용할 수 있지만 이 브라우저에서는 오프라인 보관을 준비하지 못했어요.',6000);}
   }
   if(new URL(location.href).searchParams.has('cb_recovery'))C.toast?.('새 화면 대신 이전 정상 화면으로 복구했어요. 독서 기록은 유지됩니다.',6500);
 };
 state.check=async(manual=false)=>{
   if(checking||failed||(!manual&&Date.now()-lastCheck<60000))return;checking=true;lastCheck=Date.now();
   try{const r=await fetch('/__cb/channel.json',{cache:'no-store',signal:AbortSignal.timeout(6000)});if(!r.ok)throw Error();const c=await r.json();
     if(c.schema!==1||!valid(c.current?.commit)||!Number.isSafeInteger(c.sequence))throw Error();
     if(c.sequence>sequence&&c.current.commit!==commit&&!blocked[c.current.commit]){
       state.available=c.current.commit;
       if(manual){if(window.CB?.modal||document.activeElement?.matches('input,textarea')){window.CB.toast('입력 중인 내용을 저장하고 창을 닫은 뒤 새 책 확인을 다시 눌러 주세요.',6000);return;}window.CB?.Reader?.persist?.();const u=new URL(location.href);u.pathname='/';u.search='';location.replace(u.href);}
       else window.CB?.toast?.('새 교재·화면이 준비됐어요. 설정의 새 책 확인으로 적용할 수 있어요.',5000);
     }else if(manual)window.CB?.toast?.('검증된 최신 서재입니다.');
   }catch{if(manual)window.CB?.toast?.('새 버전을 확인하지 못했어요. 지금 저장된 책과 기록은 그대로 사용할 수 있어요.',6000);}finally{checking=false;}
 };
 state.saveOffline=async()=>{
   if(!('serviceWorker'in navigator))throw Error('이 브라우저에서는 오프라인 보관을 지원하지 않아요.');
   const reg=await navigator.serviceWorker.ready;
   (navigator.serviceWorker.controller||reg.active)?.postMessage({type:'CHATBOOK_HEALTHY',commit,sequence,manifestSha,notify:true});
   window.CB?.toast?.('검증된 화면과 책을 보관하고 있어요. 완료 알림을 기다려 주세요.',5000);
 };
 navigator.serviceWorker?.addEventListener('message',e=>{
   if(e.data?.type==='CHATBOOK_OFFLINE_READY'){state.offlineReady=true;if(e.data.notify)window.CB?.toast?.('현재 교재와 화면의 오프라인 보관을 완료했어요.',5000);}
   if(e.data?.type==='CHATBOOK_OFFLINE_FAILED'&&e.data.notify)window.CB?.toast?.('새 버전 오프라인 보관을 완료하지 못했어요. 기존 저장본은 유지됩니다.',6000);
 });
})();
