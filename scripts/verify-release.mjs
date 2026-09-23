import {verifyRelease} from './engine-info.mjs';
try { console.log(JSON.stringify(await verifyRelease(process.argv[2]||'dist'),null,2)); }
catch(error) { console.error(error.message); process.exitCode=1; }
