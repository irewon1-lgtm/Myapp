import type { Config } from '@netlify/functions';
import {
  jsonReply,
  pullSync,
  pushSync,
  resetSync,
} from '../lib/sync-core.mts';

/**
 * Legacy compatibility route.
 * New clients use /api/sync/pull and /api/sync/push, while older clients
 * can keep using GET/PUT/DELETE on /api/sync without losing data.
 */
export default async (request: Request) => {
  if (request.method === 'GET') return pullSync(request);
  if (request.method === 'PUT') return pushSync(request);
  if (request.method === 'DELETE') return resetSync(request);
  return jsonReply({ error: '허용되지 않은 요청입니다.' }, 405);
};

export const config: Config = { path: '/api/sync' };
