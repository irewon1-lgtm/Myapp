/** Assemble the already-tested public bundle. No generation API and no Netlify deployment. */
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import {fileURLToPath} from 'node:url';
import {sha,validateManifest,safePath} from '../netlify/lib/live-gateway.mjs';
import {walk,verifyRelease} from './engine-info.mjs';
const need=(ok,msg)=>{if(!ok)throw Error('Live publication blocked: '+msg);};
export function validateCatalog(c){
 need(c?.schema===1&&Array.isArray(c.books)&&c.books.length>0,'catalog schema');const ids=new Set();
 for(const b of c.books){need(typeof b.id==='string'&&b.id&&!ids.has(b.id),'duplicate/missing book ID');ids.add(b.id);
   need(typeof b.title==='string'&&b.title&&Array.isArray(b.chapters)&&b.chapters.length>0,'empty book');const anchors=new Set();
   for(const ch of b.chapters){need(typeof ch.id==='string'&&ch.id&&!anchors.has(ch.id),'duplicate/missing chapter ID');anchors.add(ch.id);need(typeof ch.title==='string'&&Array.isArray(ch.blocks)&&ch.blocks.length>0,'empty chapter');
     for(const block of ch.blocks){need(typeof block.id==='string'&&block.id&&!anchors.has(block.id),'duplicate/missing block ID');anchors.add(block.id);need(['p','quote','image'].includes(block.type),'unsupported block');if(block.type!=='image')need(typeof block.text==='string','invalid text');}
   }
   for(const q of b.questions||[])need(Number.isInteger(q.answer)&&q.answer>=0&&q.answer<q.choices?.length,'invalid quiz answer');
 }return c;
}
export function preserveCatalog(previous,next,retired=[]){
 validateCatalog(next);const permitted=new Set(retired);const map=new Map(next.books.map(b=>[b.id,b]));
 for(const b of previous?.books||[]){const n=map.get(b.id);need(n||permitted.has(b.id),'existing book removed: '+b.id);if(!n)continue;
   const anchors=new Set(n.chapters.flatMap(c=>[c.id,...c.blocks.map(x=>x.id)]));
   for(const id of b.chapters.flatMap(c=>[c.id,...c.blocks.map(x=>x.id)]))need(anchors.has(id)||permitted.has(id),'existing annotation anchor removed: '+id);
 }return true;
}
export function makeManifest(dist,{sequence,sourceSha}){
 const assets={};for(const f of walk(dist).sort()){const p=path.relative(dist,f).replaceAll('\\','/');if(!safePath(p)||p.startsWith('_')||p==='bootstrap-manifest.json')continue;const b=fs.readFileSync(f);assets[p]={sha256:sha(b),bytes:b.length};}
 const version=JSON.parse(fs.readFileSync(path.join(dist,'version.json'))),spec=JSON.parse(fs.readFileSync(path.join(dist,'engine-spec.json')));
 return validateManifest({schema:1,kind:'chatbook-live-release',shell:1,syncSchema:1,sequence,sourceSha,releaseId:version.releaseId,specDigest:spec.contentDigest,assets});
}
export async function prepare(dist='dist'){
 await verifyRelease(dist);const catalog=validateCatalog(JSON.parse(fs.readFileSync(path.join(dist,'content/catalog.json'))));
 const sandbox={window:{}};vm.runInNewContext(fs.readFileSync(path.join(dist,'library.js'),'utf8'),sandbox,{timeout:2000});
 need(JSON.stringify(sandbox.window.CHATBOOK_CATALOG)===JSON.stringify(catalog),'embedded library/catalog mismatch');
 if(fs.existsSync('live/assets/content/catalog.json')){const retired=fs.existsSync('live/retired-anchors.json')?JSON.parse(fs.readFileSync('live/retired-anchors.json')):[];preserveCatalog(JSON.parse(fs.readFileSync('live/assets/content/catalog.json')),catalog,retired);}
 const old=fs.existsSync('live/channel.json')?JSON.parse(fs.readFileSync('live/channel.json')):null;
 const version=JSON.parse(fs.readFileSync(path.join(dist,'version.json')));
 const sequence=Math.max(Date.now(),(old?.sequence||0)+1);
 const manifest=makeManifest(dist,{sequence,sourceSha:version.sourceSha});
 fs.mkdirSync('live',{recursive:true});fs.rmSync('live/assets',{recursive:true,force:true});fs.mkdirSync('live/assets');
 for(const p of Object.keys(manifest.assets)){const target=path.join('live/assets',p);fs.mkdirSync(path.dirname(target),{recursive:true});fs.copyFileSync(path.join(dist,p),target);}
 fs.writeFileSync('live/release.json',JSON.stringify(manifest,null,2)+'\n');
 return {manifest,hash:sha(fs.readFileSync('live/release.json'))};
}
if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url))await prepare(process.argv[2]||'dist');
