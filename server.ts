import express from 'express';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { FALLBACK_INDIA_M3U } from './src/data/fallbackPlaylist.ts';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const DEFAULT_PLAYLIST_URL = 'https://iptv-org.github.io/iptv/countries/in.m3u';
const CACHE_TTL_MS = 5 * 60 * 1000;

interface CachedPlaylist {
  url: string;
  raw: string;
  source: 'live' | 'fallback';
  fetchedAt: string;
  cachedAtMs: number;
}

const playlistCache = new Map<string, CachedPlaylist>();

async function fetchPlaylistWithFallback(url: string, forceRefresh: boolean): Promise<CachedPlaylist> {
  const now = Date.now();
  const existing = playlistCache.get(url);

  if (!forceRefresh && existing && now - existing.cachedAtMs < CACHE_TTL_MS) {
    return existing;
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 8000);

  try {
    const response = await fetch(url, {
      signal: controller.signal,
      headers: {
        'User-Agent': 'StreamCast-IPTV/1.0',
        Accept: 'audio/x-mpegurl, application/vnd.apple.mpegurl, text/plain, */*',
      },
    });

    if (!response.ok) {
      throw new Error(`Upstream playlist returned HTTP ${response.status}`);
    }

    const raw = await response.text();
    if (!raw.includes('#EXTINF')) {
      throw new Error('Playlist response did not contain valid M3U #EXTINF entries');
    }

    const entry: CachedPlaylist = {
      url,
      raw,
      source: 'live',
      fetchedAt: new Date().toISOString(),
      cachedAtMs: now,
    };
    playlistCache.set(url, entry);
    return entry;
  } catch {
    if (existing && existing.source === 'live') {
      return existing;
    }

    const fallbackEntry: CachedPlaylist = {
      url,
      raw: FALLBACK_INDIA_M3U,
      source: 'fallback',
      fetchedAt: new Date().toISOString(),
      cachedAtMs: now,
    };
    playlistCache.set(url, fallbackEntry);
    return fallbackEntry;
  } finally {
    clearTimeout(timeout);
  }
}

async function startServer() {
  const app = express();
  app.use(express.json());

  app.get('/api/health', (_req, res) => {
    res.json({ status: 'ok', service: 'streamcast-server' });
  });

  app.get('/api/playlist', async (req, res) => {
    const requestedUrl =
      typeof req.query.url === 'string' && /^https?:\/\//i.test(req.query.url)
        ? req.query.url
        : DEFAULT_PLAYLIST_URL;
    const forceRefresh = req.query.refresh === '1';

    const result = await fetchPlaylistWithFallback(requestedUrl, forceRefresh);
    res.json({
      url: result.url,
      source: result.source,
      fetchedAt: result.fetchedAt,
      raw: result.raw,
    });
  });

  const isProd = process.env.NODE_ENV === 'production';

  if (!isProd) {
    const { createServer: createViteServer } = await import('vite');
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distDir = path.resolve(__dirname, 'dist');
    app.use(express.static(distDir));
    app.get('*', (_req, res) => {
      res.sendFile(path.join(distDir, 'index.html'));
    });
  }

  const port = Number(process.env.PORT) || 3000;
  app.listen(port, '0.0.0.0', () => {
    console.log(`StreamCast server listening on http://0.0.0.0:${port}`);
  });
}

startServer();
