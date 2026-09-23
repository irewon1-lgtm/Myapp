import type { Config } from '@netlify/functions';
import { jsonReply, resetSync } from '../lib/sync-core.mts';

export default async (request: Request) => {
  if (request.method !== 'DELETE') {
    return jsonReply({ error: '허용되지 않은 요청입니다.' }, 405);
  }
  return resetSync(request);
};

export const config: Config = {
  path: '/api/sync/reset',
  method: 'DELETE',
};
