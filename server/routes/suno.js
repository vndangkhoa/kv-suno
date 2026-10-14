import express from 'express';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { execFile } from 'child_process';
import { promisify } from 'util';
import { Readable } from 'stream';
import {
  getShowcase,
  getProfile,
  getSimilarClips,
  getClipInfo,
  resolveUrl,
  getCreatorsRanking,
  getSongsChart,
  refreshAllRankings,
  crawlVietnameseHotCreators,
  getCrawlerStatus
} from '../services/sunoApi.js';

const execFileAsync = promisify(execFile);
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const AUDIO_CACHE_DIR = path.resolve(__dirname, '../../.cache/audio');

if (!fs.existsSync(AUDIO_CACHE_DIR)) {
  fs.mkdirSync(AUDIO_CACHE_DIR, { recursive: true });
}

const inFlightDownloads = new Map();

function isValidAudioBuffer(buf) {
  if (!buf || buf.length < 8) return false;
  // MP4 container ftyp box (bytes 4-7: 0x66, 0x74, 0x79, 0x70)
  if (buf[4] === 0x66 && buf[5] === 0x74 && buf[6] === 0x79 && buf[7] === 0x70) return true;
  // MP3 ID3 header (bytes 0-2: 0x49, 0x44, 0x33)
  if (buf[0] === 0x49 && buf[1] === 0x44 && buf[2] === 0x33) return true;
  // MP3 frame sync (first 11 bits 1s: 0xFF followed by 0xE0 or higher)
  if (buf[0] === 0xFF && (buf[1] & 0xE0) === 0xE0) return true;
  // Ogg container (bytes 0-3: 0x4F, 0x67, 0x67, 0x53)
  if (buf[0] === 0x4F && buf[1] === 0x67 && buf[2] === 0x67 && buf[3] === 0x53) return true;
  // WAV container (bytes 0-3: 0x52, 0x49, 0x46, 0x46)
  if (buf[0] === 0x52 && buf[1] === 0x49 && buf[2] === 0x46 && buf[3] === 0x46) return true;
  return false;
}

async function extractAudioViaFfmpeg(cleanId, cacheFile) {
  const mp4Url = `https://cdn1.suno.ai/${cleanId}.mp4`;
  const tempFile = `${cacheFile}.tmp-${Date.now()}.m4a`;
  try {
    await execFileAsync('ffmpeg', [
      '-y',
      '-loglevel', 'error',
      '-i', mp4Url,
      '-vn',
      '-c:a', 'copy',
      '-movflags', '+faststart',
      tempFile
    ], { timeout: 35000 });

    if (fs.existsSync(tempFile) && fs.statSync(tempFile).size > 10000) {
      fs.renameSync(tempFile, cacheFile);
      console.log(`[audio-proxy] Extracted & cached Suno audio via FFmpeg: ${cleanId}.m4a (${fs.statSync(cacheFile).size} bytes)`);
      return cacheFile;
    }
  } catch (err) {
    console.warn(`[audio-proxy] FFmpeg extraction failed for ${cleanId}:`, err.message);
    if (fs.existsSync(tempFile)) {
      try { fs.unlinkSync(tempFile); } catch (e) {}
    }
  }
  return null;
}

async function getOrFetchAudio(cleanId, candidateUrls = []) {
  const cacheFile = path.join(AUDIO_CACHE_DIR, `${cleanId}.m4a`);
  if (fs.existsSync(cacheFile)) {
    try {
      const stat = fs.statSync(cacheFile);
      if (stat.size > 10000) return cacheFile;
    } catch (e) {}
  }

  if (inFlightDownloads.has(cleanId)) {
    return inFlightDownloads.get(cleanId);
  }

  const downloadPromise = (async () => {
    try {
      // 1. Primary engine: Extract pure AAC directly from Suno's official CDN mp4 (0 rate limits, instant)
      const ffmpegExtracted = await extractAudioViaFfmpeg(cleanId, cacheFile);
      if (ffmpegExtracted) {
        return ffmpegExtracted;
      }

      // 2. Secondary engine: Try candidate audio URLs if any
      for (const url of candidateUrls) {
        try {
          const resp = await fetch(url, {
            headers: {
              'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
              'Accept': '*/*'
            }
          });
          if (resp.ok) {
            const arrayBuf = await resp.arrayBuffer();
            const buf = Buffer.from(arrayBuf);
            if (isValidAudioBuffer(buf)) {
              const tempFile = `${cacheFile}.tmp-${Date.now()}.m4a`;
              fs.writeFileSync(tempFile, buf);
              fs.renameSync(tempFile, cacheFile);
              return cacheFile;
            } else {
              console.warn(`[audio-proxy] Candidate rejected (scrambled/invalid media): ${url}`);
            }
          }
        } catch (e) {
          console.warn(`[audio-proxy] Candidate fetch error (${url}):`, e.message);
        }
      }
      return null;
    } finally {
      inFlightDownloads.delete(cleanId);
    }
  })();

  inFlightDownloads.set(cleanId, downloadPromise);
  return downloadPromise;
}

const router = express.Router();

/**
 * GET /api/suno-profile
 * Query: ?handle=username&page=1
 * Used by SunoExplorerTab
 */
router.get('/suno-profile', async (req, res) => {
  try {
    const handle = req.query.handle || 'futurescall';
    const page = parseInt(req.query.page, 10) || 1;
    const data = await getProfile(handle, page);
    res.json(data);

    // Warm top clips in background
    if (data && Array.isArray(data.clips)) {
      setTimeout(() => {
        data.clips.slice(0, 6).forEach(c => {
          if (c.id) {
            getOrFetchAudio(c.id).catch(() => {});
          }
        });
      }, 100);
    }
  } catch (err) {
    console.error('[suno-profile] Error:', err.message);
    res.status(500).json({ error: err.message });
  }
});

/**
 * GET /api/suno-similar
 * Query: ?id=clipId
 * Used by SunoExplorerTab & SunoSfxDiscoverTab
 */
router.get('/suno-similar', async (req, res) => {
  try {
    const { id } = req.query;
    if (!id) return res.status(400).json({ error: 'Missing id' });
    const data = await getSimilarClips(id);
    res.json(data);
  } catch (err) {
    console.error('[suno-similar] Error:', err.message);
    res.status(500).json({ error: err.message });
  }
});

/**
 * GET /api/suno-oembed
 * Query: ?id=clipId
 * Used by SunoSfxDiscoverTab
 */
router.get('/suno-oembed', (req, res) => {
  try {
    const id = req.query.id || req.query.clipId;
    if (!id) return res.status(400).json({ error: 'clipId is required' });

    res.json({
      version: "1.0",
      type: "rich",
      provider_name: "Suno",
      provider_url: "https://suno.com",
      width: 600,
      height: 140,
      title: `Suno Track ${id.slice(0, 8)}`,
      html: `<iframe width='100%' height='140' style='border-radius: 12px' frameborder='0' allowfullscreen allow='autoplay; clipboard-write; encrypted-media; fullscreen; picture-in-picture' loading='lazy' src='https://suno.com/embed/${id}'></iframe>`,
      iframe_url: `https://suno.com/embed/${id}`
    });
  } catch (err) {
    console.error('[suno-oembed] Error:', err.message);
    res.status(500).json({ error: err.message });
  }
});

/**
 * GET /api/suno-showcase
 * Query: ?handle=futurescall&refresh=true
 */
router.get('/suno-showcase', async (req, res) => {
  try {
    const handle = req.query.handle || 'futurescall';
    const forceRefresh = req.query.refresh === 'true';
    const data = await getShowcase(handle, forceRefresh);
    res.json(data);

    // Warm top clips in background for instant playback
    if (data && Array.isArray(data.clips)) {
      setTimeout(() => {
        data.clips.slice(0, 8).forEach(c => {
          if (c.id) {
            getOrFetchAudio(c.id).catch(() => {});
          }
        });
      }, 100);
    }
  } catch (err) {
    console.error('[suno-showcase] Error:', err.message);
    res.status(500).json({ error: err.message });
  }
});

/**
 * GET /api/suno-clip-info
 * Query: ?songId=uuid
 */
router.get('/suno-clip-info', async (req, res) => {
  try {
    const { songId } = req.query;
    if (!songId) return res.status(400).json({ error: 'Missing songId' });
    const clip = await getClipInfo(songId);
    res.json(clip);
  } catch (err) {
    console.error('[suno-clip-info] Error:', err.message);
    res.status(500).json({ error: err.message });
  }
});

/**
 * GET /api/suno-audio-proxy
 * Query: ?input=uuid_or_url
 * Proxies streaming audio to bypass browser CORS with disk caching and range requests
 */
router.get('/suno-audio-proxy', async (req, res) => {
  try {
    let { input } = req.query;
    if (!input) return res.status(400).json({ error: 'Missing input parameter' });

    // Handle nested proxy urls or encoded urls
    while (typeof input === 'string' && input.includes('suno-audio-proxy?input=')) {
      const idx = input.indexOf('suno-audio-proxy?input=');
      input = decodeURIComponent(input.substring(idx + 'suno-audio-proxy?input='.length));
    }

    const uuidMatch = typeof input === 'string' ? input.match(/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i) : null;
    const cleanId = uuidMatch ? uuidMatch[0].toLowerCase() : null;

    if (cleanId) {
      const candidateUrls = [
        `https://suno-wiki.replit.app/api/suno-audio-proxy?input=${cleanId}`,
        `https://cdn1.suno.ai/${cleanId}.mp3`,
        `https://cdn1.suno.ai/${cleanId}.m4a`,
        `https://audiopipe.suno.ai/?item_id=${cleanId}`
      ];

      const cacheFile = await getOrFetchAudio(cleanId, candidateUrls);
      if (cacheFile && fs.existsSync(cacheFile)) {
        res.set({
          'Access-Control-Allow-Origin': '*',
          'Content-Type': 'audio/mp4',
          'Cache-Control': 'public, max-age=86400'
        });
        return res.sendFile(cacheFile, {
          acceptRanges: true,
          cacheControl: true,
          maxAge: '1d'
        });
      }

      // Direct fallback to Suno official CDN MP4 if extraction didn't complete
      const directMp4Url = `https://cdn1.suno.ai/${cleanId}.mp4`;
      try {
        const testHead = await fetch(directMp4Url, { method: 'HEAD' });
        if (testHead.ok) {
          return res.redirect(directMp4Url);
        }
      } catch (e) {}

      return res.status(404).json({ error: 'Could not fetch or decrypt audio from Suno CDN.' });
    }

    // Direct streaming fallback for non-UUID external URLs
    const targetUrl = typeof input === 'string' && input.startsWith('http') ? input : null;
    if (!targetUrl) {
      return res.status(400).json({ error: 'Invalid audio input parameter' });
    }

    const fetchHeaders = {
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
      'Accept': '*/*'
    };
    if (req.headers.range) {
      fetchHeaders['Range'] = req.headers.range;
    }

    const audioResp = await fetch(targetUrl, {
      method: 'GET',
      headers: fetchHeaders
    });

    if (!audioResp.ok && audioResp.status !== 206) {
      return res.status(audioResp.status).json({ error: 'Target audio URL request failed' });
    }

    let contentType = audioResp.headers.get('content-type') || 'audio/mp4';
    const resHeaders = {
      'Content-Type': contentType,
      'Accept-Ranges': 'bytes',
      'Cache-Control': 'public, max-age=86400',
      'Access-Control-Allow-Origin': '*'
    };
    if (audioResp.headers.get('content-length')) {
      resHeaders['Content-Length'] = audioResp.headers.get('content-length');
    }
    if (audioResp.headers.get('content-range')) {
      resHeaders['Content-Range'] = audioResp.headers.get('content-range');
    }

    res.status(audioResp.status === 206 ? 206 : 200).set(resHeaders);

    if (req.method === 'HEAD') {
      return res.end();
    }

    if (audioResp.body) {
      if (typeof audioResp.body.pipe === 'function') {
        audioResp.body.pipe(res);
      } else if (typeof Readable.fromWeb === 'function') {
        Readable.fromWeb(audioResp.body).pipe(res);
      } else {
        const buffer = await audioResp.arrayBuffer();
        res.send(Buffer.from(buffer));
      }
    } else {
      res.end();
    }
  } catch (err) {
    console.error('[suno-audio-proxy] Error:', err.message);
    if (!res.headersSent) {
      res.status(500).json({ error: 'Audio streaming proxy failure' });
    }
  }
});

/**
 * GET /api/download-suno-m4a
 * Query: ?id=uuid&title=filename
 */
router.get('/download-suno-m4a', async (req, res) => {
  try {
    const { id, title = 'track' } = req.query;
    if (!id) return res.status(400).json({ error: 'Missing id parameter' });

    const uuidMatch = typeof id === 'string' ? id.match(/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i) : null;
    const cleanId = uuidMatch ? uuidMatch[0].toLowerCase() : id;

    const candidateUrls = [
      `https://suno-wiki.replit.app/api/suno-audio-proxy?input=${cleanId}`,
      `https://cdn1.suno.ai/${cleanId}.mp3`,
      `https://cdn1.suno.ai/${cleanId}.m4a`,
      `https://audiopipe.suno.ai/?item_id=${cleanId}`
    ];

    const cacheFile = await getOrFetchAudio(cleanId, candidateUrls);
    if (!cacheFile || !fs.existsSync(cacheFile)) {
      return res.status(404).json({ error: 'Track not found or could not be decrypted' });
    }

    const safeTitle = `${(title || 'track').replace(/[/\\?%*:|"<>]/g, '_')}.m4a`;
    res.set('Access-Control-Allow-Origin', '*');
    res.download(cacheFile, safeTitle, (err) => {
      if (err && !res.headersSent) {
        console.error('[download-suno-m4a] Error:', err.message);
        res.status(500).json({ error: err.message });
      }
    });
  } catch (err) {
    console.error('[download-suno-m4a] Error:', err.message);
    res.status(500).json({ error: err.message });
  }
});

/**
 * GET /api/suno-resolve-url
 * Query: ?input=url
 */
router.get('/suno-resolve-url', async (req, res) => {
  try {
    const { input } = req.query;
    if (!input) return res.status(400).json({ error: 'Missing input' });
    const result = await resolveUrl(input);
    res.json(result);
  } catch (err) {
    console.error('[suno-resolve-url] Error:', err.message);
    res.status(500).json({ error: err.message });
  }
});

/**
 * GET /api/suno-creators-ranking
 * Query: ?sort=hot|plays|tracks&refresh=true
 */
router.get('/suno-creators-ranking', async (req, res) => {
  try {
    const sortBy = req.query.sort || 'hot';
    const forceRefresh = req.query.refresh === 'true';
    const data = await getCreatorsRanking(sortBy, { forceRefresh });
    res.json(data);

    // Pre-warm audio for #1 creator's top songs in background
    if (data?.creators?.[0]?.top_songs) {
      setTimeout(() => {
        data.creators[0].top_songs.slice(0, 3).forEach(s => {
          if (s.id) getOrFetchAudio(s.id).catch(() => {});
        });
      }, 150);
    }
  } catch (err) {
    console.error('[suno-creators-ranking] Error:', err.message);
    res.status(500).json({ error: err.message });
  }
});

/**
 * GET /api/suno-songs-chart
 * Query: ?sort=hot|plays|upvotes&creator=all&limit=60&vnOnly=false&refresh=true
 */
router.get('/suno-songs-chart', async (req, res) => {
  try {
    const sortBy = req.query.sort || 'hot';
    const creator = req.query.creator || 'all';
    const limit = parseInt(req.query.limit, 10) || 60;
    const vnOnly = req.query.vnOnly === 'true';
    const forceRefresh = req.query.refresh === 'true';
    const data = await getSongsChart({ sortBy, creator, limit, vnOnly, forceRefresh });
    res.json(data);

    // Pre-warm audio for top 5 chart tracks in background
    if (data && Array.isArray(data.songs)) {
      setTimeout(() => {
        data.songs.slice(0, 5).forEach(s => {
          if (s.id) getOrFetchAudio(s.id).catch(() => {});
        });
      }, 150);
    }
  } catch (err) {
    console.error('[suno-songs-chart] Error:', err.message);
    res.status(500).json({ error: err.message });
  }
});

/**
 * GET /api/suno-crawler-status
 * Returns live status of the background Vietnamese hot-hits crawler & ranking updater
 */
router.get('/suno-crawler-status', (req, res) => {
  res.json(getCrawlerStatus());
});

/**
 * POST /api/suno-crawl-vn-hits
 * Body: { handles?: string[], playlistIds?: string[], waitForCompletion?: boolean }
 * Triggers a crawl for Vietnamese hot-hit creators & updates rankings immediately
 */
router.post('/suno-crawl-vn-hits', async (req, res) => {
  try {
    const extraHandles = Array.isArray(req.body?.handles) ? req.body.handles : [];
    const playlistIds = Array.isArray(req.body?.playlistIds) ? req.body.playlistIds : [];
    const waitForCompletion = req.body?.waitForCompletion !== false;

    if (waitForCompletion) {
      await Promise.race([
        refreshAllRankings({ force: true, extraHandles, playlistIds }),
        new Promise(resolve => setTimeout(resolve, 4500))
      ]);
    } else {
      refreshAllRankings({ force: true, extraHandles, playlistIds }).catch(() => {});
    }

    const [ranking, chart] = await Promise.all([
      getCreatorsRanking('hot'),
      getSongsChart({ sortBy: 'hot', limit: 60 })
    ]);

    res.json({
      success: true,
      crawler_status: getCrawlerStatus(),
      creators_count: ranking.creators_count,
      total_plays: ranking.total_plays,
      total_songs: chart.total_songs,
      vietnamese_hot_hits_count: chart.vietnamese_hot_hits_count,
      creators: ranking.creators,
      top_songs: chart.songs.slice(0, 20)
    });
  } catch (err) {
    console.error('[suno-crawl-vn-hits] Error:', err.message);
    res.status(500).json({ error: err.message });
  }
});

export default router;

