import { createServer } from 'node:http';
import { ServiceError, validateInput, validateOutput } from './validation.js';

// Bounded, short-lived counters only. No texts, translations or recordings are retained.
export class RateLimiter {
  constructor({ limit = 10, windowMs = 60000, maxEntries = 10000, now = Date.now } = {}) {
    Object.assign(this, { limit, windowMs, maxEntries, now }); this.entries = new Map();
  }
  prune() { const now = this.now(); for (const [key, entry] of this.entries) if (entry.until <= now) this.entries.delete(key); }
  accept(key) {
    this.prune();
    const current = this.entries.get(key);
    if (current) return ++current.count <= this.limit;
    if (this.entries.size >= this.maxEntries) return false;
    this.entries.set(key, { count: 1, until: this.now() + this.windowMs }); return true;
  }
}

function reply(res, status, payload) {
  if (res.destroyed || res.writableEnded) return;
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store',
    'X-Content-Type-Options': 'nosniff', ...(status === 429 ? { 'Retry-After': '60' } : {})
  });
  res.end(JSON.stringify(payload));
}

async function readBody(req) {
  if (!/^application\/json(?:\s*;|$)/i.test(req.headers['content-type'] ?? '')) throw new ServiceError('json_required', 415);
  if (Number(req.headers['content-length']) > 8192) throw new ServiceError('body_too_large', 413);
  const chunks = []; let bytes = 0;
  // Use data listeners, not async iteration: throwing must not destroy the socket before the 413 reply.
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => { cleanup(); req.resume(); reject(new ServiceError('request_timeout', 408)); }, 10000);
    function cleanup() { clearTimeout(timer); req.off('data', data); req.off('end', end); req.off('error', failed); req.off('aborted', aborted); }
    function data(chunk) {
      bytes += chunk.length;
      if (bytes > 8192) { cleanup(); req.resume(); reject(new ServiceError('body_too_large', 413)); }
      else chunks.push(chunk);
    }
    function end() {
      cleanup();
      try { resolve(JSON.parse(Buffer.concat(chunks).toString('utf8'))); }
      catch { reject(new ServiceError('invalid_json', 400)); }
    }
    function failed() { cleanup(); reject(new ServiceError('invalid_input', 400)); }
    function aborted() { cleanup(); reject(new ServiceError('aborted', 400)); }
    req.on('data', data); req.on('end', end); req.on('error', failed); req.on('aborted', aborted);
  });
}

export function createApp({ provider, enabled = false, limiter = new RateLimiter(),
    globalLimiter = new RateLimiter({ limit: 60 }), maxConcurrent = 4, timeoutMs = 27000 } = {}) {
  let active = 0;
  const server = createServer(async (req, res) => {
    if (req.method === 'GET' && req.url === '/health') return reply(res, 200, { status: 'ok', liveEnabled: enabled });
    if (req.method !== 'POST' || req.url !== '/v1/translate') return reply(res, 404, { error: 'not_found' });
    // Ignore forwarded headers: a caller must not choose their rate-limit identity.
    if (!limiter.accept(req.socket.remoteAddress ?? 'unknown') || !globalLimiter.accept('all')) {
      req.resume(); return reply(res, 429, { error: 'rate_limited' });
    }
    if (!enabled || !provider) { req.resume(); return reply(res, 503, { error: 'live_disabled' }); }
    if (active >= maxConcurrent) { req.resume(); return reply(res, 429, { error: 'busy' }); }
    active++;
    const controller = new AbortController();
    const closed = () => { if (!res.writableEnded) controller.abort(); };
    res.on('close', closed);
    let timer;
    try {
      const input = validateInput(await readBody(req));
      if (controller.signal.aborted) return;
      const timeout = new Promise((_, reject) => {
        timer = setTimeout(() => { controller.abort(); reject(new ServiceError('timeout', 504)); }, timeoutMs);
      });
      const result = await Promise.race([provider.translate(input, controller.signal), timeout]);
      reply(res, 200, validateOutput(result));
    } catch (error) {
      // Never reflect provider bodies, exception messages, user input, or secrets.
      reply(res, error instanceof ServiceError ? error.status : 502,
        { error: error instanceof ServiceError ? error.code : 'translation_failed' });
    } finally {
      clearTimeout(timer); res.off('close', closed); active--;
    }
  });
  server.requestTimeout = 15000;
  server.headersTimeout = 10000;
  server.keepAliveTimeout = 5000;
  server.maxHeadersCount = 30;
  const pruning = setInterval(() => { limiter.prune(); globalLimiter.prune(); }, 60000).unref();
  server.on('close', () => clearInterval(pruning));
  return server;
}
