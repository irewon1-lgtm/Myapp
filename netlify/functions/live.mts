import type { Config } from '@netlify/functions';
import { createGateway } from '../lib/live-gateway.mjs';
function handler() {
  // Lazy module-local cache. No authentication state or user records are stored here.
  return createGateway();
}
let gateway: ReturnType<typeof createGateway> | undefined;
export default async (request: Request) => {
  gateway ??= handler();
  return gateway(request);
};
export const config: Config = {
  path: ['/', '/index.html', '/__cb/*', '/engine-spec', '/engine-spec/', '/engine-spec.json', '/engine-spec.html', '/engine-info', '/engine-info/', '/engine-info.json', '/engine-info.html', '/version.json', '/llms.txt', '/content/*', '/engine/*'],
  preferStatic: false,
};
