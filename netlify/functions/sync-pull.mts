import type { Config } from '@netlify/functions';
import { jsonReply, pullSync } from '../lib/sync-core.mts';

export default async (request: Request) => {
  if (request.method !== 'GET') {
    return jsonReply({ error: '허용되지 않은 요청입니다.' }, 405);
  }
  return pullSync(request);
};

export const config: Config = {
  path: '/api/sync/pull',
  method: 'GET',
};
