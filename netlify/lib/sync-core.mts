import { getStore, getDeployStore } from '@netlify/blobs';
import { createHash } from 'node:crypto';

type SyncContext = {
  prefix: string;
  store: ReturnType<typeof getStore> | ReturnType<typeof getDeployStore>;
};

export function jsonReply(data: unknown, status = 200) {
  const headers = {
    'Content-Type': 'application/json; charset=utf-8',
    'Cache-Control': 'private, no-store',
    'X-Content-Type-Options': 'nosniff',
  };
  return new Response(JSON.stringify(data), { status, headers });
}

function resolveSyncContext(request: Request): SyncContext | Response {
  const url = new URL(request.url);
  const origin = request.headers.get('origin');
  if (origin && origin !== url.origin) {
    return jsonReply({ error: '다른 사이트에서의 요청은 허용하지 않습니다.' }, 403);
  }

  const bearer = request.headers
    .get('authorization')
    ?.match(/^Bearer ([A-Za-z0-9_-]{43})$/)?.[1];
  if (!bearer) {
    return jsonReply({ error: '서재 연결 코드가 필요합니다.' }, 401);
  }

  const prefix =
    createHash('sha256').update('chatbook-store:' + bearer).digest('hex') + '/';
  const prodHost =
    Netlify.env.get('CHATBOOK_PUBLIC_HOST') ||
    'chatbook-library-20260923.netlify.app';
  const store =
    url.hostname === prodHost
      ? getStore({ name: 'chatbook-sync-v1', consistency: 'strong' })
      : getDeployStore({ name: 'chatbook-sync-v1' });

  return { prefix, store };
}

export async function pullSync(request: Request) {
  const sync = resolveSyncContext(request);
  if (sync instanceof Response) return sync;
  try {
    const { blobs } = await sync.store.list({ prefix: sync.prefix });
    const items = await Promise.all(
      blobs.slice(0, 24).map(blob => sync.store.get(blob.key, { type: 'json' }))
    );
    return jsonReply({
      schema: 1,
      apiVersion: 2,
      items: items.filter(Boolean),
      devices: blobs.length,
    });
  } catch (error) {
    console.error(
      'chatbook_sync_pull_failed',
      error instanceof Error ? error.name : 'StorageError'
    );
    return jsonReply(
      { error: '동기화 서버가 응답하지 않습니다. 기기에 저장된 기록은 유지됩니다.' },
      503
    );
  }
}

export async function pushSync(request: Request) {
  const sync = resolveSyncContext(request);
  if (sync instanceof Response) return sync;
  try {
    const declared = Number(request.headers.get('content-length') || 0);
    if (declared > 2_800_000) {
      return jsonReply(
        { error: '서재 기록이 너무 큽니다. 2 MB 이하로 백업을 정리해 주세요.' },
        413
      );
    }

    const text = await request.text();
    if (text.length > 2_800_000) {
      return jsonReply({ error: '서재 기록 용량을 초과했습니다.' }, 413);
    }

    let body: any;
    try {
      body = JSON.parse(text);
    } catch {
      return jsonReply({ error: '요청 형식이 올바르지 않습니다.' }, 400);
    }

    const { device, payload } = body || {};
    const valid =
      typeof device === 'string' &&
      /^[a-f0-9-]{32,40}$/i.test(device) &&
      payload &&
      typeof payload.iv === 'string' &&
      /^[A-Za-z0-9_-]{16}$/.test(payload.iv) &&
      typeof payload.data === 'string' &&
      /^[A-Za-z0-9_-]{20,2800000}$/.test(payload.data);

    if (!valid) {
      return jsonReply({ error: '암호화된 기록 형식이 올바르지 않습니다.' }, 400);
    }

    const { blobs } = await sync.store.list({ prefix: sync.prefix });
    const key = sync.prefix + device;
    if (blobs.length >= 24 && !blobs.some(blob => blob.key === key)) {
      return jsonReply({ error: '연결 가능한 기기 수를 초과했습니다.' }, 409);
    }

    await sync.store.setJSON(key, { iv: payload.iv, data: payload.data });
    return jsonReply({
      ok: true,
      apiVersion: 2,
      savedAt: new Date().toISOString(),
    });
  } catch (error) {
    console.error(
      'chatbook_sync_push_failed',
      error instanceof Error ? error.name : 'StorageError'
    );
    return jsonReply(
      { error: '동기화 서버가 응답하지 않습니다. 기기에 저장된 기록은 유지됩니다.' },
      503
    );
  }
}

export async function resetSync(request: Request) {
  const sync = resolveSyncContext(request);
  if (sync instanceof Response) return sync;
  try {
    const { blobs } = await sync.store.list({ prefix: sync.prefix });
    await Promise.all(blobs.map(blob => sync.store.delete(blob.key)));
    return jsonReply({ ok: true, apiVersion: 2 });
  } catch (error) {
    console.error(
      'chatbook_sync_reset_failed',
      error instanceof Error ? error.name : 'StorageError'
    );
    return jsonReply(
      { error: '동기화 서버가 응답하지 않습니다. 기기에 저장된 기록은 유지됩니다.' },
      503
    );
  }
}
