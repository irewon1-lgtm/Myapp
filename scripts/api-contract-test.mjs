import fs from 'node:fs';

const read = path => fs.readFileSync(path, 'utf8');
const mustContain = (path, values) => {
  const text = read(path);
  for (const value of values) {
    if (!text.includes(value)) {
      throw new Error(path + ' missing contract: ' + value);
    }
  }
};

mustContain('netlify/functions/sync-pull.mts', [
  "path: '/api/sync/pull'",
  "method: 'GET'",
]);
mustContain('netlify/functions/sync-push.mts', [
  "path: '/api/sync/push'",
  "method: 'PUT'",
]);
mustContain('netlify/functions/sync-reset.mts', [
  "path: '/api/sync/reset'",
  "method: 'DELETE'",
]);
mustContain('netlify/functions/health.mts', [
  "path: '/api/health'",
  'apiVersion: 2',
]);
mustContain('netlify/functions/sync.mts', [
  "path: '/api/sync'",
  'pullSync',
  'pushSync',
  'resetSync',
]);
mustContain('public/core.js', [
  "C.api('sync/pull'",
  "C.api('sync/push'",
]);

console.log('Chatbook API contract: split routes + legacy compatibility OK');
