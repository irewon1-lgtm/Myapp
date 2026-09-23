import {generateKeyPairSync,privateDecrypt,createDecipheriv,constants} from 'node:crypto';
import {spawnSync} from 'node:child_process';
import fs from 'node:fs';
import {verifyRelease} from './engine-info.mjs';
const repo=process.env.GITHUB_REPOSITORY,branch=process.env.GITHUB_REF_NAME,run=process.env.GITHUB_RUN_ID;
if(branch!=='chatbook-app-20260923')throw Error('wrong branch');
const root=`https://api.github.com/repos/${repo}`;
const headers={Authorization:`Bearer ${process.env.GITHUB_TOKEN}`,Accept:'application/vnd.github+json','X-GitHub-Api-Version':'2022-11-28'};
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function read(p){const r=await fetch(`${root}/contents/${p}?ref=${encodeURIComponent(branch)}`,{headers});if(r.status===404)return null;if(!r.ok)throw Error('read '+r.status);const j=await r.json();return {...j,text:Buffer.from(j.content,'base64').toString()};}
async function put(p,content,message){const old=await read(p);const r=await fetch(`${root}/contents/${p}`,{method:'PUT',headers:{...headers,'Content-Type':'application/json'},body:JSON.stringify({message,content:Buffer.from(content).toString('base64'),branch,...(old?{sha:old.sha}:{})})});if(!r.ok)throw Error('write '+r.status);}
function cmd(name,args,timeout=600000){const r=spawnSync(name,args,{stdio:'inherit',timeout,env:{...process.env,CI:'true'}});if(r.status!==0)throw Error(name+' failed '+r.status);}
function currentSource(){
  cmd('git',['fetch','--no-tags','origin',branch],60000);
  // Handoff/result records are not app source. A delayed older source must never publish.
  cmd('git',['diff','--quiet','HEAD','FETCH_HEAD','--','public','netlify','scripts','engine','tests','.github/workflows','package.json','package-lock.json','netlify.toml','index.html','AGENTS.md','CHATBOOK_ENGINE.md'],60000);
}
currentSource();
cmd('npm',['ci','--no-audit','--no-fund']);
cmd('npm',['run','build']);
await verifyRelease('dist');
const localVersion=JSON.parse(fs.readFileSync('dist/version.json','utf8'));
const localSpec=JSON.parse(fs.readFileSync('dist/engine-spec.json','utf8'));
const {publicKey,privateKey}=generateKeyPairSync('rsa',{modulusLength:3072,publicKeyEncoding:{type:'spki',format:'pem'},privateKeyEncoding:{type:'pkcs8',format:'pem'}});
await put('.deployment/current-request.json',JSON.stringify({run,branch,publicKey,sourceDigest:localVersion.sourceDigest,createdAt:new Date().toISOString()},null,2),'Replace current one-use deployment request');
let payload;
for(let i=0;i<60;i++){
  const sealed=await read('.deployment/current-sealed.json');
  if(sealed){const j=JSON.parse(sealed.text);if(String(j.run)===run){
    const key=privateDecrypt({key:privateKey,padding:constants.RSA_PKCS1_OAEP_PADDING,oaepHash:'sha256'},Buffer.from(j.key,'base64'));
    const data=Buffer.from(j.data,'base64'),dec=createDecipheriv('aes-256-gcm',key,Buffer.from(j.iv,'base64'));
    dec.setAuthTag(data.subarray(-16));payload=JSON.parse(Buffer.concat([dec.update(data.subarray(0,-16)),dec.final()]).toString());break;
  }}
  await sleep(5000);
}
if(!payload?.proxy?.startsWith('https://netlify-mcp.netlify.app/proxy/'))throw Error('deploy handoff missing');
console.log('::add-mask::'+payload.proxy);
currentSource();
await verifyRelease('dist');
cmd('npx',['-y','@netlify/mcp@latest','--site-id','2c37965b-f193-4d0f-b371-652fdf1989c0','--proxy-path',payload.proxy]);
const base='https://chatbook-library-20260923.netlify.app';
async function publicJson(p){const r=await fetch(base+p+'?fresh='+Date.now(),{cache:'no-store'});if(!r.ok||!r.headers.get('content-type')?.includes('application/json'))throw Error('published JSON missing '+p);return r.json();}
const version=await publicJson('/version.json'),info=await publicJson('/engine-info.json'),spec=await publicJson('/engine-spec.json');
if(version.sourceDigest!==localVersion.sourceDigest||version.releaseId!==localVersion.releaseId||info.release.id!==version.releaseId||spec.release.id!==version.releaseId||spec.contentDigest!==localSpec.contentDigest)throw Error('published bundle identity mismatch');
const result={run,status:'success',url:base,sourceSha:localVersion.sourceSha,sourceDigest:localVersion.sourceDigest,releaseId:version.releaseId,engineVersion:info.engine.version,specDigest:spec.contentDigest,verificationScope:'Actual build tests and published bundle identity. Browser evidence is a subsequent workflow step.',completedAt:new Date().toISOString()};
fs.mkdirSync('tests/release-live-results',{recursive:true});fs.writeFileSync('tests/release-live-results/publication.json',JSON.stringify(result,null,2));
await put('.deployment/current-release.json',JSON.stringify(result,null,2),'Replace current verified publication record');
