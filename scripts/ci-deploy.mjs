/** Per-run sealed deployment handoff: no credential is committed to source. */
import fs from 'node:fs';
import {generateKeyPairSync,privateDecrypt,createDecipheriv,createHash,constants} from 'node:crypto';
import {spawnSync} from 'node:child_process';
const repo=process.env.GITHUB_REPOSITORY,branch=process.env.GITHUB_REF_NAME,run=process.env.GITHUB_RUN_ID;
if(branch!=='chatbook-app-20260923')throw Error('Refusing to touch any other branch');
const root=`https://api.github.com/repos/${repo}`,headers={Authorization:`Bearer ${process.env.GITHUB_TOKEN}`,Accept:'application/vnd.github+json','X-GitHub-Api-Version':'2022-11-28'};
const pause=ms=>new Promise(r=>setTimeout(r,ms));
async function read(path){const r=await fetch(`${root}/contents/${path}?ref=${encodeURIComponent(branch)}`,{headers});if(r.status===404)return null;if(!r.ok)throw Error(`GitHub read ${r.status}`);const j=await r.json();return {...j,text:Buffer.from(j.content,'base64').toString()};}
async function put(path,content,message){const old=await read(path);const r=await fetch(`${root}/contents/${path}`,{method:'PUT',headers:{...headers,'Content-Type':'application/json'},body:JSON.stringify({message,content:Buffer.from(content).toString('base64'),branch,...(old?{sha:old.sha}:{})})});if(!r.ok)throw Error(`GitHub write ${r.status}`);return r.json();}
function command(name,args,timeout=600000){const r=spawnSync(name,args,{stdio:'inherit',timeout,env:{...process.env,CI:'true'}});if(r.status!==0)throw Error(`${name} failed with status ${r.status}`);}
const {publicKey,privateKey}=generateKeyPairSync('rsa',{modulusLength:3072,publicKeyEncoding:{type:'spki',format:'pem'},privateKeyEncoding:{type:'pkcs8',format:'pem'}});
await put(`.deployment/request-${run}.json`,JSON.stringify({run,branch,siteId:'2c37965b-f193-4d0f-b371-652fdf1989c0',publicKey,createdAt:new Date().toISOString()},null,2),'Publish one-use deployment public key');
console.log('Awaiting encrypted one-use deployment input for run',run);
let proxy='',payload;
try{
 for(let i=0;i<120;i++){const sealed=await read(`.deployment/sealed-${run}.json`);if(sealed){const j=JSON.parse(sealed.text);if(j.run!==run)throw Error('Handoff run mismatch');const key=privateDecrypt({key:privateKey,padding:constants.RSA_PKCS1_OAEP_PADDING,oaepHash:'sha256'},Buffer.from(j.key,'base64'));const cipher=Buffer.from(j.data,'base64'),dec=createDecipheriv('aes-256-gcm',key,Buffer.from(j.iv,'base64'));dec.setAuthTag(cipher.subarray(-16));payload=JSON.parse(Buffer.concat([dec.update(cipher.subarray(0,-16)),dec.final()]).toString());break;}await pause(10000);}
 if(!payload)throw Error('Handoff expired');proxy=payload.proxy;
 if(!proxy.startsWith('https://netlify-mcp.netlify.app/proxy/'))throw Error('Unexpected deployment endpoint');
 console.log('::add-mask::'+proxy);console.log('::add-mask::'+payload.sourceUrl);
 const res=await fetch(payload.sourceUrl);if(!res.ok)throw Error(`Source retrieval ${res.status}`);const zip=Buffer.from(await res.arrayBuffer());if(createHash('sha256').update(zip).digest('hex')!==payload.sha256)throw Error('Source hash mismatch');
 const zipPath=process.env.RUNNER_TEMP+'/chatbook-source.zip';fs.writeFileSync(zipPath,zip);
 command('git',['pull','--ff-only','origin',branch]);
 command('python3',['-c',"import zipfile,pathlib,sys; z=zipfile.ZipFile(sys.argv[1]); root=pathlib.Path('.').resolve(); assert all((root/n.filename).resolve().is_relative_to(root) for n in z.infolist()); z.extractall('.')",zipPath]);
 command('npm',['install','--no-audit','--no-fund']);command('npm',['test']);command('npm',['run','build']);
 command('npx',['-y','@netlify/mcp@latest','--site-id','2c37965b-f193-4d0f-b371-652fdf1989c0','--proxy-path',proxy]);
 command('npm',['install','--no-save','--no-audit','--no-fund','playwright']);command('npx',['playwright','install','--with-deps','chromium']);
 command('node',['tests/browser-ci.mjs'],300000);
 command('git',['config','user.name','Chatbook Builder']);command('git',['config','user.email','chatbook-builder@users.noreply.github.com']);
 command('git',['pull','--ff-only','origin',branch]);command('git',['add','public','netlify','scripts','tests','package.json','package-lock.json','netlify.toml','.gitignore','index.html']);
 const status=spawnSync('git',['diff','--cached','--quiet']);if(status.status!==0){command('git',['commit','-m','Publish verified Chatbook source and browser results']);command('git',['push','origin',branch]);}
 await put(`.deployment/result-${run}.json`,JSON.stringify({run,status:'success',url:'https://chatbook-library-20260923.netlify.app',sourceSha256:payload.sha256,completedAt:new Date().toISOString()},null,2),'Record Chatbook deployment result');
}catch(error){const text=String(error.message).replaceAll(proxy||'__NO_SECRET__','[redacted]');if(fs.existsSync('tests/ci-report.json'))await put('tests/ci-report.json',fs.readFileSync('tests/ci-report.json','utf8'),'Record browser verification results');await put(`.deployment/result-${run}.json`,JSON.stringify({run,status:'error',error:text,completedAt:new Date().toISOString()},null,2),'Record Chatbook deployment failure');throw Error(text);}
