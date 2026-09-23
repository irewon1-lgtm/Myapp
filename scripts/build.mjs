import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const pub = path.join(root, 'public');
const pkg = JSON.parse(fs.readFileSync(path.join(root, 'package.json'), 'utf8'));

function walk(dir) {
  return fs
    .readdirSync(dir, { withFileTypes: true })
    .flatMap(entry =>
      entry.isDirectory()
        ? walk(path.join(dir, entry.name))
        : [path.join(dir, entry.name)]
    );
}

const paths = walk(pub)
  .map(file => './' + path.relative(pub, file).replaceAll('\\', '/'))
  .filter(file => !file.endsWith('sw.js') && !file.endsWith('LICENSE.txt'));
paths.push('./');

const sw = `const NAME='chatbook-offline-v1';const URLS=${JSON.stringify(paths)};self.addEventListener('install',e=>{e.waitUntil(caches.open(NAME).then(c=>c.addAll(URLS)).then(()=>self.skipWaiting()));});self.addEventListener('activate',e=>e.waitUntil(self.clients.claim()));self.addEventListener('fetch',e=>{if(e.request.method!=='GET')return;const u=new URL(e.request.url);if(u.origin!==location.origin||u.pathname.includes('/api/'))return;e.respondWith(fetch(e.request).then(r=>{if(r.ok){const clone=r.clone();caches.open(NAME).then(c=>c.put(e.request,clone));}return r;}).catch(async()=>await caches.match(e.request)||(e.request.mode==='navigate'?await caches.match('./index.html'):new Response('',{status:503}))));});self.addEventListener('notificationclick',e=>{e.notification.close();e.waitUntil(clients.openWindow(e.notification.data?.url||'./#review'));});`;

fs.writeFileSync(path.join(pub, 'sw.js'), sw);
fs.rmSync('dist', { recursive: true, force: true });
fs.cpSync(pub, 'dist', { recursive: true });

const version = {
  app: 'chatbook',
  version: pkg.version,
  sourceBranch: process.env.GITHUB_REF_NAME || 'local',
  sourceSha: process.env.GITHUB_SHA || 'local',
  runId: process.env.GITHUB_RUN_ID || null,
  builtAt: new Date().toISOString(),
};
fs.writeFileSync(
  path.join('dist', 'version.json'),
  JSON.stringify(version, null, 2) + '\n'
);

console.log(
  'Built static Chatbook:',
  paths.length,
  'files; source',
  version.sourceSha === 'local' ? 'local' : version.sourceSha.slice(0, 12)
);
