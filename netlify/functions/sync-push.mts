import type { Config } from '@netlify/functions';
import { jsonReply, pushSync } from '../lib/sync-core.mts';

export default async (request: Request) => {
  if (request.method !== 'PUT') {
    return jsonReply({ error: '허용되지 않은 요청입니다.' }, 405);
  }
  return pushSync(request);
};

export const config: Config = {
  path: '/api/sync/push',
  method: 'PUT',
};
