/** One-time integration into the existing Chatbook branch. Never changes books. */
import fs from 'node:fs';
import {DEFAULTS} from '../public/engine/core.mjs';
const put=(p,s)=>fs.writeFileSync(p,s);
function replaceOnce(p,from,to,marker){let s=fs.readFileSync(p,'utf8');if(s.includes(marker))return;if(s.split(from).length!==2)throw Error('Integration anchor missing or ambiguous: '+p);put(p,s.replace(from,to));}
const info="${C.settingsRow('info','앱 정보','버전 정보, 데이터 보관, 콘텐츠 안내','about','','blue')}";
replaceOnce('public/views.js',info,"${C.settingsRow('layers','교재 제작 엔진','원문 보존 · 초보자 해설 · 검수 기준','engine-settings','','green')}"+info,"'engine-settings'");
replaceOnce('public/app.js',"case'about':C.showAbout();break;","case'engine-settings':location.href='./engine/';break;\n case'about':C.showAbout();break;","case'engine-settings'");
let index=fs.readFileSync('public/index.html','utf8');for(const name of['views','app'])index=index.replace(new RegExp(`src="\\./${name}\\.js(?:\\?[^\"]*)?"`),`src="./${name}.js?v=engine-1.0.0"`);put('public/index.html',index);
// Existing builder re-created v1 regardless of previous sw.js edits. Use content hash.
let build=fs.readFileSync('scripts/build.mjs','utf8');build=build.replace("const NAME='chatbook-offline-v1'","const NAME='chatbook-offline-${digest.slice(0,12)}'");put('scripts/build.mjs',build);
const pkg=JSON.parse(fs.readFileSync('package.json','utf8'));if(!pkg.scripts.test.includes('tests/engine'))pkg.scripts.test+=' && node --test tests/engine/*.test.mjs';pkg.scripts['engine:benchmark']='node engine/benchmark.mjs';pkg.scripts['engine:test']='node --test tests/engine/*.test.mjs';put('package.json',JSON.stringify(pkg,null,2)+'\n');
put('public/engine/defaults.json',JSON.stringify(DEFAULTS,null,2)+'\n');
console.log('Integrated engine panel and tests. Existing book catalog and reader code left untouched.');
