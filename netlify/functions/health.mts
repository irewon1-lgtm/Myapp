import type { Config } from '@netlify/functions';

export default async (request: Request) => {
  if (request.method !== 'GET') {
    return new Response(
      JSON.stringify({ error: '허용되지 않은 요청입니다.' }),
      {
        status: 405,
        headers: {
          'Content-Type': 'application/json; charset=utf-8',
          'Cache-Control': 'no-store',
          'X-Content-Type-Options': 'nosniff',
        },
      }
    );
  }

  return new Response(
    JSON.stringify({
      ok: true,
      service: 'chatbook',
      apiVersion: 2,
      syncSchema: 1,
    }),
    {
      status: 200,
      headers: {
        'Content-Type': 'application/json; charset=utf-8',
        'Cache-Control': 'no-store',
        'X-Content-Type-Options': 'nosniff',
      },
    }
  );
};

export const config: Config = {
  path: '/api/health',
  method: 'GET',
};
