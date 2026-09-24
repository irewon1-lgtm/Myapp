/** Run only after unit/build/browser checks. An atomic Git push publishes one immutable release. */
import fs from 'node:fs';import {spawnSync} from 'node:child_process';import {sha} from '../netlify/lib/live-gateway.mjs';
const branch='chatbook-app-20260923';
function cmd(args){const r=spawnSync('git',args,{encoding:'utf8'});if(r.status!==0)throw Error('git '+args.join(' ')+': '+r.stderr);return r.stdout.trim();}
const before=process.env.CHATBOOK_BASE_SHA||process.env.GITHUB_SHA;if(!/^[a-f0-9]{40}$/.test(before||''))throw Error('Missing verified base SHA');
cmd(['fetch','origin',branch]);if(cmd(['rev-parse','origin/'+branch])!==before)throw Error('Newer source exists; stale publication refused.');
const report=JSON.parse(fs.readFileSync('review/live-browser/report.json'));if(report.status!=='passed'||report.checks.some(x=>!x.passed))throw Error('Browser validation not passed');
const manifest=JSON.parse(fs.readFileSync('live/release.json'));const old=fs.existsSync('live/channel.json')?JSON.parse(fs.readFileSync('live/channel.json')):null;
if(old&&manifest.sequence<=old.sequence)throw Error('Non-monotonic publication sequence');
cmd(['config','user.name','github-actions[bot]']);cmd(['config','user.email','41898282+github-actions[bot]@users.noreply.github.com']);
cmd(['add','live/assets','live/release.json','review/live-browser/report.json']);
cmd(['commit','-m','Build tested immutable Chatbook live bundle (no Netlify deploy)']);
const commit=cmd(['rev-parse','HEAD']);const channel={schema:1,kind:'chatbook-live-channel',sequence:manifest.sequence,current:{commit,manifestSha:sha(fs.readFileSync('live/release.json'))},previous:[old?.current,...(old?.previous||[])].filter(Boolean).slice(0,2)};
fs.writeFileSync('live/channel.json',JSON.stringify(channel,null,2)+'\n');cmd(['add','live/channel.json']);cmd(['commit','-m','Promote verified Chatbook books UI and authoring specification']);
cmd(['push','origin','HEAD:'+branch]);console.log(JSON.stringify({status:'published',netlifyDeploys:0,channel},null,2));
