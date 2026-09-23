import { getStore, getDeployStore } from '@netlify/blobs';
import { createHash } from 'node:crypto';
import type { Context, Config } from '@netlify/functions';
/** Only encrypted per-device snapshots are stored. A random 256-bit client key never leaves the browser. */
export default async (request: Request, context: Context) => {
  const headers = {'Content-Type':'application/json; charset=utf-8','Cache-Control':'private, no-store','X-Content-Type-Options':'nosniff'};
  const reply = (data: unknown, status = 200) => new Response(JSON.stringify(data), {status,headers});
  if (!['GET','PUT','DELETE'].includes(request.method)) return reply({error:'허용되지 않은 요청입니다.'},405);
  const url = new URL(request.url), origin = request.headers.get('origin');
  if (origin && origin !== url.origin) return reply({error:'다른 사이트에서의 요청은 허용하지 않습니다.'},403);
  const bearer = request.headers.get('authorization')?.match(/^Bearer ([A-Za-z0-9_-]{43})$/)?.[1];
  if (!bearer) return reply({error:'서재 연결 코드가 필요합니다.'},401);
  const prefix = createHash('sha256').update('chatbook-store:'+bearer).digest('hex')+'/';
  const prodHost = Netlify.env.get('CHATBOOK_PUBLIC_HOST') || 'chatbook-library-20260923.netlify.app';
  const store = url.hostname === prodHost ? getStore({name:'chatbook-sync-v1',consistency:'strong'}) : getDeployStore({name:'chatbook-sync-v1'});
  try {
    if (request.method === 'GET') {
      const {blobs} = await store.list({prefix});
      const items = await Promise.all(blobs.slice(0,24).map(b=>store.get(b.key,{type:'json'})));
      return reply({schema:1,items:items.filter(Boolean),devices:blobs.length});
    }
    if (request.method === 'DELETE') {
      const {blobs} = await store.list({prefix});
      await Promise.all(blobs.map(b=>store.delete(b.key)));
      return reply({ok:true});
    }
    const declared = Number(request.headers.get('content-length')||0);
    if (declared>2_800_000) return reply({error:'서재 기록이 너무 큽니다. 2 MB 이하로 백업을 정리해 주세요.'},413);
    const text = await request.text();
    if (text.length>2_800_000) return reply({error:'서재 기록 용량을 초과했습니다.'},413);
    let body: any;
    try { body=JSON.parse(text); } catch { return reply({error:'요청 형식이 올바르지 않습니다.'},400); }
    const {device,payload}=body||{};
    if(typeof device!=='string'||!/^[a-f0-9-]{32,40}$/i.test(device)||!payload||typeof payload.iv!=='string'||!/^[A-Za-z0-9_-]{16}$/.test(payload.iv)||typeof payload.data!=='string'||! /^[A-Za-z0-9_-]{20,2800000}$/.test(payload.data)) return reply({error:'암호화된 기록 형식이 올바르지 않습니다.'},400);
    const {blobs}=await store.list({prefix});
    const key=prefix+device;
    if(blobs.length>=24&&!blobs.some(b=>b.key===key))return reply({error:'연결 가능한 기기 수를 초과했습니다.'},409);
    await store.setJSON(key,{iv:payload.iv,data:payload.data});
    return reply({ok:true,savedAt:new Date().toISOString()});
  } catch(error) {
    console.error('chatbook_sync_failed',error instanceof Error?error.name:'StorageError');
    return reply({error:'동기화 서버가 응답하지 않습니다. 기기에 저장된 기록은 유지됩니다.'},503);
  }
};
export const config: Config = {path:'/api/sync'};
