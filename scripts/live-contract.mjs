import fs from 'node:fs';import path from 'node:path';import {sha} from '../netlify/lib/live-gateway.mjs';
const files=['netlify.toml','netlify/lib/sync-core.mts','netlify/lib/live-gateway.mjs',...fs.readdirSync('netlify/functions').map(p=>'netlify/functions/'+p),'public/live-guard.js','public/live-sw.js','public/manifest.webmanifest'];
const hashes=()=>Object.fromEntries(files.sort().map(p=>[p,sha(fs.readFileSync(p))]));
const target='live/shell-contract.json';
if(process.argv[2]==='record'){
 if(process.env.CHATBOOK_INITIAL_MIGRATION!=='1'||fs.existsSync(target))throw Error('Fixed shell contract already exists or initial migration not approved');
 fs.mkdirSync('live',{recursive:true});fs.writeFileSync(target,JSON.stringify({schema:1,site:'2c37965b-f193-4d0f-b371-652fdf1989c0',files:hashes()},null,2)+'\n');
}else{const old=JSON.parse(fs.readFileSync(target));if(JSON.stringify(old.files)!==JSON.stringify(hashes()))throw Error('Fixed server/boot code changed. Ordinary live publication is blocked; a separately tested bootstrap update is required.');console.log('Fixed shell, API and storage contract unchanged.');}
