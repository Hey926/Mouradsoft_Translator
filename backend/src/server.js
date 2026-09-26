import { createApp } from './app.js';
import { OpenAITranslationProvider } from './provider.js';

export function configuration(env) {
  const enabled = env.LIVE_TRANSLATION_ENABLED === 'true';
  if (enabled && (env.CHILD_DATA_REVIEW_COMPLETED !== 'true' ||
      !env.OPENAI_API_KEY || env.OPENAI_API_KEY.startsWith('replace_') ||
      !env.OPENAI_MODEL || env.OPENAI_MODEL.startsWith('replace_'))) {
    throw new Error('Live translation requires a completed child-data review, a server API key, and a reviewed model. See docs/PRIVACY.md.');
  }
  const port = Number(env.PORT ?? 8787);
  if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('Invalid server port.');
  return { enabled, port, host: env.HOST ?? '127.0.0.1',
    provider: enabled ? new OpenAITranslationProvider({ apiKey: env.OPENAI_API_KEY, model: env.OPENAI_MODEL }) : undefined };
}

// Importable without opening a port, which keeps tests isolated and non-billable.
if (import.meta.main) {
  try {
    const config = configuration(process.env);
    const server = createApp(config);
    server.on('error', () => { console.error('Server could not start. Check the host and port.'); process.exitCode = 1; });
    server.listen(config.port, config.host, () => console.info(`Mouradsoft backend ready. Live translation ${config.enabled ? 'enabled' : 'disabled'}.`));
    for (const signal of ['SIGTERM', 'SIGINT']) process.on(signal, () => server.close());
  } catch {
    console.error('Configuration rejected. Complete the server setup described in README.md.'); process.exitCode = 1;
  }
}
