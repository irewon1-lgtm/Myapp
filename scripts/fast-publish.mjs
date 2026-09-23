import {generateKeyPairSync,privateDecrypt,createDecipheriv,constants} from 'node:crypto';
import {spawnSync} from 'node:child_process';

const repo=process.env.GITHUB_REPOSITORY, branch=process.env.GITHUB_REF_NAME, run=process.env.GITHUB_RUN_ID;
if(branch!=='chatbook-app-20260923') throw Error('wrong branch');
const root=`https://api.github.com/repos/${repo}`;
const headers={Authorization:`Bearer ${process.env.GITHUB_TOKEN}`,Accept:'application/vnd.github+json','X-GitHub-Api-Version':'2022-11-28'};
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function read(path){
  const r=await fetch(`${root}/contents/${path}?ref=${encodeURIComponent(branch)}`,{headers});
  if(r.status===404)return null;
  if(!r.ok)throw Error('read '+r.status);
  const j=await r.json();
  return {...j,text:Buffer.from(j.content,'base64').toString()};
}
async function put(path,content,message){
  const old=await read(path);
  const r=await fetch(`${root}/contents/${path}`,{method:'PUT',headers:{...headers,'Content-Type':'application/json'},body:JSON.stringify({message,content:Buffer.from(content).toString('base64'),branch,...(old?{sha:old.sha}:{})})});
  if(!r.ok)throw Error('write '+r.status);
}
function cmd(name,args,timeout=600000){
  const r=spawnSync(name,args,{stdio:'inherit',timeout,env:{...process.env,CI:'true'}});
  if(r.status!==0)throw Error(name+' failed '+r.status);
}
const {publicKey,privateKey}=generateKeyPairSync('rsa',{modulusLength:3072,publicKeyEncoding:{type:'spki',format:'pem'},privateKeyEncoding:{type:'pkcs8',format:'pem'}});
await put(`.deployment/fast-request-${run}.json`,JSON.stringify({run,branch,publicKey,createdAt:new Date().toISOString()},null,2),'Open one-use fast deploy handoff');
let payload;
for(let i=0;i<60;i++){
  const sealed=await read(`.deployment/fast-sealed-${run}.json`);
  if(sealed){
    const j=JSON.parse(sealed.text);
    const key=privateDecrypt({key:privateKey,padding:constants.RSA_PKCS1_OAEP_PADDING,oaepHash:'sha256'},Buffer.from(j.key,'base64'));
    const data=Buffer.from(j.data,'base64');
    const dec=createDecipheriv('aes-256-gcm',key,Buffer.from(j.iv,'base64'));
    dec.setAuthTag(data.subarray(-16));
    payload=JSON.parse(Buffer.concat([dec.update(data.subarray(0,-16)),dec.final()]).toString());
    break;
  }
  await sleep(5000);
}
if(!payload?.proxy?.startsWith('https://netlify-mcp.netlify.app/proxy/'))throw Error('deploy handoff missing');
console.log('::add-mask::'+payload.proxy);
cmd('npm',['install','--no-audit','--no-fund']);
cmd('npm',['test']);
cmd('npm',['run','build']);
cmd('npx',['-y','@netlify/mcp@latest','--site-id','2c37965b-f193-4d0f-b371-652fdf1989c0','--proxy-path',payload.proxy]);
const check=await fetch('https://chatbook-library-20260923.netlify.app/library.js?ts='+Date.now());
const txt=await check.text();
if(!check.ok||!txt.includes('window.CHATBOOK_CATALOG'))throw Error('published app catalog not visible');
const versionCheck=await fetch('https://chatbook-library-20260923.netlify.app/version.json?ts='+Date.now());
if(!versionCheck.ok)throw Error('published source manifest not visible');
const version=await versionCheck.json();
if(version.sourceSha!==process.env.GITHUB_SHA)throw Error('published source SHA mismatch');
await put(`.deployment/fast-result-${run}.json`,JSON.stringify({run,status:'success',url:'https://chatbook-library-20260923.netlify.app',sourceSha:process.env.GITHUB_SHA,completedAt:new Date().toISOString()},null,2),'Record fast Chatbook publish result');
