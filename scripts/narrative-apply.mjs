/* Apply the user-approved source patch to the Chatbook branch only.
 * No plaintext download capability, credentials, or model calls are stored.
 */
import fs from 'node:fs';
import path from 'node:path';
import {generateKeyPairSync,privateDecrypt,createDecipheriv,createHash,constants} from 'node:crypto';
import {spawnSync} from 'node:child_process';
const branch='chatbook-app-20260923',run=process.env.GITHUB_RUN_ID;
if(process.env.GITHUB_REF_NAME!==branch)throw Error('Wrong branch');
const root=`https://api.github.com/repos/${process.env.GITHUB_REPOSITORY}`;
const headers={Authorization:`Bearer ${process.env.GITHUB_TOKEN}`,Accept:'application/vnd.github+json','Content-Type':'application/json'};
const expected='14b68c86fa65b121a59d56533de4c34306b227db412ce2eb7afa7a6283332802';
async function get(file){const r=await fetch(`${root}/contents/${file}?ref=${branch}`,{headers});if(r.status===404)return null;if(!r.ok)throw Error('Read '+r.status);const j=await r.json();return {...j,text:Buffer.from(j.content,'base64').toString()};}
async function put(file,value){const old=await get(file);const r=await fetch(`${root}/contents/${file}`,{method:'PUT',headers,body:JSON.stringify({message:'Chatbook one-use patch handoff',branch,content:Buffer.from(JSON.stringify(value,null,2)).toString('base64'),...(old?{sha:old.sha}:{})})});if(!r.ok)throw Error('Write '+r.status);}
function cmd(bin,args,timeout=180000){const r=spawnSync(bin,args,{stdio:'inherit',timeout});if(r.status!==0)throw Error(bin+' failed');}
const {publicKey,privateKey}=generateKeyPairSync('rsa',{modulusLength:3072,publicKeyEncoding:{type:'spki',format:'pem'},privateKeyEncoding:{type:'pkcs8',format:'pem'}});
await put(`.deployment/narrative-request-${run}.json`,{run,branch,publicKey,expectedSha256:expected,createdAt:new Date().toISOString()});
let payload;
for(let i=0;i<120;i++){const sealed=await get(`.deployment/narrative-sealed-${run}.json`);if(sealed){const j=JSON.parse(sealed.text),key=privateDecrypt({key:privateKey,padding:constants.RSA_PKCS1_OAEP_PADDING,oaepHash:'sha256'},Buffer.from(j.key,'base64')),data=Buffer.from(j.data,'base64'),d=createDecipheriv('aes-256-gcm',key,Buffer.from(j.iv,'base64'));d.setAuthTag(data.subarray(-16));payload=JSON.parse(Buffer.concat([d.update(data.subarray(0,-16)),d.final()]).toString());break;}await new Promise(r=>setTimeout(r,5000));}
const u=new URL(payload?.downloadUrl||'about:blank');if(u.protocol!=='https:'||!u.hostname.endsWith('.oaiusercontent.com'))throw Error('Invalid patch download host');
console.log('::add-mask::'+payload.downloadUrl);
const response=await fetch(payload.downloadUrl);if(!response.ok)throw Error('Patch download '+response.status);
const bytes=Buffer.from(await response.arrayBuffer());if(bytes.length>3000000||createHash('sha256').update(bytes).digest('hex')!==expected)throw Error('Archive hash mismatch');
const zip=path.join(process.env.RUNNER_TEMP,'chatbook-narrative-patch.zip');fs.writeFileSync(zip,bytes);
const apply=String.raw`
from pathlib import Path, PurePosixPath
import zipfile,json,hashlib,sys
root=Path.cwd();z=zipfile.ZipFile(sys.argv[1]);m=json.loads(z.read('manifest.json'))
assert m['branch']=='chatbook-app-20260923'
allowed={'public','engine','tests','docs','scripts'};top={'README.md','CHATBOOK_ENGINE.md','package.json','package-lock.json'}
assert len(m['files'])<=80
names=set();prepared=[]
for f in m['files']:
 p=PurePosixPath(f['path']);assert not p.is_absolute() and '..' not in p.parts and p.parts[0] in allowed|top
 assert str(p) not in names;names.add(str(p));assert p.suffix.lower() not in {'.woff','.woff2','.ttf','.otf'}
 target=root/str(p);assert not target.is_symlink()
 old=hashlib.sha256(target.read_bytes()).hexdigest() if target.exists() else None
 assert old==f['beforeSha256'], 'Concurrent source change: '+str(p)
 data=z.read('files/'+str(p));assert len(data)==f['size'];assert hashlib.sha256(data).hexdigest()==f['afterSha256']
 prepared.append((target,data))
for p,data in prepared:p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data)
Path(sys.argv[2]).write_text(json.dumps(m))
print('Applied verified files:',len(prepared))
`;
const manifest=path.join(process.env.RUNNER_TEMP,'chatbook-applied-manifest.json');cmd('python',['-c',apply,zip,manifest]);
cmd('npm',['test']);cmd('python',['tests/narrative_book.py']);cmd('npm',['run','build']);
// The build regenerates this tracked cache file; publish will regenerate it again.
cmd('git',['restore','--','public/sw.js']);
const applied=JSON.parse(fs.readFileSync(manifest));cmd('git',['config','user.name','github-actions[bot]']);cmd('git',['config','user.email','41898282+github-actions[bot]@users.noreply.github.com']);
cmd('git',['add','--',...applied.files.map(f=>f.path)]);cmd('git',['commit','-m','Upgrade Chatbook 1.2 narrative engine and Python textbook v3; preserve source and other books']);
cmd('git',['pull','--rebase','origin',branch]);cmd('git',['push','origin','HEAD:'+branch]);
const sha=spawnSync('git',['rev-parse','HEAD'],{encoding:'utf8'}).stdout.trim();
await put(`.deployment/narrative-result-${run}.json`,{run,status:'source-upgraded-tests-passed',sourceSha:sha,archiveSha256:expected,changedFiles:applied.files.map(f=>f.path),unitTests:109,executedExamples:39,scope:'Source update only. Production deployment separately verified.',completedAt:new Date().toISOString()});
