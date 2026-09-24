/** One approved installation of the fixed gateway. Not a normal publishing route. */
import {generateKeyPairSync,privateDecrypt,createDecipheriv,constants} from 'node:crypto';
import {spawnSync} from 'node:child_process';import fs from 'node:fs';
const repo=process.env.GITHUB_REPOSITORY,branch=process.env.GITHUB_REF_NAME,run=process.env.GITHUB_RUN_ID;
if(repo!=='irewon1-lgtm/Myapp'||branch!=='chatbook-app-20260923'||process.env.CHATBOOK_INITIAL_MIGRATION!=='1')throw Error('Initial migration authorization required');
const endpoint='https://api.github.com/repos/'+repo,headers={Authorization:'Bearer '+process.env.GITHUB_TOKEN,Accept:'application/vnd.github+json','X-GitHub-Api-Version':'2022-11-28'};
async function read(p){const r=await fetch(endpoint+'/contents/'+p+'?ref='+branch,{headers});if(r.status===404)return null;if(!r.ok)throw Error('GitHub read '+r.status);const x=await r.json();return{sha:x.sha,value:JSON.parse(Buffer.from(x.content,'base64').toString())};}
async function put(p,v,message){const old=await read(p);const r=await fetch(endpoint+'/contents/'+p,{method:'PUT',headers:{...headers,'Content-Type':'application/json'},body:JSON.stringify({branch,message,content:Buffer.from(JSON.stringify(v,null,2)+'\n').toString('base64'),...(old?{sha:old.sha}:{})})});if(!r.ok)throw Error('GitHub write '+r.status);}
const receiptPath='.deployment/live-bootstrap-receipt.json';if(await read(receiptPath))throw Error('Initial deployment already recorded. Refusing another production deployment.');
const channel=JSON.parse(fs.readFileSync('live/channel.json')),manifest=JSON.parse(fs.readFileSync('live/release.json'));
const {publicKey,privateKey}=generateKeyPairSync('rsa',{modulusLength:3072,publicKeyEncoding:{type:'spki',format:'pem'},privateKeyEncoding:{type:'pkcs8',format:'pem'}});
await put('.deployment/current-request.json',{kind:'chatbook-live-one-time-bootstrap',run,branch,siteId:'2c37965b-f193-4d0f-b371-652fdf1989c0',publicKey,liveCommit:channel.current.commit,manifestSha:channel.current.manifestSha,createdAt:new Date().toISOString()},'Request one approved live-gateway installation; no deployment performed yet');
console.log('BROWSER_CHECKS_PASSED_WAITING_FOR_ONE_TIME_NETLIFY_HANDOFF');
let payload;for(let i=0;i<180;i++){const s=await read('.deployment/current-sealed.json');if(String(s?.value?.run)===run){const j=s.value;const key=privateDecrypt({key:privateKey,padding:constants.RSA_PKCS1_OAEP_PADDING,oaepHash:'sha256'},Buffer.from(j.key,'base64'));const data=Buffer.from(j.data,'base64'),dec=createDecipheriv('aes-256-gcm',key,Buffer.from(j.iv,'base64'));dec.setAuthTag(data.subarray(-16));payload=JSON.parse(Buffer.concat([dec.update(data.subarray(0,-16)),dec.final()]).toString());break;}await new Promise(r=>setTimeout(r,5000));}
if(!payload?.proxy)throw Error('No deployment handoff; existing production was not modified.');const u=new URL(payload.proxy);if(u.protocol!=='https:'||u.hostname!=='netlify-mcp.netlify.app'||!u.pathname.startsWith('/proxy/'))throw Error('Unexpected deploy handoff');
console.log('::add-mask::'+payload.proxy);
function cmd(name,args){const r=spawnSync(name,args,{stdio:'inherit',timeout:600000,env:{...process.env,CI:'true'}});if(r.status!==0)throw Error(name+' exited '+r.status);}
cmd('git',['fetch','--no-tags','origin',branch]);cmd('git',['diff','--quiet','HEAD','FETCH_HEAD','--','public','netlify','scripts','engine','tests','.github/workflows','package.json','package-lock.json','netlify.toml','index.html','AGENTS.md']);
cmd('node',['scripts/live-contract.mjs','check']);
// This is the only production call. The receipt prevents a retry from silently charging again.
cmd('npx',['-y','@netlify/mcp@latest','--site-id','2c37965b-f193-4d0f-b371-652fdf1989c0','--proxy-path',payload.proxy]);
const receipt={kind:'chatbook-live-bootstrap',status:'deployed-awaiting-https-check',run,liveCommit:channel.current.commit,productionCalls:1,createdAt:new Date().toISOString()};
await put(receiptPath,receipt,'Record one-time Netlify gateway deployment; prohibit duplicate deployment');
const base='https://chatbook-library-20260923.netlify.app';let checked=false;
for(let i=0;i<10;i++){try{const s=await fetch(base+'/engine-spec.json',{cache:'no-store',signal:AbortSignal.timeout(15000)}),v=await fetch(base+'/version.json',{cache:'no-store',signal:AbortSignal.timeout(15000)});if(!s.ok||!v.ok)throw Error();const spec=await s.json(),version=await v.json();if(spec.contentDigest!==manifest.specDigest||version.releaseId!==manifest.releaseId)throw Error();const r=await fetch(base+'/',{cache:'no-store',signal:AbortSignal.timeout(15000)});if(r.headers.get('x-chatbook-commit')!==channel.current.commit)throw Error();checked=true;break;}catch{await new Promise(r=>setTimeout(r,3000));}}
if(!checked)throw Error('Gateway deployed but current HTTPS identities not yet verified; do NOT redeploy automatically.');
receipt.status='https-identity-verified';receipt.verifiedAt=new Date().toISOString();await put(receiptPath,receipt,'Confirm live gateway and canonical authoring specification over HTTPS');console.log(JSON.stringify(receipt,null,2));
