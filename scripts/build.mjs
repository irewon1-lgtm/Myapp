import fs from 'node:fs';
import path from 'node:path';
import {spawnSync} from 'node:child_process';
import {walk,sourceDigest,engineState,releaseId,sha256,createInfo,writeInfo,verifyRelease,serviceWorker} from './engine-info.mjs';

const root=process.cwd(),pub=path.join(root,'public');
const pkg=JSON.parse(fs.readFileSync(path.join(root,'package.json'),'utf8'));
// All supported publishing routes call this build. No skip-test switch is provided.
const engine=await engineState(pub),digest=sourceDigest(root);
const result=spawnSync('npm',['test'],{cwd:root,encoding:'utf8',timeout:180000,maxBuffer:16*1024*1024,env:{...process.env,CI:'true'}});
process.stdout.write(result.stdout||'');process.stderr.write(result.stderr||'');
if(result.error||result.status!==0)throw Error('Build stopped: npm test did not finish successfully.');
if(digest!==sourceDigest(root))throw Error('Build stopped: source changed during tests. Run a fresh build.');
const output=(result.stdout||'')+(result.stderr||'');
const count=label=>{const hits=[...output.matchAll(new RegExp('^# '+label+' (\\d+)\\s*$','gm'))];return hits.length?Number(hits.at(-1)[1]):null;};
const git=spawnSync('git',['rev-parse','HEAD'],{cwd:root,encoding:'utf8'});
const realSha=git.status===0&&/^[a-f0-9]{40,64}$/.test(git.stdout.trim())?git.stdout.trim():null;
const candidate=realSha||process.env.CHATBOOK_SOURCE_SHA||process.env.GITHUB_SHA||process.env.COMMIT_REF||'';
const sourceSha=/^[a-f0-9]{40,64}$/.test(candidate)?candidate:'unavailable';
const numeric=name=>/^\d+$/.test(process.env[name]||'')?process.env[name]:null;
const deployId=/^[a-f0-9]{24}$/.test(process.env.DEPLOY_ID||'')?process.env.DEPLOY_ID:null;
const version={app:'chatbook',version:pkg.version,engineVersion:engine.version,sourceBranch:'chatbook-app-20260923',sourceSha,sourceDigest:digest,runId:numeric('GITHUB_RUN_ID'),deployId,builtAt:new Date().toISOString()};
version.releaseId=releaseId(version);
const evidence={command:'npm test',status:'passed',exitCode:0,checkedAt:version.builtAt,sourceDigest:digest,nodeTests:count('tests'),nodePassed:count('pass'),nodeFailed:count('fail'),nodeSkipped:count('skipped'),outputSha256:sha256(output),scope:'Local syntax, API contract and Node engine tests only; not live video or production end-to-end verification.'};
const info=await createInfo(root,version,evidence);
const temp=fs.mkdtempSync(path.join(root,'.chatbook-build-'));
try {
  fs.cpSync(pub,temp,{recursive:true});
  const urls=walk(pub).map(f=>'./'+path.relative(pub,f).replaceAll('\\','/')).filter(f=>!f.endsWith('sw.js')&&!f.endsWith('LICENSE.txt'));urls.push('./');
  fs.writeFileSync(path.join(temp,'sw.js'),serviceWorker(digest,urls));
  fs.writeFileSync(path.join(temp,'version.json'),JSON.stringify(version,null,2)+'\n');
  writeInfo(temp,info);
  const verified=await verifyRelease(temp);
  fs.rmSync(path.join(root,'dist'),{recursive:true,force:true});
  fs.renameSync(temp,path.join(root,'dist'));
  console.log('Built and verified Chatbook release:',verified.releaseId,`(${verified.assetCount} file hashes)`);
} finally {fs.rmSync(temp,{recursive:true,force:true});}

// Frozen emergency package for the fixed live gateway; never part of the moving live asset map.
const {makeManifest}=await import('./live-package.mjs');
fs.writeFileSync(path.join(root,'dist/bootstrap-manifest.json'),JSON.stringify(makeManifest(path.join(root,'dist'),{sequence:1,sourceSha}),null,2)+'\n');
