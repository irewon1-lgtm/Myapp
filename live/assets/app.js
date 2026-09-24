(()=>{'use strict';const C=window.CB;
 const guarded=async fn=>{try{return await fn();}catch(err){console.warn('Chatbook operation:',err.message);C.toast(err.message||'작업을 마치지 못했어요. 다시 시도해 주세요.',6000);}};
 const picker=mode=>{C.closeModal();const f=document.createElement('input');f.type='file';f.accept=mode==='backup'?'.chatbook,.json':'.chatbook,.json,.txt,.md';f.style.display='none';document.body.append(f);f.onchange=()=>{guarded(()=>C.importFile(f.files[0],mode));f.remove();};f.click();};
 C.offlineAll=async()=>{if(window.ChatbookLive)return window.ChatbookLive.saveOffline();if(window.CHATBOOK_PORTABLE||location.protocol==='file:'){C.toast('이 실행파일에 모든 기본 도서와 표지가 이미 포함돼 있어요.');return;}if(!('caches'in window))throw Error('이 브라우저는 오프라인 보관을 지원하지 않아요. 서재 백업을 이용해 주세요.');C.toast('책과 앱 화면을 이 기기에 저장하고 있어요.',6000);const reg=await Promise.race([navigator.serviceWorker?.ready,new Promise((_,reject)=>setTimeout(()=>reject(Error('오프라인 보관을 시작하지 못했어요. 새로고침 후 다시 시도해 주세요.')),12000))]);const cache=await caches.open('chatbook-offline-v1');await cache.addAll(window.CHATBOOK_CACHE_URLS||['./','./index.html','./style.css','./library.js','./core.js','./views.js','./reader.js','./exports.js','./app.js','./vendor/pptxgen.bundle.js',...new Set(C.catalog.books.map(b=>C.art(b.cover)).filter(u=>!/^https?:/i.test(u)))]);await C.filePut('catalog',C.catalog);try{await navigator.storage?.persist?.();}catch{}C.set('offline:saved',Date.now());C.toast('오프라인 서재를 저장했어요. 연결이 끊겨도 다시 읽을 수 있어요.',5000);};
 C.handle=async(a,d,el)=>{switch(a){
 case'nav':C.closeModal();C.go(d.value||'home');break;
 case'read':C.read(d.id||C.selected().id);break;
 case'book-menu':C.showBookMenu(d.id);break;
 case'filter':C.shelfState.filter=d.value;C.refreshShelf();break;
 case'shelf-view':C.shelfState.view=d.value;C.render();break;
 case'book-picker':C.showBookPicker(d.context||'exports');break;
 case'choose-book':C.select(d.id);C.closeModal();if(d.context==='quiz')C.startQuiz(d.id);else if(d.context==='reader')C.read(d.id);else C.go('exports');break;
 case'book-toc':case'reader-toc':C.showToc(d.id||C.Reader.book.id);break;
 case'toc-go':case'annotation-go':C.read(d.id,d.anchor,Number(d.offset)||0);break;
 case'book-export':C.select(d.id);C.closeModal();C.go('exports');break;
 case'book-complete':{const p=C.progress(d.id);C.set('progress:'+d.id,{...p,complete:!p.complete,pct:!p.complete?100:p.pct});C.closeModal();C.render();C.toast(p.complete?'읽는 중으로 바꿨어요.':'완독한 책으로 표시했어요.');break;}
 case'book-hide':C.confirm('서재에서 숨기기','책의 메모와 기록은 지우지 않습니다. 설정에서 다시 꺼낼 수 있어요.',()=>{C.set('hidden:'+d.id,true);C.closeModal();C.render();});break;
 case'hidden-books':{let books=C.catalog.books.filter(b=>C.get('hidden:'+b.id,false));C.showModal('숨긴 책',books.length?books.map(b=>`<div class="toc-item"><div><strong>${C.e(b.title)}</strong></div>${C.btn('다시 꺼내기','unhide-book',`data-id="${C.e(b.id)}"`,'small secondary')}</div>`).join(''):C.empty('숨긴 책이 없어요.'));break;}
 case'unhide-book':C.set('hidden:'+d.id,false);C.closeModal();C.render();C.toast('책을 다시 서재에 꺼냈어요.');break;
 case'reader-back':C.go('library');break;
 case'reader-prev':C.Reader.goPage(C.Reader.page-1);break;
 case'reader-next':C.Reader.goPage(C.Reader.page+1);break;
 case'reader-image':{const block=C.Reader.book?.chapters?.flatMap(ch=>ch.blocks||[]).find(bl=>bl.id===d.blockId);if(block?.src)C.showModal('그림 크게 보기',`<div class="reader-image-modal"><img src="${C.art(block.src)}" alt="${C.e(block.caption||'강의 이해를 돕는 그림')}"/></div>${block.caption?`<p class="reader-image-caption">${C.e(block.caption)}</p>`:''}`,true);break;}
 case'reader-note':C.selectionText='';C.showNote();break;
 case'reader-bookmark':C.Reader.bookmark();break;
 case'selection-save':{if(!C.selectionText)return;C.set('annotation:highlight:'+C.uid(),{kind:'highlight',bookId:C.Reader.book.id,anchor:C.selectionAnchor,excerpt:C.selectionText});const anchor=C.selectionAnchor;C.selectionText='';C.$('#selection')?.remove();C.read(C.Reader.book.id,anchor);C.toast('문장을 저장했어요.');break;}
 case'selection-note':{const anchor=C.selectionAnchor;C.showNote();C.noteDraft.anchor=anchor;C.$('#selection')?.remove();break;}
 case'annotations':C.showAnnotations();break;
 case'all-notes':C.showAnnotations('note');break;
 case'only-bookmarks':C.showAnnotations('bookmark');break;
 case'only-highlights':C.showAnnotations('highlight');break;
 case'edit-note':C.showNote(d.id);break;
 case'annotation-delete':C.confirm('저장 항목 삭제','이 메모 또는 북마크를 삭제할까요?',()=>{C.set(d.id,null);C.showAnnotations();C.Reader.updateControls?.();},true);break;
 case'save-note':{const text=C.$('#note-text').value.trim();if(!text){C.toast('저장할 메모를 먼저 적어주세요.');return;}const v=C.noteDraft;C.set(v.key,{kind:'note',bookId:v.bookId,anchor:v.anchor,offset:v.offset||0,excerpt:v.excerpt,text});C.closeModal();C.toast('메모를 저장했어요.');break;}
 case'export':await C.exportBook(d.kind||d.value,d.id);break;
 case'browser-print':C.browserPrint();break;
 case'all-exports':C.showModal('모든 출력물',C.filesList(true));break;
 case'open-export':await C.openExport(d.id);break;
 case'export-menu':C.exportMenu(d.id);break;
 case'delete-export':C.confirm('출력물 삭제','저장된 출력물 파일과 목록만 지웁니다. 원본 책은 남아 있습니다.',async()=>{await C.fileDelete(d.id);C.set(d.id,null);C.closeModal();if(C.route==='exports')C.render();C.toast('출력물을 삭제했어요.');},true);break;
 case'quiz-choice':{const s=C.quizState();if(s&&!s.submitted){s.selection=Number(d.value);C.set('quiz:draft',s);const scroll=window.scrollY;C.render();window.scrollTo(0,scroll);}break;}
 case'quiz-submit':{const s=C.quizState();if(!s||s.selection===null||s.submitted)return;const q=C.book(s.bookId).questions.find(q=>q.id===s.ids[s.index]);const correct=s.selection===q.answer;const old=C.get('answer:'+q.id,{attempts:0});s.submitted=true;s.answers[s.index]={id:q.id,selection:s.selection,correct};C.set('answer:'+q.id,{bookId:s.bookId,qid:q.id,selection:s.selection,correct,attempts:old.attempts+1,lastAt:Date.now(),interval:correct?3:1,due:correct?Date.now()+3*864e5:Date.now()});C.set('quiz:draft',s);const top=scrollY;C.render();scrollTo(0,top);break;}
 case'quiz-next':{const s=C.quizState();if(!s?.submitted)return;if(s.index===s.ids.length-1){s.finished=true;C.set('attempt:'+C.uid(),{bookId:s.bookId,total:s.ids.length,correct:s.answers.filter(a=>a.correct).length});}else{s.index++;s.selection=null;s.submitted=false;}C.set('quiz:draft',s);C.render();break;}
 case'quiz-retry':C.startQuiz(d.id||C.selected().id,d.wrong==='true');break;
 case'quiz-source':{const b=C.book(d.id);C.read(b.id,b.chapters[Number(d.chapter)||0].id);break;}
 case'review-start':{let items=d.value==='wrong'?C.catalog.books.flatMap(b=>(b.questions||[]).map(q=>({b,q,answer:C.get('answer:'+q.id,null)}))).filter(x=>x.answer&&!x.answer.correct):C.reviewItems();if(!items.length){C.toast('지금 복습할 문제가 없어요.');return;}C.reviewSession={items,index:0,revealed:false};C.go('review');break;}
 case'review-reveal':C.reviewSession.revealed=true;C.render();break;
 case'review-rate':{let rs=C.reviewSession;let item=rs.items[rs.index];C.setReview(item.q.id,d.value==='easy');rs.index++;rs.revealed=false;if(rs.index>=rs.items.length){C.reviewSession=null;C.toast('오늘의 복습을 마쳤어요.');}C.render();break;}
 case'review-stop':C.reviewSession=null;C.render();break;
 case'reading-settings':C.showReadingSettings();break;
 case'pref-value':C.setPref(d.key,d.value);C.Reader.applySettings();C.showReadingSettings();break;
 case'toggle-motion':C.setPref('motion',!C.pref('motion',false));C.Reader.applySettings();C.showReadingSettings();break;
 case'toggle-dark-mode':C.toggleDarkMode();C.render();break;
 case'theme':C.showTheme();break;
 case'theme-set':C.setPref('theme',d.value);C.showTheme();break;
 case'theme-system':C.setPref('theme',C.pref('theme','cream')==='system'?'cream':'system');C.showTheme();break;
 case'account':C.showAccount();break;
 case'save-name':{const n=C.$('#library-name').value.trim();C.set('profile:name',n||'내 서재 계정');C.closeModal();C.render();C.toast('서재 이름을 저장했어요.');break;}
 case'copy-code':await C.copy('CBK-'+C.secret());break;
 case'create-sync-code':case'join-sync':{const code=a==='join-sync'?C.$('#sync-code').value:null;el.disabled=true;el.textContent='서버 연결 확인 중…';try{await C.startSync(code);C.showAccount();C.toast('연결 코드를 만들고 기록을 동기화했어요.');}catch(err){C.showAccount();throw err;}break;}
 case'sync-now':el.disabled=true;try{await C.sync();C.showAccount();C.render();C.toast('독서 기록을 동기화했어요.');}finally{el.disabled=false;}break;
 case'toggle-sync':if(!C.secret()){C.showAccount();return;}C.setPref('sync',!C.pref('sync',false));if(C.pref('sync',false))await C.sync();C.render();break;
 case'unlink-sync':C.confirm('이 기기의 연결 해제','이 기기에 저장된 책과 기록은 유지됩니다. 연결 코드를 잃지 않도록 먼저 보관해 주세요.',()=>{C.storage.removeItem('chatbook.sync.secret');C.setPref('sync',false);C.showAccount();});break;
 case'toggle-autobooks':C.setPref('autoBooks',!C.pref('autoBooks',true));C.render();break;
 case'updates':C.showUpdates();break;
 case'check-books':await C.updateBooks(true);break;
 case'app-refresh':C.closeModal();location.reload();break;
 case'downloads':await C.showDownloads();break;
 case'offline-all':case'offline-book':await C.offlineAll();break;
 case'backup':C.backup();break;
 case'restore-dialog':picker('backup');break;
 case'import-dialog':picker('book');break;
 case'notifications':C.showNotifications();break;
 case'notification-settings':C.showNotificationSettings();break;
 case'notifications-read':for(const n of C.all('notice:'))C.set(n.key,{...n,seen:true});C.closeModal();C.render();break;
 case'toggle-notice-books':C.setPref('noticeBooks',!C.pref('noticeBooks',true));C.showNotificationSettings();break;
 case'toggle-notice-review':C.setPref('noticeReview',!C.pref('noticeReview',false));C.showNotificationSettings();break;
 case'notification-permission':if(!('Notification'in window))throw Error('이 브라우저는 기기 알림을 지원하지 않습니다. 앱 안의 알림함을 이용해 주세요.');await Notification.requestPermission();C.showNotificationSettings();break;
 case'notification-test':await C.notify('챗북','오늘도, 나의 속도로 한 페이지. 알림이 연결됐어요.');C.toast('알림 전송을 요청했어요.');break;
 case'help':C.showHelp();break;
 case'engine-settings':location.href='./engine/';break;
 case'about':C.showAbout();break;
 case'install-help':{if(C.installPrompt){await C.installPrompt.prompt();C.installPrompt=null;}else C.showModal('앱처럼 사용하기',`<p>${location.protocol==='file:'?'현재 파일은 설치 없이 열어 사용하는 실행본입니다. 파일을 보관해 두고 같은 브라우저로 열어주세요.':'Chrome에서 오른쪽 위 메뉴를 열고 ‘홈 화면에 추가’ 또는 ‘앱 설치’를 선택하세요.'}</p><p class="help">기록은 브라우저별로 따로 저장됩니다. 이동하기 전에 ‘서재 백업’을 이용해 주세요.</p>${C.btn('닫기','modal-close','','full')}`);break;}
 case'modal-close':C.closeModal();break;
 case'confirm-action':{const fn=C.pendingConfirm;C.pendingConfirm=null;if(fn)await fn();break;}
 }};
 document.addEventListener('click',ev=>{const el=ev.target.closest('[data-action]');if(!el||el.disabled)return;const a=el.dataset.action;if(a==='overlay-close'){if(ev.target===el)C.closeModal();return;}ev.preventDefault();guarded(()=>C.handle(a,el.dataset,el));});
 document.addEventListener('submit',ev=>{ev.preventDefault();if(ev.target.id==='note-form')guarded(()=>C.handle('save-note',{},ev.target));else if(ev.target.id==='name-form')guarded(()=>C.handle('save-name',{},ev.target));});
 document.addEventListener('input',ev=>{const el=ev.target;if(el.id==='shelf-search'){C.searchTerm=el.value;C.refreshShelf();}if(el.id==='home-search'){C.searchTerm=el.value;}if(el.id==='reader-range')C.Reader.goPage(el.value);if(el.id==='font-size'||el.id==='line-height'){const key=el.id==='font-size'?'fontSize':'lineHeight';C.setPref(key,Number(el.value));C.$('#font-size-label').textContent=C.pref('fontSize',18)+'px';C.$('#line-height-label').textContent=C.pref('lineHeight',1.85);C.$('#font-preview').style.fontSize=C.pref('fontSize',18)+'px';C.$('#font-preview').style.lineHeight=C.pref('lineHeight',1.85);C.Reader.applySettings();}});
 document.addEventListener('change',ev=>{if(ev.target.id==='shelf-sort'){C.shelfState.sort=ev.target.value;C.refreshShelf();}});
 document.addEventListener('keydown',ev=>{if(ev.key==='Enter'&&ev.target.id==='home-search'){ev.preventDefault();C.go('library');}if(ev.key==='Enter'&&ev.target.id==='shelf-search'){ev.preventDefault();ev.target.blur();}});
 window.addEventListener('hashchange',()=>{C.closeModal();C.render();});window.addEventListener('beforeunload',()=>C.Reader.persist());window.addEventListener('beforeinstallprompt',ev=>{ev.preventDefault();C.installPrompt=ev;});
 window.addEventListener('online',()=>{C.toast('인터넷에 다시 연결됐어요.');if(C.pref('sync',false)&&C.secret())C.sync().catch(()=>{});});
 document.addEventListener('visibilitychange',()=>{if(document.hidden){C.Reader.persist();C.Reader.releaseWakeLock?.();return;}if(C.route?.startsWith('read/'))C.Reader.requestWakeLock?.();if(C.pref('autoBooks',true))C.updateBooks();if(C.pref('sync',false)&&C.secret())C.sync().catch(()=>{});});
 window.addEventListener('storage',ev=>{if(ev.key==='chatbook.records.v1'){try{C.merge(JSON.parse(ev.newValue||'{}'));C.applyPrefs();if(!C.modal&&!C.route.startsWith('read/'))C.render();}catch{}}});
 C.init=async()=>{await C.loadBuildInfo?.();try{const cached=await C.fileGet('catalog');if(cached?.books){for(const b of cached.books.filter(b=>b.imported))C.upsertBook(b);}}catch{}if(!C.get('welcome',false)){C.set('welcome',true);C.set('notice:welcome',{title:'첫 번째 서재에 오신 걸 환영해요',text:'디자인 속 책 9권을 기능 체험용 창작 도서로 준비했습니다. 책을 열고, 메모하고, 출력하고, 퀴즈를 풀어 보세요.',seen:false});}C.render();if(!C.persistent)C.toast('이 미리보기 환경에서는 임시 저장만 가능합니다. 닫기 전에 서재를 백업해 주세요.',7000);if(!window.CHATBOOK_PORTABLE&&location.protocol!=='file:'&&'serviceWorker'in navigator){navigator.serviceWorker.register('/live-sw.js',{scope:'/'}).catch(err=>console.warn('Offline unavailable:',err.message));}if(C.pref('autoBooks',true))C.updateBooks();if(C.pref('sync',false)&&C.secret())C.sync().catch(()=>{});if(C.pref('noticeReview',false)&&C.reviewItems().length){const today=new Date().toLocaleDateString();if(C.get('review:notified','')!==today){C.set('review:notified',today);C.set('notice:'+C.uid(),{title:'오늘 다시 만날 질문',text:C.reviewItems().length+'개의 복습 문제가 기다리고 있어요.',seen:false});C.notify('챗북 복습',C.reviewItems().length+'개의 질문을 다시 떠올려 보세요.').catch(()=>{});}}};
 C.init().then(()=>window.ChatbookLive?.ready()).catch(error=>{if(window.ChatbookLive)window.ChatbookLive.fail('init');else console.error(error);});
})();
