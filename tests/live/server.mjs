/** LOCAL-ONLY fault injection server. Never included in a Netlify function or public bundle. */
import http from 'node:http';import fs from 'node:fs';import path from 'node:path';
import {createGateway,sha} from '../../netlify/lib/live-gateway.mjs';
const root=process.cwd(),dist=path.join(root,'dist');
const initial=JSON.parse(fs.readFileSync('live/release.json'));
const original=Object.fromEntries(Object.keys(initial.assets).map(p=>[p,fs.readFileSync(path.join(dist,p))]));
function release(char,sequence,patch={},tweak=()=>{}){const files={...original,...patch},m=structuredClone(initial);m.sequence=sequence;for(const[p,b]of Object.entries(files))m.assets[p]={sha256:sha(b),bytes:b.length};tweak(m);const bytes=Buffer.from(JSON.stringify(m,null,2)+'\n');return{commit:char.repeat(40),files,manifest:m,bytes,hash:sha(bytes)};}
const nextCatalog=JSON.parse(original['content/catalog.json']);
nextCatalog.version='live-fixture-B';
nextCatalog.books[0].chapters[0].title='LIVE_CHAPTER_UPDATE_CONFIRMED';
nextCatalog.books[0].chapters[0].blocks[0].text='LIVE_BODY_UPDATE_CONFIRMED';
const extra=structuredClone(nextCatalog.books[0]);extra.id='live-fixture-new-book';extra.title='LIVE_NEW_BOOK_CONFIRMED';extra.questions=[];extra.chapters=[{id:'live-fixture-ch1',title:'새 장',blocks:[{id:'live-fixture-p1',type:'p',text:'새 책 등록 테스트'}]}];nextCatalog.books.push(extra);
const A=release('a',100),B=release('b',200,{'views.js':Buffer.concat([original['views.js'],Buffer.from('\nwindow.__LIVE_TEST_EDITION="B";\n')]),'content/catalog.json':Buffer.from(JSON.stringify(nextCatalog)),'library.js':Buffer.from('window.CHATBOOK_CATALOG='+JSON.stringify(nextCatalog)+';')});
const syntax=release('c',300,{'core.js':Buffer.from('const invalid syntax @@@;')});
const corrupt=release('d',400);corrupt.files['core.js']=Buffer.from('tampered');
const partial=release('e',500);delete partial.files['library.js'];
const future=release('f',600,{},m=>m.shell=2);
const recovered=release('1',700);
const map=new Map([A,B,syntax,corrupt,partial,future,recovered].map(r=>[r.commit,r]));
const bundle=release('0',1);fs.mkdirSync('review/live-local',{recursive:true});fs.writeFileSync('review/live-local/bootstrap-manifest.json',bundle.bytes);
let mode='a',failPath='',requests=[];
const gateway=createGateway({bundleRoot:dist,manifestFile:path.join(root,'review/live-local/bootstrap-manifest.json'),channelTTL:0,fetcher:async(url)=>{
 requests.push(url);if(mode==='upstream-down')throw Error('injected upstream outage');
 if(url.endsWith('/live/channel.json')){
   if(mode==='malformed')return new Response('{broken',{status:200});
   const r=map.get(mode.repeat(40))||A;return new Response(JSON.stringify({schema:1,kind:'chatbook-live-channel',sequence:r.manifest.sequence,current:{commit:r.commit,manifestSha:r.hash}}));
 }
 const m=url.match(/\/([a-f0-9]{40})\/live\/(.*)$/);if(!m)return new Response('',{status:404});const r=map.get(m[1]);if(!r)return new Response('',{status:404});
 if(m[2]==='release.json')return new Response(r.bytes);
 const file=m[2].replace(/^assets\//,'');if(file===failPath||!r.files[file])return new Response('deliberate failure',{status:503});return new Response(r.files[file]);
}});
const server=http.createServer(async(req,res)=>{
 try{
   const u=new URL(req.url,'http://127.0.0.1');
   if(u.pathname==='/__test/control'){let b='';for await(const chunk of req)b+=chunk;const d=JSON.parse(b||'{}');mode=d.mode||mode;failPath=d.failPath||'';res.setHeader('content-type','application/json');res.end(JSON.stringify({mode,failPath,requests:requests.length}));return;}
   if(u.pathname==='/__test/requests'){res.setHeader('content-type','application/json');res.end(JSON.stringify(requests));return;}
   if(u.pathname.startsWith('/api/')){res.setHeader('Content-Type','application/json');res.statusCode=401;res.end('{"error":"test only; no real sync credentials"}');return;}
   const out=await gateway(new Request('http://127.0.0.1:'+server.address().port+req.url,{method:req.method}));res.writeHead(out.status,Object.fromEntries(out.headers));res.end(Buffer.from(await out.arrayBuffer()));
 }catch(e){res.statusCode=500;res.end(String(e));}
});server.listen(0,'127.0.0.1',()=>{const port=server.address().port;fs.writeFileSync('review/live-local/server.json',JSON.stringify({port}));console.log('LOCAL_LIVE_TEST_PORT='+port);});
