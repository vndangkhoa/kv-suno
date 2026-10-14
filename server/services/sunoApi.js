import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// In-memory cache for Suno data
const cache = new Map();
const CACHE_TTL_MS = 3 * 60 * 60 * 1000; // 3 hours

const TOP_8_CREATORS = [
  'futurescall',
  'muzikvn',
  'daoxudeo',
  'aimusiccreater',
  'saigonmusic',
  'phamphungoc',
  'neomusiccafe',
  'vietmusic'
];

const CHART_CREATORS = [
  'futurescall',
  'muzikvn',
  'daoxudeo',
  'aimusiccreater',
  'saigonmusic',
  'phamphungoc',
  'neomusiccafe',
  'vietmusic',
  'rawenergy068',
  'luinight'
];

const CSV_PATH = path.resolve(__dirname, '../../vietnamese-suno-creators.csv');
const SNAPSHOT_PATH = path.resolve(__dirname, '../../.cache/suno-rankings-snapshot.json');

// Regex patterns for strictly identifying Vietnamese songs, lyrics, and genres (excludes French/Spanish shared accents like é/è/à)
const VN_EXCLUSIVE_CHAR_REGEX = /[ạảãầấậẩẫăằắặẳẵẹẻẽềếệểễịỉĩọỏõồốộổỗơờớợởỡụủũưừứựửữỳỵỷỹđ]/i;
const VN_STYLE_REGEX = /\b(v-?pop|vietnam|vietnamese|nhạc\s*việt|bolero|vinahouse|vina\s*house|trữ\s*tình|nhạc\s*trẻ|nhạc\s*vàng|dân\s*ca|quê\s*hương|đàn\s*tranh|sáo\s*trúc|đàn\s*bầu|đàn\s*nhị|xẩm|cải\s*lương|rap\s*việt|lofi\s*việt|sài\s*gòn|hà\s*nội|đà\s*lạt|sapa|miền\s*tây|nam\s*bộ|bắc\s*bộ)\b/i;
const VN_LYRIC_PHRASE_REGEX = /\b(tình\s*yêu|thương\s*nhớ|nỗi\s*buồn|cơn\s*mưa|đêm\s*khuya|người\s*thương|cuộc\s*đời|mùa\s*thu|nắng\s*chiều|khúc\s*ca|bài\s*hát|tiếng\s*đàn|giấc\s*mơ|kỷ\s*niệm|cô\s*đơn|hạnh\s*phúc)\b/i;

let cachedCsvCreators = null;

/**
 * Determine whether a Suno clip or playlist is genuinely Vietnamese based on title, tags, or lyrics
 */
export function isVietnameseSong(clip) {
  if (!clip) return false;
  const title = String(clip.title || clip.name || '');
  const tags = String(clip.metadata?.tags || clip.tags || clip.display_tags || clip.description || '');
  const prompt = String(clip.metadata?.prompt || clip.prompt || '');
  return (
    VN_STYLE_REGEX.test(tags) ||
    VN_STYLE_REGEX.test(title) ||
    VN_EXCLUSIVE_CHAR_REGEX.test(title) ||
    VN_EXCLUSIVE_CHAR_REGEX.test(tags) ||
    VN_EXCLUSIVE_CHAR_REGEX.test(prompt) ||
    VN_LYRIC_PHRASE_REGEX.test(prompt)
  );
}

/**
 * Determine whether a Suno clip qualifies as a Vietnamese Hot Hit
 */
export function isVietnameseHotHit(clip, { minPlays = 100, minUpvotes = 5, minHotScore = 250 } = {}) {
  if (!isVietnameseSong(clip)) return false;
  const plays = Number(clip.play_count) || 0;
  const upvotes = Number(clip.upvote_count) || 0;
  const hotScore = plays + upvotes * 25;
  return plays >= minPlays || upvotes >= minUpvotes || hotScore >= minHotScore;
}

/**
 * Load and parse Vietnamese Suno community creators from vietnamese-suno-creators.csv
 */
function loadCsvCreators(forceReload = false) {
  if (!forceReload && cachedCsvCreators && cachedCsvCreators.length > 0) {
    return cachedCsvCreators;
  }
  try {
    const raw = fs.readFileSync(CSV_PATH, 'utf-8');
    const lines = raw.split(/\r?\n/).map(l => l.trim()).filter(Boolean);
    const parsed = [];
    for (let i = 1; i < lines.length; i++) {
      const m = lines[i].match(/^([^,]+),([^,]+),(\d+),(\d+),"?([^"]*)"?$/);
      if (m) {
        parsed.push({
          username: m[1].trim(),
          display_name: m[2].trim(),
          tracks_count: parseInt(m[3], 10) || 0,
          sample_plays: parseInt(m[4], 10) || 0,
          style_focus: m[5].trim()
        });
      }
    }
    if (parsed.length > 0) {
      cachedCsvCreators = parsed;
      return parsed;
    }
  } catch (err) {
    console.warn('[sunoApi] Failed to read vietnamese-suno-creators.csv:', err.message);
  }
  return [];
}

/**
 * Persist updated creator metrics and newly discovered creators back to vietnamese-suno-creators.csv
 */
function saveCsvCreators(creatorsList = []) {
  if (!Array.isArray(creatorsList) || creatorsList.length === 0) return;
  try {
    const header = 'username,display_name,tracks_count,sample_plays,style_focus';
    const rows = creatorsList.map(c => {
      const username = String(c.username || c.handle || '').replace(/,/g, '').trim();
      const displayName = String(c.display_name || username).replace(/,/g, ' ').trim();
      const tracksCount = Math.max(0, parseInt(c.tracks_count || c.num_total_clips || 0, 10));
      const totalPlays = Math.max(0, parseInt(c.total_plays || c.sample_plays || 0, 10));
      const styleFocus = String(c.style_focus || (c.style_tags || []).join(', ') || 'V-Pop, Nhạc Việt')
        .replace(/"/g, '')
        .trim();
      return `${username},${displayName},${tracksCount},${totalPlays},"${styleFocus}"`;
    });
    fs.writeFileSync(CSV_PATH, [header, ...rows].join('\n') + '\n', 'utf-8');
    cachedCsvCreators = creatorsList.map(c => ({
      username: String(c.username || c.handle || '').trim(),
      display_name: String(c.display_name || c.username || '').trim(),
      tracks_count: Math.max(0, parseInt(c.tracks_count || c.num_total_clips || 0, 10)),
      sample_plays: Math.max(0, parseInt(c.total_plays || c.sample_plays || 0, 10)),
      style_focus: String(c.style_focus || (c.style_tags || []).join(', ') || 'V-Pop, Nhạc Việt').trim()
    }));
  } catch (err) {
    console.warn('[sunoApi] Failed to save vietnamese-suno-creators.csv:', err.message);
  }
}

/**
 * Hydrate in-memory profile cache from disk snapshot on startup for instant 0ms real rankings
 */
function hydrateCacheFromSnapshot() {
  try {
    if (!fs.existsSync(SNAPSHOT_PATH)) return;
    const raw = JSON.parse(fs.readFileSync(SNAPSHOT_PATH, 'utf-8'));
    if (raw && raw.profiles && typeof raw.profiles === 'object') {
      const now = Date.now();
      for (const [handle, profileData] of Object.entries(raw.profiles)) {
        if (profileData && Array.isArray(profileData.clips) && profileData.clips.length > 0) {
          cache.set(`profile:${handle.toLowerCase()}:p1`, {
            data: profileData,
            cachedAt: raw.updatedAtMs || now,
            expiresAt: now + CACHE_TTL_MS
          });
        }
      }
    }
  } catch (err) {
    console.warn('[sunoApi] Snapshot hydration skipped:', err.message);
  }
}

function saveCacheSnapshot(meta = {}) {
  try {
    const dir = path.dirname(SNAPSHOT_PATH);
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    const profiles = {};
    for (const [key, entry] of cache.entries()) {
      if (key.startsWith('profile:') && key.endsWith(':p1') && entry?.data && !entry.data._isFallback) {
        const handle = key.slice('profile:'.length, -3);
        profiles[handle] = {
          user_id: entry.data.user_id,
          display_name: entry.data.display_name,
          handle: entry.data.handle || handle,
          profile_description: entry.data.profile_description,
          num_total_clips: entry.data.num_total_clips,
          avatar_image_url: entry.data.avatar_image_url,
          is_verified: entry.data.is_verified,
          stats: entry.data.stats || {},
          playlists: (entry.data.playlists || []).slice(0, 6).map(p => ({
            id: p.id,
            name: p.name,
            description: p.description,
            upvote_count: p.upvote_count,
            play_count: p.play_count,
            num_total_results: p.num_total_results
          })),
          clips: (entry.data.clips || []).slice(0, 25)
        };
      }
    }
    fs.writeFileSync(
      SNAPSHOT_PATH,
      JSON.stringify({
        updatedAt: new Date().toISOString(),
        updatedAtMs: Date.now(),
        ...meta,
        profiles
      }),
      'utf-8'
    );
  } catch (err) {
    console.warn('[sunoApi] Failed to write rankings snapshot:', err.message);
  }
}

hydrateCacheFromSnapshot();

/**
 * Fetch Suno user profile and clips sorted by most listened (play_count)
 */
export async function getProfile(handle = 'futurescall', page = 1, forceRefresh = false) {
  let cleanHandle = String(handle || 'futurescall').trim();
  const match = cleanHandle.match(/suno\.com\/@([a-zA-Z0-9_.-]+)/i) || cleanHandle.match(/@([a-zA-Z0-9_.-]+)/);
  if (match) cleanHandle = match[1];
  cleanHandle = cleanHandle.split('?')[0].replace(/^@/, '').trim() || 'futurescall';

  const cacheKey = `profile:${cleanHandle.toLowerCase()}:p${page}`;
  const now = Date.now();

  if (!forceRefresh && cache.has(cacheKey)) {
    const entry = cache.get(cacheKey);
    if (now < entry.expiresAt && !entry.data?._isFallback) {
      return entry.data;
    }
  }

  // 1. Primary: Query Suno official public studio API directly sorted by play_count (most listened)
  try {
    const directUrl = `https://studio-api.prod.suno.com/api/profiles/${encodeURIComponent(cleanHandle)}?playlists_sort_by=upvote_count&clips_sort_by=play_count&page=${page}`;
    const directRes = await fetch(directUrl, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
      }
    });

    if (directRes.ok) {
      const data = await directRes.json();
      if (data && Array.isArray(data.clips)) {
        data.clips = data.clips
          .map(formatClip)
          .sort((a, b) => (b.play_count || 0) - (a.play_count || 0));
        cache.set(cacheKey, {
          data,
          cachedAt: now,
          expiresAt: now + CACHE_TTL_MS
        });
        return data;
      }
    }
  } catch (err) {
    console.warn(`[sunoApi] Direct Suno API profile fetch failed for ${cleanHandle}:`, err.message);
  }

  // 2. Secondary: Try querying upstream suno-wiki
  try {
    const upstreamUrl = `https://suno-wiki.replit.app/api/suno-profile?handle=${encodeURIComponent(cleanHandle)}&page=${page}`;
    const res = await fetch(upstreamUrl, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'
      }
    });

    if (res.ok) {
      const data = await res.json();
      if (data && Array.isArray(data.clips)) {
        data.clips = data.clips
          .map(formatClip)
          .sort((a, b) => (b.play_count || 0) - (a.play_count || 0));
        cache.set(cacheKey, {
          data,
          cachedAt: now,
          expiresAt: now + CACHE_TTL_MS
        });
        return data;
      }
    }
  } catch (err) {
    console.warn(`[sunoApi] Upstream profile fetch failed for ${cleanHandle}:`, err.message);
  }

  // If we already had a cached non-fallback entry, keep using it on network error
  if (cache.has(cacheKey) && !cache.get(cacheKey)?.data?._isFallback) {
    return cache.get(cacheKey).data;
  }

  // 3. Fallback to local showcase data
  const fallback = getFallbackShowcase(cleanHandle);
  cache.set(cacheKey, {
    data: fallback,
    cachedAt: now,
    expiresAt: now + CACHE_TTL_MS
  });
  return fallback;
}

/**
 * Fetch public showcase / clips for a user handle (e.g. futurescall)
 */
export async function getShowcase(handle = 'futurescall', forceRefresh = false) {
  const cacheKey = `showcase:${handle.toLowerCase()}`;
  const now = Date.now();

  if (!forceRefresh && cache.has(cacheKey)) {
    const entry = cache.get(cacheKey);
    if (now < entry.expiresAt) {
      const remainingMinutes = Math.max(1, Math.round((entry.expiresAt - now) / 60000));
      return {
        ...entry.data,
        _cached: true,
        _remainingMinutes: remainingMinutes,
        cachedAt: entry.cachedAt,
        expiresAt: entry.expiresAt
      };
    }
  }

  return await getProfile(handle, 1, forceRefresh);
}

/**
 * Fetch similar clips for a given song ID
 */
/**
 * Tokenize style tags into normalized keywords for DNA similarity scoring
 */
function extractDnaTokens(rawTags = '', title = '') {
  const combined = `${rawTags}, ${title}`.toLowerCase();
  const phraseList = combined
    .split(/[,;|/\n]+/)
    .map(s => s.trim())
    .filter(s => s.length >= 2 && s.length <= 36);
  const wordSet = new Set();
  combined
    .replace(/[^a-z0-9àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ\s-]/gi, ' ')
    .split(/\s+/)
    .map(w => w.trim())
    .filter(w => w.length >= 3 && !['the', 'and', 'for', 'with', 'from', 'song', 'music', 'track'].includes(w))
    .forEach(w => wordSet.add(w));
  return { phrases: phraseList, words: wordSet };
}

/**
 * Fetch similar clips for a given song ID using multi-creator Musical DNA matching
 */
export async function getSimilarClips(songId) {
  if (!songId) return { similar_clips: [] };
  const cleanId = String(songId).trim().toLowerCase();
  const cacheKey = `similar:v2:${cleanId}`;
  const now = Date.now();

  if (cache.has(cacheKey)) {
    const entry = cache.get(cacheKey);
    if (entry && entry.expiresAt && now < entry.expiresAt) {
      return entry.data;
    }
  }

  // 1. Resolve seed track info so we know its tags, genre, and creator handle
  let seedClip = null;
  for (const [k, v] of cache.entries()) {
    if (k.startsWith('profile:') && v?.data?.clips) {
      const found = v.data.clips.find(c => String(c.id).toLowerCase() === cleanId);
      if (found) {
        seedClip = found;
        break;
      }
    }
  }
  if (!seedClip) {
    try {
      seedClip = await getClipInfo(cleanId);
    } catch {}
  }

  const seedTagsRaw = seedClip?.metadata?.tags || seedClip?.tags || '';
  const seedTitle = seedClip?.title || '';
  const seedHandle = (seedClip?.handle || '').toLowerCase();
  const seedDna = extractDnaTokens(seedTagsRaw, seedTitle);

  // 2. Ensure we have a rich multi-creator candidate pool in cache
  const poolHandles = ['futurescall', 'muzikvn', 'saigonmusic', 'vietmusic', 'daoxudeo', 'neomusiccafe'];
  if (seedHandle && !poolHandles.includes(seedHandle)) {
    poolHandles.unshift(seedHandle);
  }

  const missingHandles = poolHandles.filter(h => !cache.has(`profile:${h}:p1`));
  if (missingHandles.length > 0) {
    await Promise.allSettled(
      missingHandles.slice(0, 4).map(h => getProfile(h, 1))
    );
  }

  // 3. Collect all unique candidate clips across cached profiles
  const candidateMap = new Map();
  for (const [k, v] of cache.entries()) {
    if (k.startsWith('profile:') && Array.isArray(v?.data?.clips)) {
      const profileHandle = v.data.handle || k.split(':')[1] || 'futurescall';
      for (const clip of v.data.clips) {
        if (!clip || !clip.id) continue;
        const cid = String(clip.id).toLowerCase();
        if (cid === cleanId || candidateMap.has(cid)) continue;
        candidateMap.set(cid, {
          ...formatClip(clip),
          handle: clip.handle || profileHandle
        });
      }
    }
  }

  // 4. Score every candidate clip by Musical DNA overlap + popularity
  const scored = [];
  for (const candidate of candidateMap.values()) {
    const candTagsRaw = candidate.metadata?.tags || candidate.tags || '';
    const candDna = extractDnaTokens(candTagsRaw, candidate.title);

    let overlapScore = 0;
    const matchedPhrases = [];

    for (const phrase of candDna.phrases) {
      if (seedDna.phrases.includes(phrase)) {
        overlapScore += 4;
        if (matchedPhrases.length < 3) matchedPhrases.push(phrase);
      } else {
        const phraseWords = phrase.split(/\s+/);
        if (phraseWords.some(w => seedDna.words.has(w))) {
          overlapScore += 2;
          if (matchedPhrases.length < 3) matchedPhrases.push(phrase);
        }
      }
    }

    for (const w of candDna.words) {
      if (seedDna.words.has(w)) {
        overlapScore += 1.2;
      }
    }

    // Same creator bonus (small, so we get cross-creator discovery too)
    if (seedHandle && String(candidate.handle || '').toLowerCase() === seedHandle) {
      overlapScore += 1.5;
    }

    // Popularity weight
    const popBonus = Math.min(3.5, Math.log10((candidate.play_count || 10) + 1));
    const totalScore = overlapScore + popBonus;

    // Normalized match percentage (85% - 98%)
    const matchPercent = Math.min(98, Math.max(85, Math.round(85 + Math.min(13, overlapScore * 1.4))));
    const displayTags = (matchedPhrases.length > 0 ? matchedPhrases : candDna.phrases.slice(0, 2))
      .map(t => t.replace(/\b\w/g, ch => ch.toUpperCase()))
      .slice(0, 2);

    scored.push({
      clip: {
        ...candidate,
        match_score: matchPercent,
        shared_tags: displayTags
      },
      totalScore,
      handle: String(candidate.handle || '').toLowerCase()
    });
  }

  scored.sort((a, b) => b.totalScore - a.totalScore);

  // 5. Select top 8 with creator diversity (max 2 per creator in first pass, then fill)
  const selected = [];
  const handleCounts = new Map();
  for (const item of scored) {
    if (selected.length >= 8) break;
    const hCount = handleCounts.get(item.handle) || 0;
    if (hCount < 2) {
      selected.push(item.clip);
      handleCounts.set(item.handle, hCount + 1);
    }
  }
  if (selected.length < 8) {
    const pickedIds = new Set(selected.map(c => c.id));
    for (const item of scored) {
      if (selected.length >= 8) break;
      if (!pickedIds.has(item.clip.id)) {
        selected.push(item.clip);
        pickedIds.add(item.clip.id);
      }
    }
  }

  const result = {
    seed_id: cleanId,
    seed_title: seedTitle || 'Selected Track',
    similar_clips: selected
  };

  if (selected.length > 0) {
    cache.set(cacheKey, {
      data: result,
      cachedAt: now,
      expiresAt: now + CACHE_TTL_MS
    });
  }

  return result;
}

/**
 * Clean promotional link headers/separators from Suno prompt lyrics while preserving all song lyrics & metatags
 */
export function cleanPromptLyrics(rawPrompt = '') {
  if (!rawPrompt || typeof rawPrompt !== 'string') return '';
  const lines = rawPrompt.split(/\r?\n/);
  const cleaned = [];
  let inPromoBlock = false;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();

    // Skip pure ASCII/Unicode separator lines (====, ****, ----, ⸻)
    if (/^[=*\-_~⸻—]{3,}$/.test(line)) {
      continue;
    }

    // Skip social/promo header lines (Youtube:, Muzik VN:, Soundcloud, http://, https://)
    if (
      /^(youtube|muzik\s*vn|muzik\s*ai|ai\s*muzik|soundcloud|tiktok|facebook|zalo|instagram|follow|subscribe)\b/i.test(line) ||
      /^https?:\/\/\S+$/i.test(line)
    ) {
      inPromoBlock = true;
      continue;
    }

    if (inPromoBlock && line === '') {
      continue;
    }
    inPromoBlock = false;
    cleaned.push(lines[i]);
  }

  return cleaned.join('\n').replace(/^\s+|\s+$/g, '');
}

/**
 * Fetch full metadata of a specific Suno song clip
 */
export async function getClipInfo(songId) {
  if (!songId) throw new Error('Missing songId');
  const cacheKey = `clip:${songId.toLowerCase()}`;
  if (cache.has(cacheKey)) {
    return cache.get(cacheKey);
  }

  // 1. Primary: Query Suno official public studio clip API directly for full lyrics & tags
  try {
    const directUrl = `https://studio-api.prod.suno.com/api/clip/${encodeURIComponent(songId)}`;
    const directRes = await fetch(directUrl, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
      }
    });
    if (directRes.ok) {
      const data = await directRes.json();
      if (data && data.id) {
        const formatted = formatClip(data);
        cache.set(cacheKey, formatted);
        return formatted;
      }
    }
  } catch (err) {
    console.warn(`[sunoApi] Direct Suno clip fetch failed for ${songId}:`, err.message);
  }

  // 2. Secondary: Upstream fallback
  try {
    const upstreamUrl = `https://suno-wiki.replit.app/api/suno-clip-info?songId=${encodeURIComponent(songId)}`;
    const res = await fetch(upstreamUrl, {
      headers: { 'User-Agent': 'Mozilla/5.0' }
    });

    if (res.ok) {
      const data = await res.json();
      const formatted = formatClip(data);
      cache.set(cacheKey, formatted);
      return formatted;
    }
  } catch (err) {
    console.warn(`[sunoApi] Error querying clip ${songId}:`, err.message);
  }

  return {
    id: songId,
    title: `Suno Track ${songId.slice(0, 8)}`,
    audio_url: `/api/suno-audio-proxy?input=${songId}`,
    image_url: `https://cdn1.suno.ai/image_${songId}.png`,
    tags: 'AI generated music, Suno Studio',
    metadata: {
      tags: 'AI generated music, Suno Studio',
      prompt: '[Intro]\n[Verse 1]\n[Chorus]\n[Outro]',
      type: 'gen',
      duration: 180
    }
  };
}

/**
 * Resolve Suno short links (e.g. https://suno.com/s/xyz) to UUID
 */
export async function resolveUrl(inputUrl) {
  const uuidMatch = inputUrl.match(/[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}/i);
  if (uuidMatch) {
    return {
      originalUrl: inputUrl,
      resolvedUrl: inputUrl,
      songId: uuidMatch[0].toLowerCase()
    };
  }

  if (inputUrl.includes('/s/') || inputUrl.includes('suno.com')) {
    try {
      const res = await fetch(inputUrl, { redirect: 'follow' });
      const finalUrl = res.url;
      const finalMatch = finalUrl.match(/[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}/i);
      return {
        originalUrl: inputUrl,
        resolvedUrl: finalUrl,
        songId: finalMatch ? finalMatch[0].toLowerCase() : null
      };
    } catch (e) {
      console.warn('[sunoApi] Failed to resolve URL redirect:', e.message);
    }
  }

  return {
    originalUrl: inputUrl,
    resolvedUrl: inputUrl,
    songId: null
  };
}

function formatClip(clip) {
  const id = clip.id || clip.clip_id || '';
  const rawMetadata = clip.metadata || {};
  const cleanedPrompt = cleanPromptLyrics(rawMetadata.prompt || clip.prompt || '');
  return {
    ...clip,
    id,
    title: (clip.title || 'Untitled Track').trim(),
    display_name: clip.display_name || 'Suno Artist',
    audio_url: `/api/suno-audio-proxy?input=${id}`,
    image_url: clip.image_url || (id ? `https://cdn1.suno.ai/image_${id}.png` : ''),
    tags: rawMetadata.tags || clip.tags || '',
    play_count: clip.play_count || 0,
    upvote_count: clip.upvote_count || 0,
    created_at: clip.created_at || new Date().toISOString(),
    status: clip.status || 'complete',
    metadata: {
      ...rawMetadata,
      prompt: cleanedPrompt || rawMetadata.prompt || ''
    }
  };
}

function getFallbackShowcase(handle) {
  const csvCreators = loadCsvCreators();
  const matched = csvCreators.find(
    c => c.username.toLowerCase() === String(handle || '').toLowerCase()
  );
  const isFuturescall = String(handle || 'futurescall').toLowerCase() === 'futurescall';
  return {
    user_id: "bf065270-325a-4086-85ac-5e82b7c4aa70",
    display_name: matched?.display_name || "TienNg",
    handle: handle,
    num_total_clips: matched?.tracks_count || 56,
    profile_description: matched
      ? `Suno AI Creator • ${matched.style_focus}`
      : "Suno Studio Showcase Creator",
    _isFallback: !isFuturescall,
    clips: [
      {
        id: "a15a3e62-b002-4e5b-a571-0d86adc503f2",
        title: "Duyên Lỡ .. Lỡ Duyên",
        display_name: matched?.display_name || "TienNg",
        handle: handle,
        audio_url: "/api/suno-audio-proxy?input=a15a3e62-b002-4e5b-a571-0d86adc503f2",
        image_url: "https://cdn2.suno.ai/746f5806-0e35-4ee5-94fd-e64d2f9a333b.jpeg",
        play_count: matched?.sample_plays || 567,
        upvote_count: 24,
        tags: matched?.style_focus || "Vietnamese folk trap, fast driving tempo, aggressive male rap, hard 808 sub bass, bamboo flute, dan tranh zither solo",
        metadata: {
          tags: matched?.style_focus || "Vietnamese folk trap, fast driving tempo, aggressive male rap, hard 808 sub bass, bamboo flute, dan tranh zither solo",
          prompt: "[INTRO]\n(Nhịp trống Trap dồn dập, tiếng đàn tranh rải mộc mạc)\nÂm dương... dương âm.\n\n[VERSE 1]\nĐêm mờ sương, người thương mờ mắt,\nTình dắt lối, tối lối ta đi.\n\n[CHORUS]\nDuyên âm không thành, đừng thành âm duyên.\nTình thực tan tành, đừng tành thực tình.",
          duration: 181.2
        }
      }
    ]
  };
}

// Verified real playable Suno seed clips for instant fallback before background profile fetch finishes
const VERIFIED_SAMPLE_SEEDS = [
  {
    id: "0242a69a-69c1-4fa1-b918-102eb1044e9b",
    image_url: "https://cdn2.suno.ai/c6bfd5c2-ed4a-4db5-a009-c800ac55aedc.jpeg",
    duration: 165
  },
  {
    id: "5f1e3166-50b1-4b81-9def-465bf04e61e6",
    image_url: "https://cdn2.suno.ai/image_5f1e3166-50b1-4b81-9def-465bf04e61e6.jpeg",
    duration: 182
  },
  {
    id: "a15a3e62-b002-4e5b-a571-0d86adc503f2",
    image_url: "https://cdn2.suno.ai/746f5806-0e35-4ee5-94fd-e64d2f9a333b.jpeg",
    duration: 181
  },
  {
    id: "4d8e56a4-5aa2-4740-8d7f-464bbc59c4cb",
    image_url: "https://cdn2.suno.ai/image_video_upload_bf486926-9865-4e2d-9719-8ce07347d2b9_snapshot_0s.jpeg",
    duration: 175
  },
  {
    id: "bbe0adf5-f3a3-43ba-ae75-f3587ba6328b",
    image_url: "https://cdn2.suno.ai/image_bbe0adf5-f3a3-43ba-ae75-f3587ba6328b.jpeg",
    duration: 210
  },
  {
    id: "10cfc44e-736b-48fe-a2a7-a5b8e925f0ba",
    image_url: "https://cdn2.suno.ai/10cfc44e-736b-48fe-a2a7-a5b8e925f0ba_348c378e.jpeg",
    duration: 213
  }
];

function buildFallbackTopSongs(csvCreator, idxOffset = 0) {
  const styleParts = (csvCreator.style_focus || 'V-Pop, Ballad')
    .split(',')
    .map(s => s.trim())
    .filter(Boolean);
  const primaryStyle = styleParts[0] || 'V-Pop';
  const secondaryStyle = styleParts[1] || primaryStyle;
  const basePlays = Math.max(50, csvCreator.sample_plays || 100);

  const sampleTitles = [
    `${primaryStyle} Đêm Khuya (${csvCreator.display_name} Mix)`,
    `Giai Điệu ${secondaryStyle}`,
    `Hoài Niệm ${primaryStyle}`
  ];
  const ratios = [0.52, 0.31, 0.17];

  return sampleTitles.map((title, i) => {
    const seed = VERIFIED_SAMPLE_SEEDS[(idxOffset + i) % VERIFIED_SAMPLE_SEEDS.length];
    const playCount = Math.max(15, Math.round(basePlays * ratios[i]));
    const upvoteCount = Math.max(2, Math.round(playCount * 0.04));
    return {
      id: seed.id,
      title,
      handle: csvCreator.username,
      display_name: csvCreator.display_name,
      audio_url: `/api/suno-audio-proxy?input=${seed.id}`,
      image_url: seed.image_url,
      play_count: playCount,
      upvote_count: upvoteCount,
      duration: seed.duration,
      tags: csvCreator.style_focus,
      hot_score: playCount + upvoteCount * 20,
      _isSample: true,
      metadata: {
        tags: csvCreator.style_focus,
        prompt: `[Intro - ${primaryStyle}]\n[Verse 1]\n[Chorus]\n[Outro]`,
        duration: seed.duration
      }
    };
  });
}

const RANKING_UPDATE_INTERVAL_MS = 30 * 60 * 1000; // Auto-crawl & update rankings every 30 minutes
const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms));

const crawlerState = {
  isCrawling: false,
  lastStartedAt: null,
  lastUpdatedAt: null,
  lastDurationMs: 0,
  totalCreators: 32,
  liveSyncedCreators: 0,
  totalIndexedSongs: 0,
  vietnameseHotHitsCount: 0,
  newlyDiscoveredCreators: [],
  crawlCount: 0,
  lastError: null
};

export function getCrawlerStatus() {
  return {
    ...crawlerState,
    nextUpdateIntervalMinutes: Math.round(RANKING_UPDATE_INTERVAL_MS / 60000)
  };
}

/**
 * Fetch clips from a public Suno playlist and merge Vietnamese clips into cache
 */
async function fetchPlaylistClips(playlistId, page = 1) {
  if (!playlistId) return [];
  const cleanId = String(playlistId).trim();
  const cacheKey = `playlist:${cleanId}:p${page}`;
  const now = Date.now();

  if (cache.has(cacheKey)) {
    const entry = cache.get(cacheKey);
    if (now < entry.expiresAt) return entry.data;
  }

  try {
    const url = `https://studio-api.prod.suno.com/api/playlist/${encodeURIComponent(cleanId)}/?page=${page}`;
    const res = await fetch(url, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
      }
    });
    if (res.ok) {
      const data = await res.json();
      const clips = (data?.playlist_clips || [])
        .map(item => item?.clip || item)
        .filter(c => c && c.id)
        .map(formatClip);
      cache.set(cacheKey, {
        data: clips,
        cachedAt: now,
        expiresAt: now + CACHE_TTL_MS
      });
      return clips;
    }
  } catch (err) {
    console.warn(`[sunoApi] Failed to fetch playlist ${cleanId}:`, err.message);
  }
  return [];
}

/**
 * Crawl Suno Discover feed, creator playlists, and seed handles to find Vietnamese hot-hit songs & creators
 */
export async function crawlVietnameseHotCreators({ extraHandles = [], playlistIds = [] } = {}) {
  const csvCreators = loadCsvCreators();
  const knownMap = new Map(csvCreators.map(c => [c.username.toLowerCase(), { ...c }]));
  const candidateHandles = new Set(
    extraHandles
      .map(h => String(h || '').replace(/^@/, '').split('?')[0].trim().toLowerCase())
      .filter(Boolean)
  );
  const newlyDiscovered = [];

  // 1. Query Suno Discover API (Trending, Featured Feed, New Songs, Trending Playlists)
  try {
    const discoverRes = await fetch('https://studio-api.prod.suno.com/api/discover/', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
      },
      body: JSON.stringify({ page: 1, page_size: 30 })
    });
    if (discoverRes.ok) {
      const discoverData = await discoverRes.json();
      for (const section of discoverData.sections || []) {
        for (const item of section.items || []) {
          // Direct clip items in discover sections
          if (item?.id && item?.handle && isVietnameseHotHit(item, { minPlays: 80, minUpvotes: 3 })) {
            const h = String(item.handle).replace(/^@/, '').toLowerCase();
            if (!knownMap.has(h)) candidateHandles.add(h);
          }
          // Nested sub-feed items (e.g. featured_feed_trending)
          if (Array.isArray(item?.items)) {
            for (const subClip of item.items) {
              if (subClip?.handle && isVietnameseHotHit(subClip, { minPlays: 80, minUpvotes: 3 })) {
                const h = String(subClip.handle).replace(/^@/, '').toLowerCase();
                if (!knownMap.has(h)) candidateHandles.add(h);
              }
            }
          }
          // Playlist items in discover whose title/description indicates Vietnamese music
          if (item?.entity_type === 'playlist_schema' && item?.id && isVietnameseSong(item)) {
            playlistIds.push(item.id);
          }
        }
      }
    }
  } catch (err) {
    console.warn('[Crawler] Discover feed scan warning:', err.message);
  }

  // 2. Scan Vietnamese playlists from existing cached creators + any explicit playlistIds
  const playlistsToScan = new Set(playlistIds.filter(Boolean));
  for (const [key, entry] of cache.entries()) {
    if (!key.startsWith('profile:') || !entry?.data || entry.data._isFallback) continue;
    for (const pl of entry.data.playlists || []) {
      if (!pl?.id) continue;
      // Crawl playlists explicitly focused on Vietnamese music
      if (isVietnameseSong(pl)) {
        playlistsToScan.add(pl.id);
      }
    }
  }

  for (const plId of Array.from(playlistsToScan).slice(0, 15)) {
    const plClips = await fetchPlaylistClips(plId, 1);
    for (const clip of plClips) {
      const clipHandle = String(clip.handle || '').replace(/^@/, '').trim().toLowerCase();
      if (!clipHandle) continue;

      // Merge Vietnamese playlist clips into the owner's cached profile so their Vietnamese hits surface
      const ownerCacheKey = `profile:${clipHandle}:p1`;
      if (cache.has(ownerCacheKey)) {
        const ownerEntry = cache.get(ownerCacheKey);
        if (ownerEntry?.data && Array.isArray(ownerEntry.data.clips)) {
          const existingIds = new Set(ownerEntry.data.clips.map(c => String(c.id).toLowerCase()));
          if (!existingIds.has(String(clip.id).toLowerCase())) {
            ownerEntry.data.clips.push(clip);
          }
        }
      }

      if (!knownMap.has(clipHandle) && isVietnameseHotHit(clip, { minPlays: 100, minUpvotes: 4 })) {
        candidateHandles.add(clipHandle);
      }
    }
  }

  // 3. Verify each candidate handle by crawling their live Suno profile
  for (const handle of candidateHandles) {
    if (knownMap.has(handle)) continue;
    try {
      const profile = await getProfile(handle, 1, true);
      if (!profile || profile._isFallback || !Array.isArray(profile.clips)) continue;

      const vnHotClips = profile.clips.filter(c =>
        isVietnameseHotHit(c, { minPlays: 100, minUpvotes: 4, minHotScore: 200 })
      );
      const vnClips = profile.clips.filter(c => isVietnameseSong(c));

      if (vnHotClips.length >= 2 || vnClips.length >= 3) {
        const bestClips = vnHotClips.length > 0 ? vnHotClips : vnClips;
        const maxClipPlays = Math.max(0, ...bestClips.map(c => Number(c.play_count) || 0));
        const totalProfilePlays = Math.max(
          Number(profile?.stats?.play_count__sum) || 0,
          profile.clips.reduce((s, c) => s + (Number(c.play_count) || 0), 0),
          maxClipPlays
        );

        const rawTags = bestClips[0]?.metadata?.tags || bestClips[0]?.tags || '';
        const inferredStyles = rawTags
          .split(',')
          .map(s => s.trim())
          .filter(s => s.length >= 2 && s.length <= 28)
          .slice(0, 3)
          .join(', ') || 'V-Pop, Nhạc Việt';

        const newCreator = {
          username: profile.handle || handle,
          display_name: profile.display_name || handle,
          tracks_count: Number(profile.num_total_clips) || profile.clips.length,
          sample_plays: totalProfilePlays,
          style_focus: inferredStyles
        };

        knownMap.set(handle, newCreator);
        newlyDiscovered.push(newCreator.username);
        console.log(`[Crawler] Discovered Vietnamese hot-hit creator: @${newCreator.username} (${totalProfilePlays} plays)`);
      }
    } catch (err) {
      console.warn(`[Crawler] Candidate verification failed for @${handle}:`, err.message);
    }
  }

  return {
    creators: Array.from(knownMap.values()),
    newlyDiscovered
  };
}

let backgroundWarmInProgress = false;
function triggerBackgroundProfileWarm(handles = []) {
  if (backgroundWarmInProgress || handles.length === 0) return;
  backgroundWarmInProgress = true;
  setTimeout(async () => {
    try {
      const uncached = handles.filter(h => {
        const entry = cache.get(`profile:${h.toLowerCase()}:p1`);
        return !entry || entry.data?._isFallback;
      });
      for (let i = 0; i < uncached.length; i += 4) {
        const batch = uncached.slice(i, i + 4);
        await Promise.allSettled(batch.map(h => getProfile(h, 1)));
        await sleep(250);
      }
      saveCacheSnapshot({ trigger: 'background-warm' });
    } catch (e) {
      // Ignore background errors
    } finally {
      backgroundWarmInProgress = false;
    }
  }, 50);
}

/**
 * Full background job: crawls all creators (plus discovers new Vietnamese hot-hit creators),
 * refreshes their live Suno profiles, updates song & creator rankings, and saves to CSV + disk snapshot.
 */
export async function refreshAllRankings({ force = true, extraHandles = [], playlistIds = [] } = {}) {
  if (crawlerState.isCrawling) {
    return getCrawlerStatus();
  }

  const startedAt = Date.now();
  crawlerState.isCrawling = true;
  crawlerState.lastStartedAt = new Date(startedAt).toISOString();
  crawlerState.lastError = null;

  try {
    const csvCreators = loadCsvCreators();

    // 1. Refresh all known creators in polite batches of 4
    const BATCH_SIZE = 4;
    for (let i = 0; i < csvCreators.length; i += BATCH_SIZE) {
      const batch = csvCreators.slice(i, i + BATCH_SIZE);
      await Promise.allSettled(
        batch.map(c => getProfile(c.username, 1, force))
      );
      if (i + BATCH_SIZE < csvCreators.length) {
        await sleep(300);
      }
    }

    // 2. Crawl Vietnamese playlists & Discover feed for new creators and hidden Vietnamese playlist hits
    const { creators: mergedCreators, newlyDiscovered } = await crawlVietnameseHotCreators({
      extraHandles,
      playlistIds
    });

    if (newlyDiscovered.length > 0) {
      crawlerState.newlyDiscoveredCreators = [
        ...new Set([...crawlerState.newlyDiscoveredCreators, ...newlyDiscovered])
      ];
      cachedCsvCreators = mergedCreators;
    }

    // 3. Recompute live rankings and songs chart
    const rankingResult = await getCreatorsRanking('hot');
    const chartResult = await getSongsChart({ sortBy: 'hot', limit: 200 });

    // 4. Persist updated live metrics back to vietnamese-suno-creators.csv & disk snapshot
    saveCsvCreators(rankingResult.creators);
    saveCacheSnapshot({
      trigger: 'scheduled-crawl',
      creatorsCount: rankingResult.creators_count,
      songsCount: chartResult.total_songs
    });

    const finishedAt = Date.now();
    crawlerState.lastUpdatedAt = new Date(finishedAt).toISOString();
    crawlerState.lastDurationMs = finishedAt - startedAt;
    crawlerState.totalCreators = rankingResult.creators_count;
    crawlerState.liveSyncedCreators = rankingResult.creators.filter(c => c.is_live).length;
    crawlerState.totalIndexedSongs = chartResult.total_songs;
    crawlerState.vietnameseHotHitsCount = chartResult.vietnamese_hot_hits_count || 0;
    crawlerState.crawlCount += 1;

    console.log(
      `[RankingUpdater] Completed crawl #${crawlerState.crawlCount} in ${crawlerState.lastDurationMs}ms: ` +
      `${crawlerState.liveSyncedCreators}/${crawlerState.totalCreators} live creators, ` +
      `${crawlerState.totalIndexedSongs} songs (${crawlerState.vietnameseHotHitsCount} VN hot hits).`
    );
  } catch (err) {
    crawlerState.lastError = err.message;
    console.error('[RankingUpdater] Crawl error:', err.message);
  } finally {
    crawlerState.isCrawling = false;
  }

  return getCrawlerStatus();
}

/**
 * Get ranked list of all Vietnamese Suno community creators with live metrics and top 3 songs
 * @param {'hot'|'plays'|'tracks'} sortBy
 * @param {{ forceRefresh?: boolean }} options
 */
export async function getCreatorsRanking(sortBy = 'hot', { forceRefresh = false } = {}) {
  const normalizedSort = ['hot', 'plays', 'tracks'].includes(sortBy) ? sortBy : 'hot';
  const csvCreators = loadCsvCreators();

  if (forceRefresh) {
    // Refresh top 8 immediately and trigger full background crawl for the rest
    await Promise.race([
      Promise.allSettled(TOP_8_CREATORS.map(u => getProfile(u, 1, true))),
      new Promise(resolve => setTimeout(resolve, 2200))
    ]);
    refreshAllRankings({ force: true }).catch(() => {});
  } else {
    // Pre-warm page 1 profiles for the top 8 creators if not yet cached
    const missingTop8 = TOP_8_CREATORS.filter(u => {
      const entry = cache.get(`profile:${u.toLowerCase()}:p1`);
      return !entry || entry.data?._isFallback;
    });

    if (missingTop8.length > 0) {
      await Promise.race([
        Promise.allSettled(missingTop8.map(u => getProfile(u, 1))),
        new Promise(resolve => setTimeout(resolve, 1300))
      ]);
    }
  }

  // Trigger background fetch for remaining uncached creators so cache fills up seamlessly
  const remainingHandles = csvCreators
    .map(c => c.username)
    .filter(u => {
      const entry = cache.get(`profile:${u.toLowerCase()}:p1`);
      return !entry || entry.data?._isFallback;
    });
  if (remainingHandles.length > 0) {
    triggerBackgroundProfileWarm(remainingHandles);
  }

  const enriched = csvCreators.map((csvCreator, idx) => {
    const cacheEntry = cache.get(`profile:${csvCreator.username.toLowerCase()}:p1`);
    const live = cacheEntry?.data && !cacheEntry.data._isFallback ? cacheEntry.data : null;
    const liveClips = Array.isArray(live?.clips) ? live.clips : [];

    let topSongs = [];
    if (liveClips.length > 0) {
      const sortedClips = [...liveClips].sort((a, b) => {
        if (normalizedSort === 'plays') {
          return (b.play_count || 0) - (a.play_count || 0);
        }
        const vnBonusB = isVietnameseSong(b) ? 150 : 0;
        const vnBonusA = isVietnameseSong(a) ? 150 : 0;
        const scoreB = (b.play_count || 0) + (b.upvote_count || 0) * 25 + vnBonusB;
        const scoreA = (a.play_count || 0) + (a.upvote_count || 0) * 25 + vnBonusA;
        return scoreB - scoreA;
      });

      topSongs = sortedClips.slice(0, 3).map(clip => {
        const formatted = formatClip(clip);
        const duration = Math.round(formatted.metadata?.duration || clip.duration || 180);
        const is_vietnamese = isVietnameseSong(formatted);
        return {
          ...formatted,
          handle: csvCreator.username,
          display_name: live?.display_name || csvCreator.display_name,
          avatar_image_url: live?.avatar_image_url || clip.avatar_image_url || '',
          duration,
          is_vietnamese,
          hot_score: (formatted.play_count || 0) + (formatted.upvote_count || 0) * 25 + (is_vietnamese ? 150 : 0)
        };
      });
    } else {
      topSongs = buildFallbackTopSongs(csvCreator, idx);
    }

    const clipsPlaySum = liveClips.reduce((sum, c) => sum + (Number(c.play_count) || 0), 0);
    const clipsUpvoteSum = liveClips.reduce((sum, c) => sum + (Number(c.upvote_count) || 0), 0);
    const profileStatsPlays = Number(live?.stats?.play_count__sum) || 0;
    const profileStatsUpvotes = Number(live?.stats?.upvote_count__sum) || 0;
    const followers_count = Number(live?.stats?.followers_count) || 0;

    const maxLiveClipPlays = liveClips.length > 0
      ? Math.max(...liveClips.map(c => Number(c.play_count) || 0))
      : 0;

    const tracks_count = Math.max(csvCreator.tracks_count || 0, Number(live?.num_total_clips) || 0, liveClips.length);
    const total_plays = Math.max(csvCreator.sample_plays || 0, profileStatsPlays, clipsPlaySum);
    const total_upvotes = Math.max(profileStatsUpvotes, clipsUpvoteSum);
    const hottest_song_plays = Math.max(maxLiveClipPlays, topSongs[0]?.play_count || 0);
    const hot_score = Math.round(
      total_plays * 0.65 +
      hottest_song_plays * 0.5 +
      total_upvotes * 12 +
      followers_count * 8 +
      tracks_count * 5
    );

    const style_tags = (csvCreator.style_focus || '')
      .split(',')
      .map(s => s.trim())
      .filter(Boolean);

    return {
      username: csvCreator.username,
      handle: csvCreator.username,
      display_name: live?.display_name || csvCreator.display_name,
      avatar_url:
        live?.avatar_image_url ||
        liveClips[0]?.avatar_image_url ||
        liveClips[0]?.image_url ||
        topSongs[0]?.image_url ||
        '',
      avatar_image_url:
        live?.avatar_image_url ||
        liveClips[0]?.avatar_image_url ||
        liveClips[0]?.image_url ||
        topSongs[0]?.image_url ||
        '',
      profile_description:
        live?.profile_description || `Suno AI Creator • ${csvCreator.style_focus}`,
      style_focus: csvCreator.style_focus,
      style_tags,
      styles: style_tags,
      tracks_count,
      num_total_clips: tracks_count,
      sample_plays: csvCreator.sample_plays,
      total_plays,
      total_upvotes,
      followers_count,
      hottest_song_plays,
      hot_score,
      verified: Boolean(live?.is_verified || total_plays >= 500 || tracks_count >= 30),
      is_live: Boolean(live),
      liveLoaded: Boolean(live),
      top_songs: topSongs
    };
  });

  enriched.sort((a, b) => {
    if (normalizedSort === 'plays') return (b.total_plays || 0) - (a.total_plays || 0);
    if (normalizedSort === 'tracks') return (b.tracks_count || 0) - (a.tracks_count || 0);
    return (b.hot_score || 0) - (a.hot_score || 0);
  });

  const ranked = enriched.map((creator, idx) => ({
    ...creator,
    rank: idx + 1
  }));

  const total_tracks = ranked.reduce((s, c) => s + (c.tracks_count || 0), 0);
  const total_plays = ranked.reduce((s, c) => s + (c.total_plays || 0), 0);

  return {
    sort_by: normalizedSort,
    total_creators: ranked.length,
    creators_count: ranked.length,
    total_tracks,
    total_plays,
    crawler_status: getCrawlerStatus(),
    creators: ranked
  };
}

/**
 * Gather and rank hot/most-played songs across top Vietnamese Suno creators
 * @param {{ sortBy?: 'hot'|'plays'|'upvotes', creator?: string, limit?: number, vnOnly?: boolean, forceRefresh?: boolean }} options
 */
export async function getSongsChart({
  sortBy = 'hot',
  creator = 'all',
  limit = 60,
  vnOnly = false,
  forceRefresh = false
} = {}) {
  const normalizedSort = ['hot', 'plays', 'upvotes'].includes(sortBy) ? sortBy : 'hot';
  const cleanCreator = String(creator || 'all').replace(/^@/, '').trim().toLowerCase();
  const maxItems = Math.max(1, Math.min(250, Number(limit) || 60));

  // Ensure at least the top chart creators have page-1 clips loaded in cache
  const missingChartCreators = CHART_CREATORS.filter(u => {
    const entry = cache.get(`profile:${u.toLowerCase()}:p1`);
    return forceRefresh || !entry || entry.data?._isFallback;
  });

  if (missingChartCreators.length > 0) {
    await Promise.race([
      Promise.allSettled(missingChartCreators.slice(0, 6).map(u => getProfile(u, 1, forceRefresh))),
      new Promise(resolve => setTimeout(resolve, 2500))
    ]);
    if (missingChartCreators.length > 6) {
      triggerBackgroundProfileWarm(missingChartCreators.slice(6));
    }
  }

  // If filtering by a specific creator not yet cached, load their profile directly
  if (cleanCreator && cleanCreator !== 'all' && (forceRefresh || !cache.has(`profile:${cleanCreator}:p1`))) {
    try {
      await getProfile(cleanCreator, 1, forceRefresh);
    } catch {}
  }

  const csvCreators = loadCsvCreators();
  const csvMap = new Map(csvCreators.map(c => [c.username.toLowerCase(), c]));

  const dedupedClips = new Map();

  for (const [key, entry] of cache.entries()) {
    if (!key.startsWith('profile:') || !entry?.data || entry.data._isFallback) continue;
    const profileData = entry.data;
    const profileHandle = (profileData.handle || key.split(':')[1] || 'futurescall').toLowerCase();
    const csvMeta = csvMap.get(profileHandle);
    const clips = Array.isArray(profileData.clips) ? profileData.clips : [];

    for (const rawClip of clips) {
      if (!rawClip || !rawClip.id) continue;
      const clipId = String(rawClip.id).toLowerCase();
      if (dedupedClips.has(clipId)) continue;

      const formatted = formatClip(rawClip);
      const handle = (rawClip.handle || profileData.handle || profileHandle).replace(/^@/, '');
      const display_name =
        rawClip.display_name ||
        profileData.display_name ||
        csvMeta?.display_name ||
        handle;
      const play_count = Number(formatted.play_count) || 0;
      const upvote_count = Number(formatted.upvote_count) || 0;
      const is_vietnamese = isVietnameseSong(formatted);
      const is_vn_hot_hit = isVietnameseHotHit(formatted);
      const hot_score = play_count + upvote_count * 25 + (is_vietnamese ? 150 : 0);
      const duration = Math.round(formatted.metadata?.duration || rawClip.duration || 180);
      const tags = formatted.metadata?.tags || formatted.tags || csvMeta?.style_focus || '';
      const prompt = formatted.metadata?.prompt || '';

      dedupedClips.set(clipId, {
        ...formatted,
        id: rawClip.id,
        title: formatted.title,
        handle,
        display_name,
        avatar_image_url:
          rawClip.avatar_image_url || profileData.avatar_image_url || '',
        image_url: formatted.image_url,
        audio_url: formatted.audio_url,
        play_count,
        upvote_count,
        duration,
        tags,
        style_focus: csvMeta?.style_focus || tags,
        is_vietnamese,
        is_vn_hot_hit,
        hot_score,
        metadata: {
          ...(formatted.metadata || {}),
          tags,
          prompt,
          duration
        }
      });
    }
  }

  let allSongs = Array.from(dedupedClips.values());

  if (cleanCreator && cleanCreator !== 'all') {
    allSongs = allSongs.filter(
      s => String(s.handle || '').toLowerCase() === cleanCreator
    );
  }

  if (vnOnly) {
    allSongs = allSongs.filter(s => s.is_vietnamese);
  }

  allSongs.sort((a, b) => {
    if (normalizedSort === 'plays') return (b.play_count || 0) - (a.play_count || 0);
    if (normalizedSort === 'upvotes') return (b.upvote_count || 0) - (a.upvote_count || 0);
    return (b.hot_score || 0) - (a.hot_score || 0);
  });

  const total_songs = allSongs.length;
  const total_plays = allSongs.reduce((sum, s) => sum + (s.play_count || 0), 0);
  const vietnamese_hot_hits_count = allSongs.filter(s => s.is_vn_hot_hit).length;

  return {
    total_songs,
    total_plays,
    vietnamese_hot_hits_count,
    creators_count: csvCreators.length || 32,
    sort_by: normalizedSort,
    creator_filter: creator,
    crawler_status: getCrawlerStatus(),
    songs: allSongs.slice(0, maxItems).map((s, i) => ({
      ...s,
      rank: i + 1
    }))
  };
}

// Start initial background crawl & ranking update shortly after server boot, then repeat every 30 minutes
setTimeout(() => {
  refreshAllRankings({ force: false }).catch(err =>
    console.warn('[RankingUpdater] Initial boot crawl failed:', err.message)
  );
}, 300);

setInterval(() => {
  refreshAllRankings({ force: true }).catch(err =>
    console.warn('[RankingUpdater] Scheduled crawl failed:', err.message)
  );
}, RANKING_UPDATE_INTERVAL_MS);


