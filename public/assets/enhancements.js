/**
 * kv-suno — Spotify Zen ("Pure Stream & Inspector") Architecture & Audio Engine (v6.0)
 * - Full Left Sidebar with 6 Zen Modes (Discover, Songs, Creators, Prompt Studio, Sound Wiki, Liked Songs) + 12 Pinned Creators + Prompt Cart
 * - Full Center Showcase with Live Lyrics Reader Stage + Dedicated Hot Songs Chart & Creators Leaderboard Views
 * - Clean Right Track DNA & Full Lyrics Inspector (strips promo spam headers, highlights [Verse]/[Chorus] metatags)
 */

import { ensureZenCustomTabsMounted, activateZenCreatorsTab, activateZenSongsTab } from './zen-tabs.js?v=6.4';

// Ensure React's Prompt Studio composer is mounted (never collapsed into floating pill)
try {
  if (localStorage.getItem('suno-studio-hidden') !== 'false') {
    localStorage.setItem('suno-studio-hidden', 'false');
  }
} catch (e) {}

// ============================================================================
// 0. Global Track & Lyrics Registry + Fetch Interceptor
// ============================================================================
window.kvTrackRegistry = window.kvTrackRegistry || {};

export function cleanSunoLyrics(rawPrompt = '') {
  if (!rawPrompt || typeof rawPrompt !== 'string') return '';
  let text = rawPrompt.trim();

  // If the entire prompt is a JSON object without lyrics, strip JSON keys or extract "lyrics"/"prompt"
  if (text.startsWith('{') && text.endsWith('}')) {
    try {
      const parsed = JSON.parse(text);
      text = parsed.lyrics || parsed.prompt || '';
    } catch (e) {
      // Fall through to line-by-line JSON stripping
    }
  }

  const lines = text.split(/\r?\n/);
  const cleaned = [];
  let inPromoBlock = false;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();

    // Skip separator lines (====, ****, ----, ⸻)
    if (/^[=*\-_~⸻—]{3,}$/.test(line)) {
      continue;
    }

    // Skip raw JSON braces, brackets, or JSON key-value lines (e.g. "title": "Game On",)
    if (
      /^[\{\}\[\],]+$/.test(line) ||
      /^"[a-zA-Z0-9_-]+"\s*:/.test(line)
    ) {
      continue;
    }

    // Skip social/promo header lines (Youtube:, Muzik VN:, Soundcloud, http/https links)
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

export function getFullTrackLyrics(track) {
  if (!track) return '';
  const id = (track.id || '').toLowerCase();
  const reg = id ? window.kvTrackRegistry[id] : null;

  const rawCandidates = [
    reg?.metadata?.prompt,
    track?.metadata?.prompt,
    reg?.prompt,
    track?.prompt,
    track?.caption
  ];

  const tagsStr = (track?.metadata?.tags || reg?.metadata?.tags || '').trim();

  for (const cand of rawCandidates) {
    if (!cand || typeof cand !== 'string') continue;
    const cleaned = cleanSunoLyrics(cand);
    // Avoid mistaking the style tags string for lyrics
    if (cleaned && cleaned !== tagsStr && cleaned.length > 8) {
      return cleaned;
    }
  }

  // Build a structured lyric & arrangement sheet when a track is instrumental or has minimal prompt text
  const title = (track.title || reg?.title || 'Suno AI Track').trim();
  const handle = (track.handle || reg?.handle || 'futurescall').replace(/^@/, '');
  const styleTags = tagsStr || 'Cinematic V-Pop, Studio Master';
  const shortLine = cleanSunoLyrics(track?.metadata?.prompt || reg?.metadata?.prompt || '');

  return [
    `[Intro • ${styleTags.split(',')[0] || 'Acoustic Atmosphere'}]`,
    `(Giai điệu mở đầu "${title}" — @${handle})`,
    '',
    '[Verse 1]',
    shortLine && shortLine !== tagsStr
      ? shortLine
      : `Thanh âm ngân vang giữa không gian đêm lắng đọng,\nTừng nhịp trống dẫn lối cảm xúc trôi theo "${title}".`,
    '',
    '[Chorus]',
    `${title} — giai điệu cuốn theo từng nhịp thở,\nHòa quyện sắc âm ${styleTags.split(',').slice(0, 3).join(', ')}.`,
    '',
    '[Outro • Fade Out]',
    `(Dư âm lắng đọng & kết thúc bản phối)`
  ].join('\n');
}

export function getShortLyricsPreview(track) {
  const full = getFullTrackLyrics(track);
  const lines = full
    .split(/\r?\n/)
    .map(l => l.trim())
    .filter(l => l && !/^[=*\-_~⸻—]+$/.test(l));

  // Prefer first non-tag lyric lines paired with section tag
  const previewLines = [];
  for (const line of lines) {
    if (previewLines.length >= 2) break;
    if (/^\[.*\]$/.test(line) && previewLines.length === 0) {
      previewLines.push(line);
    } else if (!/^\[.*\]$/.test(line)) {
      previewLines.push(line);
    }
  }
  const text = previewLines.join(' • ') || lines.slice(0, 2).join(' • ');
  return `🎤 ${text || 'Nhấn để nghe & xem lời bài hát (Lyrics)'}`;
}

window.kvGetShortLyricsPreview = getShortLyricsPreview;

function registerClipsInGlobalRegistry(clips, defaultHandle = '') {
  if (!Array.isArray(clips)) return;
  clips.forEach(c => {
    if (!c || !c.id) return;
    const id = String(c.id).toLowerCase();
    const prev = window.kvTrackRegistry[id] || {};
    const rawPrompt = c.metadata?.prompt || c.prompt || prev.metadata?.prompt || '';
    const cleanedPrompt = cleanSunoLyrics(rawPrompt);
    const rawTags = c.metadata?.tags || c.tags || prev.metadata?.tags || '';

    window.kvTrackRegistry[id] = {
      ...prev,
      ...c,
      id,
      title: (c.title || prev.title || 'Untitled Track').trim(),
      handle: (c.handle || defaultHandle || prev.handle || 'futurescall').replace(/^@/, ''),
      display_name: c.display_name || prev.display_name || 'Suno Creator',
      image_url: c.image_url || prev.image_url || `/favicon.png`,
      metadata: {
        ...(prev.metadata || {}),
        ...(c.metadata || {}),
        tags: rawTags,
        prompt: cleanedPrompt || rawPrompt
      }
    };
  });
}

// Seed registry from localStorage cache immediately on load
try {
  const cachedShowcase = localStorage.getItem('suno_showcase_local_cache');
  if (cachedShowcase) {
    const parsed = JSON.parse(cachedShowcase);
    if (parsed && Array.isArray(parsed.clips)) {
      registerClipsInGlobalRegistry(parsed.clips, 'futurescall');
    }
  }
} catch (e) {}

// Intercept fetch to automatically index all clips and lyrics returned by backend APIs
const _origFetch = window.fetch.bind(window);
window.fetch = async function (...args) {
  const res = await _origFetch(...args);
  try {
    const urlStr = typeof args[0] === 'string' ? args[0] : args[0]?.url || '';
    if (
      urlStr.includes('/api/suno-showcase') ||
      urlStr.includes('/api/suno-profile') ||
      urlStr.includes('/api/suno-similar') ||
      urlStr.includes('/api/suno-clip-info')
    ) {
      const clone = res.clone();
      clone
        .json()
        .then(data => {
          if (!data) return;
          if (Array.isArray(data.clips)) {
            registerClipsInGlobalRegistry(data.clips, data.handle || '');
          } else if (Array.isArray(data.similar_clips)) {
            registerClipsInGlobalRegistry(data.similar_clips);
          } else if (data.id) {
            registerClipsInGlobalRegistry([data], data.handle || '');
          }
          // Refresh DOM lyrics previews once new API data arrives
          setTimeout(() => {
            injectLyricsIntoDomCards();
            if (window.kvPlayer?.currentTrack) {
              window.kvPlayer.updateLyricsContent(window.kvPlayer.currentTrack);
            }
          }, 60);
        })
        .catch(() => {});
    }
  } catch (e) {}
  return res;
};

// ============================================================================
// 1. Authored Inline SVG Icon Library & Top Creators Catalog
// ============================================================================
const SP_ICONS = {
  brandWave: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M2 12h2m4-7v14m4-10v6m4-12v18m4-11v4"/></svg>`,
  home: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 9.5L12 3l9 6.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1V9.5z"/></svg>`,
  search: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>`,
  browse: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76"/></svg>`,
  library: `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5V4.5m4 15V4.5m4 15L19 6l-3-1.5L9 18"/></svg>`,
  plus: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>`,
  close: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>`,
  play: `<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M7 4.5v15l12-7.5z"/></svg>`,
  pause: `<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M6 4h4v16H6zm8 0h4v16h-4z"/></svg>`,
  prev: `<svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor"><polygon points="19 20 9 12 19 4 19 20"/><rect x="5" y="4" width="2.5" height="16"/></svg>`,
  next: `<svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor"><polygon points="5 4 15 12 5 20 5 4"/><rect x="16.5" y="4" width="2.5" height="16"/></svg>`,
  shuffle: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="16 3 21 3 21 8"/><line x1="4" y1="20" x2="21" y2="3"/><polyline points="21 16 21 21 16 21"/><line x1="15" y1="15" x2="21" y2="21"/><line x1="4" y1="4" x2="9" y2="9"/></svg>`,
  repeat: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/></svg>`,
  repeatOne: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/><path d="M11 10h1v4"/></svg>`,
  heartOutline: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>`,
  heartFilled: `<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>`,
  volHigh: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"/></svg>`,
  volLow: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><path d="M15.54 8.46a5 5 0 0 1 0 7.07"/></svg>`,
  volMute: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"/><line x1="23" y1="9" x2="17" y2="15"/><line x1="17" y1="9" x2="23" y2="15"/></svg>`,
  mic: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/><path d="M19 10v2a7 7 0 0 1-14 0v-2"/><line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/></svg>`,
  nowPlayingPane: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><line x1="15" y1="3" x2="15" y2="21"/></svg>`,
  sparkles: `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3l1.9 5.8a2 2 0 0 0 1.3 1.3L21 12l-5.8 1.9a2 2 0 0 0-1.3 1.3L12 21l-1.9-5.8a2 2 0 0 0-1.3-1.3L3 12l5.8-1.9a2 2 0 0 0 1.3-1.3L12 3z"/></svg>`,
  download: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>`,
  external: `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/><polyline points="15 3 21 3 21 9"/><line x1="10" y1="14" x2="21" y2="3"/></svg>`,
  book: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>`,
  disc: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="3"/></svg>`,
  sliders: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="4" y1="21" x2="4" y2="14"/><line x1="4" y1="10" x2="4" y2="3"/><line x1="12" y1="21" x2="12" y2="12"/><line x1="12" y1="8" x2="12" y2="3"/><line x1="20" y1="21" x2="20" y2="16"/><line x1="20" y1="12" x2="20" y2="3"/><line x1="1" y1="14" x2="7" y2="14"/><line x1="9" y1="8" x2="15" y2="8"/><line x1="17" y1="16" x2="23" y2="16"/></svg>`,
  guitar: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg>`,
  code: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>`,
  star: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>`,
  folder: `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"/></svg>`,
  globe: `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>`,
  check: `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>`,
  chevronDown: `<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>`,
  chevronLeft: `<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>`,
  chevronRight: `<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>`,
  arrowLeft: `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg>`,
  flame: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z"/></svg>`,
  crown: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m2 4 3 12h14l3-12-6 7-4-7-4 7-6-7zm3 16h14"/></svg>`,
  trophy: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6"/><path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18"/><path d="M4 22h16"/><path d="M10 14.66V17c0 .55-.47.98-.97 1.21C7.85 18.75 7 20.24 7 22"/><path d="M14 14.66V17c0 .55.47.98.97 1.21C16.15 18.75 17 20.24 17 22"/><path d="M18 2H6v7a6 6 0 0 0 12 0V2Z"/></svg>`
};

const PINNED_CREATORS = [
  { handle: 'futurescall', name: 'TienNg', style: 'Folk Trap & V-Pop', count: 56, color: '#1ed760' },
  { handle: 'muzikvn', name: '♡ Muzik VN ♡', style: 'V-Pop, K-Pop & Remix', count: 638, color: '#ec4899' },
  { handle: 'daoxudeo', name: 'PylonStudio', style: 'Indie & Experimental', count: 360, color: '#8b5cf6' },
  { handle: 'saigonmusic', name: 'Sài Gòn Bolero', style: 'Bolero & Trữ Tình', count: 1731, color: '#f59e0b' },
  { handle: 'phamphungoc', name: 'Pham Phu Ngoc', style: 'Nhạc Quê Hương', count: 2008, color: '#10b981' },
  { handle: 'aimusiccreater', name: 'Sapa Lo-Fi', style: 'Lo-Fi & Chillhop', count: 45, color: '#38bdf8' },
  { handle: 'calypsocrossfader0996', name: 'Họa Mi Đi Nắng', style: 'Melodic Trap & Pop', count: 301, color: '#f43f5e' },
  { handle: 'rawenergy068', name: 'PASS1LIFE', style: 'Dân Ca & Rock', count: 320, color: '#fb923c' },
  { handle: 'neomusiccafe', name: 'RaonBlue', style: 'Asian Pop Ballad', count: 284, color: '#60a5fa' },
  { handle: 'luinight', name: 'NLN_LuiNight', style: 'Ballad & Melodic Rap', count: 43, color: '#a78bfa' },
  { handle: 'twinh', name: 'Viet EDM', style: 'Vinahouse & Club', count: 21, color: '#2dd4bf' },
  { handle: 'sambaguitarstrings724', name: 'ThreetN', style: 'Acoustic & Indie', count: 24, color: '#facc15' }
];

// ============================================================================
// 2. Toast Notification System
// ============================================================================
export function showToast(message) {
  let container = document.getElementById('studio-toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'studio-toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = 'studio-toast';
  toast.innerHTML = `${SP_ICONS.check}<span>${message}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(8px)';
    toast.style.transition = 'opacity 0.2s ease, transform 0.2s ease';
    setTimeout(() => toast.remove(), 200);
  }, 2200);
}

if (navigator.clipboard && navigator.clipboard.writeText) {
  const originalWriteText = navigator.clipboard.writeText.bind(navigator.clipboard);
  navigator.clipboard.writeText = async function (text) {
    const res = await originalWriteText(text);
    showToast('Copied to clipboard');
    return res;
  };
}

// ============================================================================
// 3. Lightweight Ambient Color Updater
// ============================================================================
const FALLBACK_AMBIENT_PALETTE = [
  '30, 215, 96',
  '56, 189, 248',
  '139, 92, 246',
  '245, 158, 11',
  '16, 185, 129',
  '236, 72, 153'
];

function updateAmbientColorFromTrack(track) {
  if (!track) return;
  const seed = (track.id || track.title || 'kv').split('').reduce((acc, ch) => acc + ch.charCodeAt(0), 0);
  const rgb = FALLBACK_AMBIENT_PALETTE[seed % FALLBACK_AMBIENT_PALETTE.length];
  document.documentElement.style.setProperty('--kv-ambient-rgb', rgb);
}

// ============================================================================
// 4. Default Track & Global Audio Engine (KvMusicPlayer)
// ============================================================================
const DEFAULT_TRACK = {
  id: 'a15a3e62-b002-4e5b-a571-0d86adc503f2',
  title: 'Duyên Lỡ .. Lỡ Duyên',
  image_url: 'https://cdn2.suno.ai/746f5806-0e35-4ee5-94fd-e64d2f9a333b.jpeg',
  handle: 'futurescall',
  display_name: 'TienNg',
  metadata: {
    tags: 'Vietnamese folk trap, fast driving tempo, aggressive male rap, hard 808 sub bass, bamboo flute, dan tranh zither solo',
    prompt: '[INTRO]\n(Nhịp trống Trap dồn dập, tiếng đàn tranh rải mộc mạc)\nÂm dương... dương âm.\n\n[VERSE 1]\nĐêm mờ sương, người thương mờ mắt,\nTình dắt lối, tối lối ta đi.\n\n[CHORUS]\nDuyên âm không thành, đừng thành âm duyên.\nTình thực tan tành, đừng tành thực tình.',
    duration: 181.2
  }
};

registerClipsInGlobalRegistry([DEFAULT_TRACK], 'futurescall');

class KvMusicPlayer {
  constructor() {
    this.audio = document.createElement('audio');
    this.audio.id = 'kv-pwa-audio-engine';
    this.audio.preload = 'auto';
    this.audio.setAttribute('playsinline', '');
    this.audio.setAttribute('webkit-playsinline', '');
    this.audio.setAttribute('x-webkit-airplay', 'allow');
    this.audio.style.display = 'none';

    this.mountAudioElementInDOM();

    this.currentTrack = DEFAULT_TRACK;
    this.queue = [DEFAULT_TRACK];
    this.currentIndex = 0;
    this.isPlaying = false;
    this._wantPlaying = false;
    this.isShuffle = false;
    this.repeatMode = 'off';
    this.volume = parseFloat(localStorage.getItem('kv_player_vol') || '0.85');
    this.isMuted = false;
    this.prevVolume = this.volume;
    this.likedTracks = new Set(JSON.parse(localStorage.getItem('kv_player_likes') || '[]'));
    this.likedTrackMeta = JSON.parse(localStorage.getItem('kv_player_liked_meta') || '{}');
    this._lastCallTime = 0;
    this._lastPositionSyncTime = 0;
    this._preloadedIds = new Set();
    this._bgAudioCtx = null;
    this._bgKeepAliveOsc = null;
    this._stallTimer = null;

    this.audio.volume = this.volume;
    this.audio.src = `/api/suno-audio-proxy?input=${DEFAULT_TRACK.id}`;

    this.setupAudioListeners();
    this.setupMediaSession();
    this.setupBackgroundContinuity();
    this.setupKeyboardShortcuts();
    this.updateMediaSession(DEFAULT_TRACK);
  }

  mountAudioElementInDOM() {
    const attach = () => {
      if (document.body && !document.getElementById('kv-pwa-audio-engine')) {
        document.body.appendChild(this.audio);
      }
    };
    if (document.body) {
      attach();
    } else {
      document.addEventListener('DOMContentLoaded', attach, { once: true });
    }
  }

  /**
   * Maintains an ultra-low-gain (-100 dB, 1 Hz) Web Audio graph while playing
   * so Android & iOS never suspend the PWA process during the 50-150ms track-switch
   * handoff when the phone screen is turned off or locked.
   */
  ensureBackgroundAudioKeepAlive(active) {
    try {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (!AudioCtx) return;

      if (active) {
        if (!this._bgAudioCtx) {
          this._bgAudioCtx = new AudioCtx();
          const osc = this._bgAudioCtx.createOscillator();
          const gain = this._bgAudioCtx.createGain();
          osc.type = 'sine';
          osc.frequency.value = 1; // Sub-audible 1 Hz
          gain.gain.value = 0.00001; // Near-zero amplitude (-100 dB)
          osc.connect(gain);
          gain.connect(this._bgAudioCtx.destination);
          osc.start();
          this._bgKeepAliveOsc = osc;
        }
        if (this._bgAudioCtx.state === 'suspended') {
          this._bgAudioCtx.resume().catch(() => {});
        }
      } else if (this._bgAudioCtx && this._bgAudioCtx.state === 'running') {
        this._bgAudioCtx.suspend().catch(() => {});
      }
    } catch (e) {}
  }

  /**
   * Pre-warms the next 2 tracks in the queue on the backend (.m4a FFmpeg cache)
   * while the current song is playing, so when a song finishes with the phone screen off,
   * the next song starts with 0ms server latency.
   */
  preloadUpcomingTracks() {
    if (!this.queue || this.queue.length <= 1) return;
    const candidates = [];
    const nextIdx1 = (this.currentIndex + 1) % this.queue.length;
    const nextIdx2 = (this.currentIndex + 2) % this.queue.length;
    if (this.queue[nextIdx1]?.id) candidates.push(this.queue[nextIdx1].id);
    if (this.queue[nextIdx2]?.id && nextIdx2 !== nextIdx1) candidates.push(this.queue[nextIdx2].id);

    candidates.forEach(trackId => {
      if (!trackId || this._preloadedIds.has(trackId)) return;
      this._preloadedIds.add(trackId);
      fetch(`/api/suno-audio-proxy?input=${encodeURIComponent(trackId)}`, {
        method: 'GET',
        headers: { Range: 'bytes=0-32767' }
      }).catch(() => {
        this._preloadedIds.delete(trackId);
      });
    });
  }

  setupBackgroundContinuity() {
    // Never pause when the phone screen turns off; instead refresh lock-screen state
    // and immediately recover if the mobile OS momentarily interrupted playback.
    document.addEventListener('visibilitychange', () => {
      if (document.hidden) {
        if (this._wantPlaying) {
          this.ensureBackgroundAudioKeepAlive(true);
          this.updateMediaSession(this.currentTrack);
          this.syncMediaSessionPositionState();
          if (this.audio.paused && !this.audio.ended) {
            this.audio.play().catch(() => {});
          }
        }
      } else {
        this.updatePlayStateUI(!this.audio.paused);
        this.updateProgressUI();
        this.updateActiveTrackInDOM();
        this.syncMediaSessionPositionState();
      }
    });

    window.addEventListener('pagehide', () => {
      if (this._wantPlaying && this.audio.paused && !this.audio.ended) {
        this.audio.play().catch(() => {});
      }
    });
  }

  setupAudioListeners() {
    this.audio.addEventListener('timeupdate', () => {
      this.updateProgressUI();
      const now = Date.now();
      if (now - this._lastPositionSyncTime >= 1000) {
        this._lastPositionSyncTime = now;
        this.syncMediaSessionPositionState();
      }
    });

    this.audio.addEventListener('loadedmetadata', () => {
      const durEl = document.getElementById('kv-player-duration');
      if (durEl && this.audio.duration && !isNaN(this.audio.duration)) {
        durEl.textContent = this.formatTime(this.audio.duration);
      }
      this.syncMediaSessionPositionState();
      this.preloadUpcomingTracks();
    });

    this.audio.addEventListener('durationchange', () => {
      this.syncMediaSessionPositionState();
    });

    this.audio.addEventListener('seeked', () => {
      this.updateProgressUI();
      this.syncMediaSessionPositionState();
    });

    this.audio.addEventListener('play', () => {
      this.isPlaying = true;
      this._wantPlaying = true;
      this.ensureBackgroundAudioKeepAlive(true);
      this.updatePlayStateUI(true);
      this.updateActiveTrackInDOM();
      this.syncMediaSessionPlaybackState(true);
      this.syncMediaSessionPositionState();
      this.preloadUpcomingTracks();
    });

    this.audio.addEventListener('playing', () => {
      if (this._stallTimer) {
        clearTimeout(this._stallTimer);
        this._stallTimer = null;
      }
      this.isPlaying = true;
      this._wantPlaying = true;
      this.ensureBackgroundAudioKeepAlive(true);
      this.updatePlayStateUI(true);
      this.updateActiveTrackInDOM();
      this.syncMediaSessionPlaybackState(true);
      this.syncMediaSessionPositionState();
    });

    this.audio.addEventListener('pause', () => {
      // If the track naturally reached the end, do not mark as paused — 'ended' will advance immediately
      if (this.audio.ended) return;

      // If screen just locked (document.hidden) and user did NOT request pause, recover playback
      if (document.hidden && this._wantPlaying) {
        setTimeout(() => {
          if (this._wantPlaying && this.audio.paused && !this.audio.ended) {
            this.audio.play().catch(() => {});
          }
        }, 120);
        return;
      }

      this.isPlaying = false;
      this.updatePlayStateUI(false);
      this.updateActiveTrackInDOM();
      this.syncMediaSessionPlaybackState(false);
      this.syncMediaSessionPositionState();
    });

    const handleStallOrWaiting = () => {
      if (!this._wantPlaying) return;
      if (this._stallTimer) clearTimeout(this._stallTimer);
      this._stallTimer = setTimeout(() => {
        if (this._wantPlaying && this.audio.paused && !this.audio.ended) {
          this.audio.play().catch(() => {});
        }
      }, 3000);
    };
    this.audio.addEventListener('stalled', handleStallOrWaiting);
    this.audio.addEventListener('waiting', handleStallOrWaiting);

    this.audio.addEventListener('ended', () => {
      this._wantPlaying = true;
      this.ensureBackgroundAudioKeepAlive(true);
      if (this.repeatMode === 'one') {
        this.audio.currentTime = 0;
        this.audio.play().catch(() => {});
      } else {
        this.next(true);
      }
    });

    this.audio.addEventListener('error', () => {
      console.warn('[KvPlayer] Audio error:', this.audio.error);
      if (this._wantPlaying || this.isPlaying) {
        showToast('Stream unavailable, skipping to next track...');
        setTimeout(() => this.next(true), 800);
      }
    });
  }

  formatTime(secs) {
    if (!secs || isNaN(secs) || secs < 0) return '0:00';
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  }

  normalizeTrack(track) {
    if (!track) return null;
    let trackId = track.id || track.clipId;
    if (!trackId) return null;

    const uuidMatch = String(trackId).match(/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i);
    if (uuidMatch) {
      trackId = uuidMatch[0].toLowerCase();
    }

    const reg = window.kvTrackRegistry[trackId] || {};
    const rawTags =
      track.metadata?.tags ||
      reg.metadata?.tags ||
      track.tags ||
      track.category ||
      track.snippet ||
      'Suno AI Studio, High-Fidelity Stream';

    const candidatePrompt =
      (track.metadata?.prompt && track.metadata.prompt !== rawTags ? track.metadata.prompt : '') ||
      reg.metadata?.prompt ||
      track.metadata?.prompt ||
      '';

    const cleanedPrompt = cleanSunoLyrics(candidatePrompt);

    const normalized = {
      id: trackId,
      title: (track.title || reg.title || track.name || 'Suno Music Track').trim(),
      image_url: track.image_url || reg.image_url || track.image_large_url || '/favicon.png',
      handle: (track.handle || reg.handle || (track.display_name ? `${track.display_name}` : 'futurescall')).replace(/^@/, ''),
      display_name: track.display_name || reg.display_name || track.handle || 'Suno Artist',
      play_count: track.play_count ?? reg.play_count ?? 0,
      metadata: {
        ...(reg.metadata || {}),
        ...(track.metadata || {}),
        tags: rawTags,
        prompt: cleanedPrompt || candidatePrompt,
        duration: track.metadata?.duration || reg.metadata?.duration || track.duration
      }
    };

    registerClipsInGlobalRegistry([normalized], normalized.handle);
    return normalized;
  }

  async ensureFullClipMetadata(track) {
    if (!track || !track.id) return;
    const id = track.id.toLowerCase();
    const reg = window.kvTrackRegistry[id];
    const hasRealLyrics =
      reg?.metadata?.prompt &&
      reg.metadata.prompt !== reg.metadata?.tags &&
      cleanSunoLyrics(reg.metadata.prompt).length > 15;

    if (hasRealLyrics) return;

    try {
      const res = await fetch(`/api/suno-clip-info?songId=${encodeURIComponent(id)}`);
      if (res.ok) {
        const clipData = await res.json();
        if (clipData && clipData.id) {
          registerClipsInGlobalRegistry([clipData], clipData.handle || track.handle);
          if (this.currentTrack && this.currentTrack.id === id) {
            this.currentTrack = this.normalizeTrack(clipData);
            this.updateLyricsContent(this.currentTrack);
            this.updateNowPlayingPane(this.currentTrack);
            updateCenterLyricsStage(this.currentTrack);
          }
        }
      }
    } catch (e) {}
  }

  setQueueIfEmpty(list) {
    if (!list || !Array.isArray(list) || list.length === 0) return;
    registerClipsInGlobalRegistry(list);
    const normalizedList = list.map(t => this.normalizeTrack(t)).filter(Boolean);
    if (normalizedList.length === 0) return;

    if (this.queue.length <= 1) {
      this.queue = normalizedList;
    }
    if ((!this.currentTrack || this.currentTrack.id === DEFAULT_TRACK.id) && !this.isPlaying && this.queue.length > 0) {
      const first = this.queue[0];
      this.currentTrack = first;
      this.currentIndex = 0;
      this.updateTrackInfoUI(first);
      this.updateMediaSession(first);
      this.updateLyricsContent(first);
      this.updateNowPlayingPane(first);
      updateCenterLyricsStage(first);
      this.audio.src = `/api/suno-audio-proxy?input=${encodeURIComponent(first.id)}`;
    }
  }

  async preloadDefaultShowcase() {
    try {
      let clips = null;
      const cached = localStorage.getItem('suno_showcase_local_cache');
      if (cached) {
        try {
          const parsed = JSON.parse(cached);
          if (parsed && Array.isArray(parsed.clips) && parsed.clips.length > 0) {
            clips = parsed.clips;
          }
        } catch (e) {}
      }

      if (!clips) {
        const res = await fetch('/api/suno-showcase?handle=futurescall');
        if (res.ok) {
          const data = await res.json();
          if (data && Array.isArray(data.clips) && data.clips.length > 0) {
            clips = data.clips;
          }
        }
      }

      if (clips && clips.length > 0) {
        registerClipsInGlobalRegistry(clips, 'futurescall');
        this.setQueueIfEmpty(clips);
      }
    } catch (e) {
      console.warn('[KvPlayer] Preload error:', e);
    }
  }

  playTrack(track, queueList = null, isExplicitToggle = false, isAutoAdvance = false) {
    const now = Date.now();
    const norm = this.normalizeTrack(track);
    if (!norm) return;

    if (!isAutoAdvance && this._lastCallTime && (now - this._lastCallTime < 250) && this.currentTrack?.id === norm.id) {
      return;
    }
    this._lastCallTime = now;

    if (queueList && Array.isArray(queueList) && queueList.length > 0) {
      registerClipsInGlobalRegistry(queueList);
      this.queue = queueList.map(t => this.normalizeTrack(t)).filter(Boolean);
      this.currentIndex = this.queue.findIndex(t => t.id === norm.id);
      if (this.currentIndex === -1) {
        this.queue.unshift(norm);
        this.currentIndex = 0;
      }
    } else if (this.queue.length === 0) {
      this.queue = [norm];
      this.currentIndex = 0;
    } else {
      const idx = this.queue.findIndex(t => t.id === norm.id);
      if (idx !== -1) {
        this.currentIndex = idx;
      } else {
        this.queue.push(norm);
        this.currentIndex = this.queue.length - 1;
      }
    }

    if (this.currentTrack && this.currentTrack.id === norm.id && this.audio.src.includes(norm.id) && !isAutoAdvance) {
      if (isExplicitToggle) {
        if (this.audio.paused) {
          this._wantPlaying = true;
          this.ensureBackgroundAudioKeepAlive(true);
          this.audio.play().catch(e => console.warn('[KvPlayer] play failed:', e));
        } else {
          this._wantPlaying = false;
          this.ensureBackgroundAudioKeepAlive(false);
          this.audio.pause();
        }
        return;
      }
      if (this.audio.paused) {
        this._wantPlaying = true;
        this.ensureBackgroundAudioKeepAlive(true);
        this.audio.play().catch(e => console.warn('[KvPlayer] play failed:', e));
      }
      return;
    }

    this.currentTrack = norm;
    this._wantPlaying = true;
    this.ensureBackgroundAudioKeepAlive(true);

    // Update MediaSession metadata BEFORE starting audio so Android/iOS lock screens update immediately
    this.updateMediaSession(norm);
    this.updateTrackInfoUI(norm);
    this.updateActiveTrackInDOM();
    this.updateLyricsContent(norm);
    this.updateNowPlayingPane(norm);
    updateCenterLyricsStage(norm);

    const streamUrl = `/api/suno-audio-proxy?input=${encodeURIComponent(norm.id)}`;
    this.audio.src = streamUrl;

    const playPromise = this.audio.play();
    if (playPromise !== undefined) {
      playPromise
        .then(() => {
          this.syncMediaSessionPlaybackState(true);
          this.syncMediaSessionPositionState();
          this.preloadUpcomingTracks();
        })
        .catch(err => {
          console.warn('[KvPlayer] Autoplay prevented or deferred:', err);
          if (!document.hidden) {
            showToast('Tap Play to start audio stream');
          }
        });
    }

    this.ensureFullClipMetadata(norm);
  }

  togglePlay() {
    if (!this.currentTrack) {
      if (this.queue.length > 0) {
        this.playTrack(this.queue[0], this.queue, true);
        return;
      }
      this.playTrack(DEFAULT_TRACK, [DEFAULT_TRACK], true);
      return;
    }

    if (this.audio.paused) {
      this._wantPlaying = true;
      this.ensureBackgroundAudioKeepAlive(true);
      if (!this.audio.src || this.audio.src === '' || this.audio.src === window.location.href) {
        this.audio.src = `/api/suno-audio-proxy?input=${encodeURIComponent(this.currentTrack.id)}`;
      }
      const p = this.audio.play();
      if (p !== undefined) {
        p.then(() => {
          this.syncMediaSessionPlaybackState(true);
          this.syncMediaSessionPositionState();
        }).catch(e => {
          console.warn('[KvPlayer] Play error:', e);
          showToast('Select a track to start listening');
        });
      }
    } else {
      this._wantPlaying = false;
      this.ensureBackgroundAudioKeepAlive(false);
      this.audio.pause();
      this.syncMediaSessionPlaybackState(false);
    }
  }

  next(isAutoAdvance = false) {
    if (this.queue.length === 0) return;

    if (this.isShuffle && this.queue.length > 1) {
      let randIdx;
      do {
        randIdx = Math.floor(Math.random() * this.queue.length);
      } while (randIdx === this.currentIndex);
      this.currentIndex = randIdx;
    } else {
      this.currentIndex++;
      if (this.currentIndex >= this.queue.length) {
        // Default to wrapping around to track #1 when auto-advancing so background playback continues seamlessly
        if (this.repeatMode !== 'off' || isAutoAdvance) {
          this.currentIndex = 0;
        } else {
          this.currentIndex = 0;
        }
      }
    }

    this.playTrack(this.queue[this.currentIndex], null, false, isAutoAdvance);
  }

  prev() {
    if (this.queue.length === 0) return;

    if (this.audio.currentTime > 3) {
      this.audio.currentTime = 0;
      this.syncMediaSessionPositionState();
      return;
    }

    this.currentIndex--;
    if (this.currentIndex < 0) {
      this.currentIndex = this.queue.length - 1;
    }
    this.playTrack(this.queue[this.currentIndex], null, false, true);
  }

  seek(seconds) {
    const dur = this.audio.duration || this.currentTrack?.metadata?.duration || 0;
    if (dur && !isNaN(dur) && dur > 0) {
      this.audio.currentTime = Math.max(0, Math.min(seconds, dur));
      this.updateProgressUI();
      this.syncMediaSessionPositionState();
    }
  }

  setVolume(val) {
    val = Math.max(0, Math.min(1, val));
    this.volume = val;
    this.audio.volume = val;
    this.isMuted = val === 0;
    try {
      localStorage.setItem('kv_player_vol', val.toString());
    } catch (e) {}
    this.updateVolumeUI();
  }

  toggleMute() {
    if (this.isMuted) {
      this.setVolume(this.prevVolume || 0.85);
    } else {
      this.prevVolume = this.volume > 0 ? this.volume : 0.85;
      this.setVolume(0);
    }
  }

  toggleShuffle() {
    this.isShuffle = !this.isShuffle;
    const btn = document.getElementById('kv-btn-shuffle');
    if (btn) btn.classList.toggle('active', this.isShuffle);
    showToast(this.isShuffle ? 'Shuffle: On' : 'Shuffle: Off');
  }

  toggleRepeat() {
    if (this.repeatMode === 'off') {
      this.repeatMode = 'all';
      showToast('Repeat: All Queue');
    } else if (this.repeatMode === 'all') {
      this.repeatMode = 'one';
      showToast('Repeat: Current Track');
    } else {
      this.repeatMode = 'off';
      showToast('Repeat: Off');
    }

    const btn = document.getElementById('kv-btn-repeat');
    if (btn) {
      btn.classList.toggle('active', this.repeatMode !== 'off');
      btn.innerHTML = this.repeatMode === 'one' ? SP_ICONS.repeatOne : SP_ICONS.repeat;
    }
  }

  toggleLike(trackObj = null) {
    const targetTrack = trackObj ? this.normalizeTrack(trackObj) : this.currentTrack;
    if (!targetTrack || !targetTrack.id) return;
    const trackId = targetTrack.id;

    if (this.likedTracks.has(trackId)) {
      this.likedTracks.delete(trackId);
      delete this.likedTrackMeta[trackId];
      showToast('Removed from Liked Songs');
    } else {
      this.likedTracks.add(trackId);
      this.likedTrackMeta[trackId] = targetTrack;
      showToast('Added to Liked Songs');
    }

    try {
      localStorage.setItem('kv_player_likes', JSON.stringify([...this.likedTracks]));
      localStorage.setItem('kv_player_liked_meta', JSON.stringify(this.likedTrackMeta));
    } catch (e) {}

    this.updateLikeButtonUI();
    renderLikedSongsPlaylist();
  }

  toggleLyrics(force = null) {
    const ws = document.getElementById('kv-sp-workspace');
    if (!ws) return;

    const currentlyOpen = !ws.classList.contains('right-closed');
    const shouldOpen = force !== null ? force : !currentlyOpen;
    ws.classList.toggle('right-closed', !shouldOpen);

    document.getElementById('kv-btn-np-pane')?.classList.toggle('active', shouldOpen);
    document.getElementById('kv-btn-lyrics-toggle')?.classList.toggle('active', shouldOpen);
    document.getElementById('kv-sp-mobile-backdrop')?.classList.toggle('active', shouldOpen);

    if (shouldOpen && window.innerWidth > 1023) {
      const lyricsBox = document.getElementById('kv-sp-np-lyrics');
      lyricsBox?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
  }

  /**
   * Registers all OS lock-screen, Bluetooth headset, smartwatch & notification media controls
   */
  setupMediaSession() {
    if (!('mediaSession' in navigator)) return;

    const handlers = [
      [
        'play',
        () => {
          this._wantPlaying = true;
          this.ensureBackgroundAudioKeepAlive(true);
          this.audio.play().catch(() => this.togglePlay());
        }
      ],
      [
        'pause',
        () => {
          this._wantPlaying = false;
          this.ensureBackgroundAudioKeepAlive(false);
          this.audio.pause();
          this.syncMediaSessionPlaybackState(false);
        }
      ],
      ['previoustrack', () => this.prev()],
      ['nexttrack', () => this.next(true)],
      [
        'seekbackward',
        details => {
          const offset = details?.seekOffset || 10;
          this.seek(Math.max(0, (this.audio.currentTime || 0) - offset));
        }
      ],
      [
        'seekforward',
        details => {
          const offset = details?.seekOffset || 10;
          this.seek((this.audio.currentTime || 0) + offset);
        }
      ],
      [
        'seekto',
        details => {
          if (details && typeof details.seekTime === 'number') {
            if (details.fastSeek && typeof this.audio.fastSeek === 'function') {
              this.audio.fastSeek(details.seekTime);
            } else {
              this.seek(details.seekTime);
            }
            this.syncMediaSessionPositionState();
          }
        }
      ],
      [
        'stop',
        () => {
          this._wantPlaying = false;
          this.ensureBackgroundAudioKeepAlive(false);
          this.audio.pause();
          this.syncMediaSessionPlaybackState(false);
        }
      ]
    ];

    for (const [action, handler] of handlers) {
      try {
        navigator.mediaSession.setActionHandler(action, handler);
      } catch (e) {
        // Ignore actions unsupported by specific browser versions
      }
    }
  }

  syncMediaSessionPlaybackState(isPlaying) {
    if ('mediaSession' in navigator) {
      try {
        navigator.mediaSession.playbackState = isPlaying ? 'playing' : 'paused';
      } catch (e) {}
    }
    if (this.currentTrack) {
      const handle = (this.currentTrack.handle || 'futurescall').replace(/^@/, '');
      document.title = isPlaying
        ? `▶ ${this.currentTrack.title} — @${handle} | kv-suno`
        : 'kv-suno — AI Music Streaming & Prompt Studio';
    }
  }

  syncMediaSessionPositionState() {
    if (!('mediaSession' in navigator) || typeof navigator.mediaSession.setPositionState !== 'function') {
      return;
    }
    try {
      const rawDur = this.audio.duration || this.currentTrack?.metadata?.duration || 0;
      if (rawDur && isFinite(rawDur) && rawDur > 0) {
        const pos = Math.max(0, Math.min(this.audio.currentTime || 0, rawDur));
        navigator.mediaSession.setPositionState({
          duration: rawDur,
          playbackRate: this.audio.playbackRate || 1,
          position: pos
        });
      }
    } catch (e) {}
  }

  updateMediaSession(track) {
    if (!('mediaSession' in navigator) || !track) return;

    this.setupMediaSession();

    const origin = window.location.origin;
    const toAbsUrl = url => {
      try {
        return new URL(url, origin).href;
      } catch (e) {
        return `${origin}/pwa-512.png`;
      }
    };

    const coverUrl = track.image_url ? toAbsUrl(track.image_url) : `${origin}/pwa-512.png`;
    const isPng = /\.png(\?|$)/i.test(coverUrl);
    const isWebp = /\.webp(\?|$)/i.test(coverUrl);
    const mimeType = isPng ? 'image/png' : isWebp ? 'image/webp' : 'image/jpeg';

    const handle = (track.handle || 'futurescall').replace(/^@/, '');
    const artistName =
      track.display_name && track.display_name.toLowerCase() !== handle.toLowerCase()
        ? `${track.display_name} (@${handle})`
        : `@${handle}`;

    const primaryGenre =
      (track.metadata?.tags || '')
        .split(/[,;]+/)
        .map(s => s.trim())
        .filter(Boolean)[0] || 'AI Music Stream';

    try {
      navigator.mediaSession.metadata = new MediaMetadata({
        title: track.title || 'Suno Music Track',
        artist: artistName,
        album: `kv-suno • ${primaryGenre}`,
        artwork: [
          { src: coverUrl, sizes: '96x96', type: mimeType },
          { src: coverUrl, sizes: '128x128', type: mimeType },
          { src: coverUrl, sizes: '192x192', type: mimeType },
          { src: coverUrl, sizes: '256x256', type: mimeType },
          { src: coverUrl, sizes: '384x384', type: mimeType },
          { src: coverUrl, sizes: '512x512', type: mimeType },
          { src: `${origin}/pwa-192.png`, sizes: '192x192', type: 'image/png' },
          { src: `${origin}/pwa-512.png`, sizes: '512x512', type: 'image/png' }
        ]
      });
    } catch (e) {}

    this.syncMediaSessionPlaybackState(this.isPlaying);
    this.syncMediaSessionPositionState();
  }

  setupKeyboardShortcuts() {
    window.addEventListener('keydown', e => {
      const tag = document.activeElement?.tagName?.toLowerCase();
      if (tag === 'input' || tag === 'textarea' || document.activeElement?.isContentEditable) {
        return;
      }

      if (e.code === 'Space') {
        e.preventDefault();
        this.togglePlay();
      } else if (e.code === 'ArrowRight') {
        e.preventDefault();
        this.seek(this.audio.currentTime + 5);
      } else if (e.code === 'ArrowLeft') {
        e.preventDefault();
        this.seek(this.audio.currentTime - 5);
      } else if (e.code === 'ArrowUp') {
        e.preventDefault();
        this.setVolume(this.volume + 0.05);
      } else if (e.code === 'ArrowDown') {
        e.preventDefault();
        this.setVolume(this.volume - 0.05);
      } else if (e.code === 'KeyM') {
        e.preventDefault();
        this.toggleMute();
      } else if (e.code === 'KeyL') {
        e.preventDefault();
        this.toggleLyrics();
      }
    });
  }

  updateTrackInfoUI(track) {
    const coverEl = document.getElementById('kv-player-cover');
    const titleEl = document.getElementById('kv-player-title');
    const artistEl = document.getElementById('kv-player-artist');
    const sunoLink = document.getElementById('kv-player-suno-link');

    if (coverEl) {
      coverEl.src = track.image_url || '/favicon.png';
      coverEl.alt = track.title || 'Track Cover';
    }
    if (titleEl) {
      titleEl.textContent = track.title || 'Untitled Track';
      titleEl.title = track.title || '';
    }
    if (artistEl) {
      const handle = (track.handle || '').replace(/^@/, '');
      artistEl.innerHTML = handle
        ? `<a href="https://suno.com/@${handle}" target="_blank" rel="noreferrer">@${handle}</a>`
        : (track.display_name || 'Suno Artist');
    }
    if (sunoLink) {
      sunoLink.href = `https://suno.com/song/${track.id}`;
    }

    const dur = track.metadata?.duration;
    const durEl = document.getElementById('kv-player-duration');
    if (durEl && dur) {
      durEl.textContent = this.formatTime(dur);
    }

    updateAmbientColorFromTrack(track);
    this.updateLikeButtonUI();
    this.updateNowPlayingPane(track);
  }

  updatePlayStateUI(isPlaying) {
    const playBtn = document.getElementById('kv-btn-play');
    const eqBars = document.querySelectorAll('.kv-player-eq span');

    if (playBtn) {
      playBtn.innerHTML = isPlaying ? SP_ICONS.pause : SP_ICONS.play;
      playBtn.title = isPlaying ? 'Pause (Space)' : 'Play (Space)';
    }

    eqBars.forEach(b => {
      b.style.animationPlayState = isPlaying ? 'running' : 'paused';
    });
  }

  updateProgressUI() {
    const curEl = document.getElementById('kv-player-current');
    const fillEl = document.getElementById('kv-scrubber-fill');
    const thumbEl = document.getElementById('kv-scrubber-thumb');

    const cur = this.audio.currentTime || 0;
    const dur = this.audio.duration || 1;
    const pct = Math.min(100, Math.max(0, (cur / dur) * 100));

    if (curEl) curEl.textContent = this.formatTime(cur);
    if (fillEl) fillEl.style.width = `${pct}%`;
    if (thumbEl) thumbEl.style.left = `${pct}%`;
  }

  updateVolumeUI() {
    const slider = document.getElementById('kv-vol-slider');
    const iconBtn = document.getElementById('kv-btn-vol');

    if (slider) slider.value = Math.round(this.volume * 100);
    if (iconBtn) {
      if (this.isMuted || this.volume === 0) {
        iconBtn.innerHTML = SP_ICONS.volMute;
      } else if (this.volume < 0.5) {
        iconBtn.innerHTML = SP_ICONS.volLow;
      } else {
        iconBtn.innerHTML = SP_ICONS.volHigh;
      }
    }
  }

  updateLikeButtonUI() {
    const isLiked = this.currentTrack && this.likedTracks.has(this.currentTrack.id);
    const likeBtn = document.getElementById('kv-btn-like');
    if (likeBtn) {
      likeBtn.classList.toggle('liked', !!isLiked);
      likeBtn.innerHTML = isLiked ? SP_ICONS.heartFilled : SP_ICONS.heartOutline;
    }

    const npLikeBtn = document.getElementById('kv-sp-np-like');
    if (npLikeBtn) {
      npLikeBtn.classList.toggle('liked', !!isLiked);
      npLikeBtn.innerHTML = isLiked ? SP_ICONS.heartFilled : SP_ICONS.heartOutline;
    }

    const likedCountEl = document.getElementById('kv-sp-liked-count');
    if (likedCountEl) {
      likedCountEl.textContent = `${this.likedTracks.size}`;
    }

    const menuLikedCountEl = document.getElementById('kv-sp-menu-liked-count');
    if (menuLikedCountEl) {
      menuLikedCountEl.textContent = `${this.likedTracks.size}`;
    }
  }

  updateLyricsContent(track) {
    const lyricsText = getFullTrackLyrics(track);
    const formatted = lyricsText
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\[(.*?)\]/g, '<span class="tag-highlight">[$1]</span>')
      .replace(/\n/g, '<br/>');

    const npLyricsEl = document.getElementById('kv-sp-np-lyrics');
    if (npLyricsEl) npLyricsEl.innerHTML = formatted;

    updateCenterLyricsStage(track);
  }

  updateNowPlayingPane(track) {
    if (!track) return;
    const coverEl = document.getElementById('kv-sp-np-cover');
    const titleEl = document.getElementById('kv-sp-np-title');
    const artistEl = document.getElementById('kv-sp-np-artist');
    const downloadEl = document.getElementById('kv-sp-np-download');
    const dnaWrap = document.getElementById('kv-sp-np-dna-tags');
    const dnaCountEl = document.getElementById('kv-sp-np-dna-count');

    if (coverEl) coverEl.src = track.image_url || '/favicon.png';
    if (titleEl) {
      titleEl.textContent = track.title || 'Untitled Track';
      titleEl.title = track.title || '';
    }
    if (artistEl) {
      const handle = (track.handle || 'futurescall').replace(/^@/, '');
      artistEl.textContent = `@${handle}`;
      artistEl.href = `https://suno.com/@${handle}`;
    }
    if (downloadEl) {
      downloadEl.href = `/api/suno-audio-proxy?input=${encodeURIComponent(track.id)}`;
      downloadEl.setAttribute('download', `${(track.title || track.id).replace(/[^a-z0-9-_]/gi, '_')}.m4a`);
    }

    if (dnaWrap) {
      const rawTags = track.metadata?.tags || 'Vietnamese folk trap, 808 sub bass, studio master';
      const tags = rawTags
        .split(/[,;\n]+/)
        .map(t => t.trim())
        .filter(t => t.length > 0 && t.length <= 42 && !t.includes('='))
        .slice(0, 8);

      if (dnaCountEl) {
        dnaCountEl.textContent = `${tags.length} tags`;
      }

      dnaWrap.innerHTML = tags
        .map(tag => `<button type="button" class="kv-sp-dna-pill" data-dna-tag="${tag.replace(/"/g, '&quot;')}">+ ${tag}</button>`)
        .join('');

      dnaWrap.querySelectorAll('.kv-sp-dna-pill').forEach(btn => {
        btn.addEventListener('click', () => {
          const tag = btn.getAttribute('data-dna-tag');
          if (tag) appendTagToPrompt(tag);
        });
      });
    }
  }

  updateActiveTrackInDOM() {
    if (!this.currentTrack) return;
    const activeId = this.currentTrack.id;

    document.querySelectorAll('[data-kv-track-id]').forEach(el => {
      const isCurrent = el.getAttribute('data-kv-track-id') === activeId;
      if (el.classList.contains('kv-sp-active-card') !== isCurrent) {
        el.classList.toggle('kv-sp-active-card', isCurrent);
      }

      const fab = el.querySelector('.kv-sp-card-play-fab');
      if (fab) {
        const desiredState = isCurrent && this.isPlaying ? 'pause' : 'play';
        if (fab.getAttribute('data-state') !== desiredState) {
          fab.setAttribute('data-state', desiredState);
          fab.innerHTML = desiredState === 'pause' ? SP_ICONS.pause : SP_ICONS.play;
        }
      }
    });
  }
}

window.kvPlayer = new KvMusicPlayer();

// ============================================================================
// 5. React DOM Bridge & Spotify Zen 4-Mode View Router
// ============================================================================
const TAB_INDEX_MAP = {
  Home: 0,
  SunoExplorer: 1,
  SunoSfx: 2,
  Guides: 3,
  Genres: 4,
  SFX: 5,
  Instruments: 6,
  Metatags: 7,
  Favorites: 8
};

let currentZenMode = 'songs';
let currentDiscoverSub = 'creators';
let currentWikiTab = 'Genres';
let currentCreatorHandle = 'futurescall';
let initialDiscoverActivated = false;

const ZEN_MODE_LABELS = {
  songs: 'Hot & Most Played Songs',
  creators: 'Creators Leaderboard',
  discover: 'All 32 Creators',
  studio: 'Prompt Studio',
  wiki: 'Sound Wiki',
  liked: 'Liked Songs'
};

const zenNavStack = [
  { mode: 'songs', options: {}, creatorHandle: 'futurescall', label: 'Hot & Most Played Songs' }
];
let zenNavIndex = 0;
let isApplyingHistoryState = false;

function buildHistoryLabel(mode, options = {}) {
  if (mode === 'discover') {
    if (options.creatorHandle) return `Discover (@${options.creatorHandle})`;
    if (options.discoverSub === 'showcase') return 'Featured Stream';
    return 'All 32 Creators';
  }
  if (mode === 'songs' && options.creator && options.creator !== 'all') {
    return `Songs (@${options.creator})`;
  }
  if (mode === 'wiki' && options.wikiTab) {
    return `Wiki • ${options.wikiTab}`;
  }
  return ZEN_MODE_LABELS[mode] || 'Hot & Most Played Songs';
}

function recordZenHistory(mode, options = {}) {
  if (isApplyingHistoryState || options._fromHistory) return;
  const entry = {
    mode,
    options: {
      discoverSub: options.discoverSub || currentDiscoverSub,
      wikiTab: options.wikiTab || currentWikiTab,
      creator: options.creator,
      sort: options.sort,
      style: options.style,
      genre: options.genre
    },
    creatorHandle: options.creatorHandle || currentCreatorHandle,
    label: buildHistoryLabel(mode, options)
  };

  const currentEntry = zenNavStack[zenNavIndex];
  if (
    currentEntry &&
    currentEntry.mode === entry.mode &&
    currentEntry.options?.discoverSub === entry.options?.discoverSub &&
    currentEntry.options?.wikiTab === entry.options?.wikiTab &&
    currentEntry.creatorHandle === entry.creatorHandle &&
    currentEntry.options?.creator === entry.options?.creator
  ) {
    updateZenHistoryButtonsUI();
    return;
  }

  zenNavStack.splice(zenNavIndex + 1);
  zenNavStack.push(entry);
  zenNavIndex = zenNavStack.length - 1;

  try {
    window.history.pushState({ zenNavIdx: zenNavIndex }, '', window.location.pathname + window.location.search);
  } catch (e) {}

  updateZenHistoryButtonsUI();
}

export function canGoBackZen() {
  return zenNavIndex > 0 || currentZenMode !== 'songs';
}

export function canGoForwardZen() {
  return zenNavIndex < zenNavStack.length - 1;
}

function applyZenHistoryEntry(entry) {
  if (!entry) return;
  isApplyingHistoryState = true;
  try {
    if (entry.creatorHandle) {
      currentCreatorHandle = entry.creatorHandle;
      document.querySelectorAll('.kv-sp-creator-item').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-creator-handle') === entry.creatorHandle);
      });
    }
    switchZenMode(entry.mode, { ...(entry.options || {}), _fromHistory: true });
    if (entry.mode === 'discover' && entry.creatorHandle && typeof window.kvSelectCreator === 'function') {
      setTimeout(() => {
        window.kvSelectCreator(entry.creatorHandle);
      }, 50);
    }
  } finally {
    isApplyingHistoryState = false;
    updateZenHistoryButtonsUI();
  }
}

export function goBackZen() {
  if (zenNavIndex > 0) {
    zenNavIndex -= 1;
    const prevEntry = zenNavStack[zenNavIndex];
    applyZenHistoryEntry(prevEntry);
    showToast(`Back to ${prevEntry.label}`);
    return;
  }
  if (currentZenMode !== 'songs') {
    switchZenMode('songs');
    showToast('Back to Hot & Most Played Songs');
  }
}

export function goForwardZen() {
  if (zenNavIndex < zenNavStack.length - 1) {
    zenNavIndex += 1;
    const nextEntry = zenNavStack[zenNavIndex];
    applyZenHistoryEntry(nextEntry);
    showToast(`Forward to ${nextEntry.label}`);
  }
}

export function updateZenHistoryButtonsUI() {
  const canBack = canGoBackZen();
  const canForward = canGoForwardZen();
  const prevEntry = zenNavIndex > 0 ? zenNavStack[zenNavIndex - 1] : { label: 'Hot & Most Played Songs' };
  const nextEntry = zenNavIndex < zenNavStack.length - 1 ? zenNavStack[zenNavIndex + 1] : null;

  const backBtn = document.getElementById('kv-sp-nav-back');
  const fwdBtn = document.getElementById('kv-sp-nav-forward');
  const backPill = document.getElementById('kv-sp-back-pill');
  const backPillLabel = document.getElementById('kv-sp-back-pill-label');

  if (backBtn) {
    backBtn.disabled = !canBack;
    backBtn.classList.toggle('disabled', !canBack);
    backBtn.title = canBack ? `Go Back to ${prevEntry.label} (Alt+Left)` : 'No previous page';
  }
  if (fwdBtn) {
    fwdBtn.disabled = !canForward;
    fwdBtn.classList.toggle('disabled', !canForward);
    fwdBtn.title = canForward && nextEntry ? `Go Forward to ${nextEntry.label} (Alt+Right)` : 'No forward page';
  }
  if (backPill) {
    backPill.disabled = !canBack;
    backPill.classList.toggle('disabled', !canBack);
    backPill.title = canBack ? `Return to ${prevEntry.label}` : 'Currently on Hot & Most Played Songs';
  }
  if (backPillLabel) {
    backPillLabel.textContent = canBack && zenNavIndex > 0 ? `Back • ${prevEntry.label}` : 'Go Back';
  }
}

window.kvGoBack = goBackZen;
window.kvGoForward = goForwardZen;
window.kvCanGoBack = canGoBackZen;
window.kvRecordZenHistory = recordZenHistory;
window.kvUpdateZenHistoryUI = updateZenHistoryButtonsUI;

if (typeof window !== 'undefined') {
  window.addEventListener('popstate', e => {
    const targetIdx = e.state?.zenNavIdx;
    if (typeof targetIdx === 'number' && zenNavStack[targetIdx]) {
      zenNavIndex = targetIdx;
      applyZenHistoryEntry(zenNavStack[zenNavIndex]);
    } else if (zenNavIndex > 0) {
      goBackZen();
    }
  });

  window.addEventListener('keydown', e => {
    if (e.altKey && e.key === 'ArrowLeft') {
      e.preventDefault();
      goBackZen();
    } else if (e.altKey && e.key === 'ArrowRight') {
      e.preventDefault();
      goForwardZen();
    }
  });
}

function clickUnderlyingReactTab(tabKey) {
  const headerTabBtns = document.querySelectorAll('#root > div > header div.border-t button');
  const targetIdx = TAB_INDEX_MAP[tabKey];
  if (targetIdx !== undefined && headerTabBtns[targetIdx]) {
    headerTabBtns[targetIdx].click();
    return true;
  }
  return false;
}

function ensureStudioComposerExpanded() {
  try {
    if (localStorage.getItem('suno-studio-hidden') !== 'false') {
      localStorage.setItem('suno-studio-hidden', 'false');
    }
  } catch (e) {}

  const collapsedBtn = document.querySelector('#root > div > div.fixed.bottom-3.right-3 button');
  if (collapsedBtn) {
    collapsedBtn.click();
  }

  setTimeout(() => {
    const studioDrawer = document.querySelector('#root > div > div.fixed.bottom-0');
    if (studioDrawer && !studioDrawer.querySelector('textarea')) {
      const headerBar = studioDrawer.querySelector('.bg-\\[\\#010409\\].cursor-pointer');
      if (headerBar) headerBar.click();
    }
    syncStudioPromptCounters();
  }, 40);
}

function syncTopStreamingBarUI(mode, discoverSub) {
  const activeKey =
    mode === 'songs'
      ? 'songs'
      : mode === 'creators'
        ? 'creators'
        : mode === 'discover' && discoverSub === 'showcase'
          ? 'discover-showcase'
          : mode === 'discover'
            ? 'discover-creators'
            : '';

  document.querySelectorAll('#kv-zen-discover-bar [data-top-tab]').forEach(pill => {
    pill.classList.toggle('active', pill.getAttribute('data-top-tab') === activeKey);
  });
}

export function switchZenMode(mode, options = {}) {
  currentZenMode = mode;
  document.body.setAttribute('data-zen-view', mode);

  const mainView = document.getElementById('kv-sp-main-view');
  if (mainView && typeof ensureZenCustomTabsMounted === 'function') {
    ensureZenCustomTabsMounted(mainView);
  }

  const likedView = document.getElementById('kv-sp-liked-playlist-view');
  const creatorsView = document.getElementById('kv-zen-creators-view');
  const songsView = document.getElementById('kv-zen-songs-view');
  const rootEl = document.getElementById('root');

  document.querySelectorAll('.kv-sp-lib-item[data-zen-mode], .kv-mobile-nav-item[data-zen-mode]').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-zen-mode') === mode);
  });

  const homeBtn = document.getElementById('kv-sp-home-btn');
  if (homeBtn) {
    homeBtn.classList.toggle('active', mode === 'songs' || mode === 'discover');
  }

  if (mode === 'creators') {
    if (likedView) likedView.classList.remove('active');
    if (rootEl) rootEl.style.display = 'none';
    if (songsView) {
      songsView.style.display = 'none';
      songsView.classList.remove('active');
    }
    if (creatorsView) {
      creatorsView.style.display = 'flex';
      creatorsView.classList.add('active');
    }
    if (typeof activateZenCreatorsTab === 'function') {
      activateZenCreatorsTab(options);
    }
  } else if (mode === 'songs') {
    if (likedView) likedView.classList.remove('active');
    if (rootEl) rootEl.style.display = 'none';
    if (creatorsView) {
      creatorsView.style.display = 'none';
      creatorsView.classList.remove('active');
    }
    if (songsView) {
      songsView.style.display = 'flex';
      songsView.classList.add('active');
    }
    if (typeof activateZenSongsTab === 'function') {
      activateZenSongsTab(options);
    }
  } else if (mode === 'liked') {
    if (creatorsView) {
      creatorsView.style.display = 'none';
      creatorsView.classList.remove('active');
    }
    if (songsView) {
      songsView.style.display = 'none';
      songsView.classList.remove('active');
    }
    if (likedView) likedView.classList.add('active');
    if (rootEl) rootEl.style.display = 'none';
    renderLikedSongsPlaylist();
  } else {
    if (creatorsView) {
      creatorsView.style.display = 'none';
      creatorsView.classList.remove('active');
    }
    if (songsView) {
      songsView.style.display = 'none';
      songsView.classList.remove('active');
    }
    if (likedView) likedView.classList.remove('active');
    if (rootEl) rootEl.style.display = '';
  }

  if (mode === 'discover') {
    const sub = options.discoverSub || currentDiscoverSub || 'creators';
    currentDiscoverSub = sub;
    clickUnderlyingReactTab(sub === 'showcase' ? 'Home' : 'SunoExplorer');
    if (sub === 'showcase') {
      setTimeout(() => {
        ensureShowcaseRichSections();
        hookHomeShowcaseCards();
      }, 80);
    }
  } else if (mode === 'studio') {
    ensureStudioComposerExpanded();
    clickUnderlyingReactTab('Home');
    setTimeout(() => {
      const firstTextarea = document.querySelector('#root > div > div.fixed.bottom-0 textarea');
      if (firstTextarea && options.focusTextarea) firstTextarea.focus();
    }, 80);
  } else if (mode === 'wiki') {
    const wikiTab = options.wikiTab || currentWikiTab || 'Genres';
    currentWikiTab = wikiTab;
    document.querySelectorAll('#kv-zen-wiki-bar [data-wiki-tab]').forEach(pill => {
      pill.classList.toggle('active', pill.getAttribute('data-wiki-tab') === wikiTab);
    });
    clickUnderlyingReactTab(wikiTab);
  }

  syncTopStreamingBarUI(mode, currentDiscoverSub);
  recordZenHistory(mode, options);

  if (mainView) {
    mainView.scrollTo({ top: 0, behavior: 'smooth' });
  }
}

window.kvSwitchZenMode = switchZenMode;

export function selectCreatorChannel(handle) {
  const clean = (handle || 'futurescall').replace(/^@/, '').trim();
  currentCreatorHandle = clean;
  document.querySelectorAll('.kv-sp-creator-item').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-creator-handle') === clean);
  });

  switchZenMode('discover', { discoverSub: 'creators', creatorHandle: clean });
  setTimeout(() => {
    if (typeof window.kvSelectCreator === 'function') {
      window.kvSelectCreator(clean);
      showToast(`Loaded @${clean} channel`);
    }
  }, 60);
}

window.kvSelectCreatorChannel = selectCreatorChannel;

function triggerReactHeaderAction(actionType) {
  const actionBtns = document.querySelectorAll('#root > div > header div.hidden.sm\\:flex button');
  if (actionType === 'Masterclasses' && actionBtns[0]) {
    actionBtns[0].click();
  } else if (actionType === 'Language' && actionBtns[1]) {
    actionBtns[1].click();
    setTimeout(() => {
      const langSpan = actionBtns[1]?.querySelector('span');
      const spLangEl = document.getElementById('kv-sp-lang-label');
      if (langSpan && spLangEl) {
        spLangEl.textContent = langSpan.textContent.trim();
      }
    }, 60);
  } else if (actionType === 'Presets' && actionBtns[2]) {
    actionBtns[2].click();
  } else if (actionType === 'HdAudio' && actionBtns[3]) {
    actionBtns[3].click();
  }
}

function syncSearchToReact(query) {
  if (currentZenMode === 'songs' || currentZenMode === 'creators') {
    const activeViewId = currentZenMode === 'songs' ? '#kv-zen-songs-view' : '#kv-zen-creators-view';
    const customInput = document.querySelector(`${activeViewId} input[type="search"], ${activeViewId} input[type="text"]`);
    if (customInput) {
      customInput.value = query;
      customInput.dispatchEvent(new Event('input', { bubbles: true }));
      return;
    }
  }

  if (currentZenMode === 'liked' || currentZenMode === 'studio') {
    switchZenMode('discover', { discoverSub: 'showcase' });
  }

  const reactInput = document.querySelector('#root > div > header input[type="text"]');
  if (!reactInput) return;

  const nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value')?.set;
  if (nativeSetter) {
    nativeSetter.call(reactInput, query);
  } else {
    reactInput.value = query;
  }
  reactInput.dispatchEvent(new Event('input', { bubbles: true }));
  reactInput.dispatchEvent(new Event('change', { bubbles: true }));
}

// ============================================================================
// 6. Liked Songs Virtual Playlist Renderer
// ============================================================================
function renderLikedSongsPlaylist() {
  const container = document.getElementById('kv-sp-liked-playlist-view');
  if (!container) return;

  const likedIds = [...(window.kvPlayer?.likedTracks || [])];
  const likedTracks = likedIds.map(id => {
    return (
      window.kvPlayer.likedTrackMeta[id] ||
      window.kvTrackRegistry[id] ||
      window.kvPlayer.queue.find(t => t.id === id) || {
        id,
        title: `Suno Track (${id.slice(0, 8)})`,
        image_url: '/favicon.png',
        handle: 'futurescall',
        metadata: { tags: 'Suno AI Studio Track' }
      }
    );
  });

  container.innerHTML = `
    <div class="kv-sp-playlist-hero">
      <div class="kv-sp-playlist-cover">
        <svg width="56" height="56" viewBox="0 0 24 24" fill="currentColor"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
      </div>
      <div class="kv-sp-playlist-info">
        <div style="font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em; color: var(--sp-text-subdued);">Saved Collection</div>
        <h1>Liked Songs</h1>
        <div style="font-size: 13px; color: var(--sp-text-subdued);">
          <span style="color: var(--sp-text-primary); font-weight: 700;">kv-suno</span> • <span class="kv-tabular">${likedTracks.length} songs</span>
        </div>
      </div>
    </div>

    ${
      likedTracks.length === 0
        ? `<div style="padding: 40px 24px; text-align: center; background: var(--sp-elevated); border-radius: 8px; color: var(--sp-text-subdued);">
             <p style="font-size: 15px; font-weight: 600; color: var(--sp-text-primary); margin: 0 0 6px;">No Liked Songs Yet</p>
             <p style="font-size: 13px; margin: 0 0 16px;">Click the heart icon in the player or Right Track Inspector to save your favorite reference tracks.</p>
             <button type="button" class="kv-sp-pill-btn primary" id="kv-sp-liked-find-btn">Explore Community Tracks</button>
           </div>`
        : `<div>
             <button type="button" id="kv-sp-liked-play-all" class="kv-btn-play-all" style="margin-bottom: 16px;">
               ${SP_ICONS.play}
               <span>Play All</span>
             </button>
             <table class="kv-sp-track-table">
               <thead>
                 <tr>
                   <th style="width: 44px;">#</th>
                   <th>Title & Lyrics Snippet</th>
                   <th>Creator</th>
                   <th style="width: 70px; text-align: right;">Like</th>
                 </tr>
               </thead>
               <tbody>
                 ${likedTracks
                   .map(
                     (t, idx) => `
                   <tr class="kv-sp-track-row" data-liked-idx="${idx}">
                     <td class="kv-tabular">${idx + 1}</td>
                     <td>
                       <div style="display: flex; align-items: center; gap: 12px;">
                         <img src="${t.image_url || '/favicon.png'}" alt="" style="width: 40px; height: 40px; border-radius: 4px; object-fit: cover;" />
                         <div style="min-width: 0;">
                           <div style="color: var(--sp-text-primary); font-weight: 600;">${t.title || 'Untitled'}</div>
                           <div style="font-size: 11px; color: var(--sp-text-muted); max-width: 420px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${getShortLyricsPreview(t)}</div>
                         </div>
                       </div>
                     </td>
                     <td>@${(t.handle || 'futurescall').replace(/^@/, '')}</td>
                     <td style="text-align: right;">
                       <button type="button" class="kv-player-like-btn liked" data-unlike-id="${t.id}" title="Remove">${SP_ICONS.heartFilled}</button>
                     </td>
                   </tr>
                 `
                   )
                   .join('')}
               </tbody>
             </table>
           </div>`
    }
  `;

  container.querySelector('#kv-sp-liked-play-all')?.addEventListener('click', () => {
    if (likedTracks.length > 0) window.kvPlayer.playTrack(likedTracks[0], likedTracks);
  });

  container.querySelector('#kv-sp-liked-find-btn')?.addEventListener('click', () => {
    switchZenMode('discover', { discoverSub: 'creators' });
  });

  container.querySelectorAll('.kv-sp-track-row').forEach(row => {
    row.addEventListener('click', e => {
      const unlikeBtn = e.target.closest('[data-unlike-id]');
      if (unlikeBtn) {
        e.stopPropagation();
        const id = unlikeBtn.getAttribute('data-unlike-id');
        window.kvPlayer.toggleLike({ id });
        return;
      }
      const idx = parseInt(row.getAttribute('data-liked-idx') || '0', 10);
      if (likedTracks[idx]) window.kvPlayer.playTrack(likedTracks[idx], likedTracks);
    });
  });

  updateZenHistoryButtonsUI();
}

// ============================================================================
// 7. Mount Spotify Zen Shell (#kv-sp-shell)
// ============================================================================
function mountSpotifyAppShell() {
  if (document.getElementById('kv-sp-shell')) return;
  const rootEl = document.getElementById('root');
  if (!rootEl) return;

  document.body.setAttribute('data-zen-view', 'songs');

  const shell = document.createElement('div');
  shell.id = 'kv-sp-shell';

  // 1. Clean Top Bar (#kv-sp-topbar) with Back / Forward History Controls
  const topbar = document.createElement('header');
  topbar.id = 'kv-sp-topbar';
  topbar.innerHTML = `
    <div class="kv-sp-topbar-left">
      <div class="kv-sp-brand" id="kv-sp-brand-home" title="kv-suno Zen Stream">
        <div class="kv-sp-brand-logo">${SP_ICONS.brandWave}</div>
        <span class="kv-sp-brand-name">kv-suno</span>
      </div>

      <div class="kv-sp-history-nav" role="group" aria-label="Navigation History">
        <button type="button" class="kv-sp-history-btn disabled" id="kv-sp-nav-back" disabled title="No previous page">
          ${SP_ICONS.chevronLeft}
        </button>
        <button type="button" class="kv-sp-history-btn disabled" id="kv-sp-nav-forward" disabled title="No forward page">
          ${SP_ICONS.chevronRight}
        </button>
        <button type="button" class="kv-sp-back-pill disabled" id="kv-sp-back-pill" disabled title="Return to previous view">
          ${SP_ICONS.arrowLeft}
          <span id="kv-sp-back-pill-label">Go Back</span>
        </button>
      </div>
    </div>

    <div class="kv-sp-topbar-center">
      <button type="button" class="kv-sp-home-fab active" id="kv-sp-home-btn" title="Hot & Most Played Songs (Home)">${SP_ICONS.home}</button>
      <div class="kv-sp-search-omnibox">
        <span class="kv-sp-search-icon">${SP_ICONS.search}</span>
        <input id="kv-sp-search-input" type="text" placeholder="Search tracks, @creators, lyrics, or acoustic tags..." autocomplete="off" />
        <button type="button" class="kv-sp-search-clear" id="kv-sp-search-clear" title="Clear">${SP_ICONS.close}</button>
      </div>
    </div>

    <div class="kv-sp-topbar-right">
      <button type="button" class="kv-sp-pill-btn primary" id="kv-sp-open-studio" title="Open Distraction-Free Prompt & Lyrics Studio">
        ${SP_ICONS.sparkles}
        <span>✦ Prompt Studio</span>
        <span id="kv-sp-studio-count" class="kv-sp-studio-badge kv-tabular">0</span>
      </button>

      <div class="kv-sp-tools-dropdown" id="kv-sp-tools-dropdown">
        <button type="button" class="kv-sp-pill-btn" id="kv-sp-tools-menu-btn" title="Audio DSP, Saved Presets & Language">
          ${SP_ICONS.sliders}
          <span>Studio Tools</span>
          ${SP_ICONS.chevronDown}
        </button>
        <div class="kv-sp-tools-popover" id="kv-sp-tools-popover">
          <button type="button" class="kv-sp-tools-item" id="kv-sp-open-liked">
            <span class="kv-sp-tools-item-left">${SP_ICONS.heartFilled}<span>Liked Songs</span></span>
            <span id="kv-sp-menu-liked-count" class="kv-sp-tools-item-tag">${window.kvPlayer?.likedTracks?.size || 0}</span>
          </button>
          <button type="button" class="kv-sp-tools-item" id="kv-sp-pwa-bg-status">
            <span class="kv-sp-tools-item-left">${SP_ICONS.brandWave}<span>Lock-Screen &amp; BG Play</span></span>
            <span class="kv-sp-tools-item-tag" style="color: var(--sp-green);">ON</span>
          </button>
          <button type="button" class="kv-sp-tools-item" id="kv-sp-install-pwa" style="display: none;">
            <span class="kv-sp-tools-item-left">${SP_ICONS.download}<span>Install PWA App</span></span>
            <span class="kv-sp-tools-item-tag" style="color: var(--sp-green);">PWA</span>
          </button>
          <button type="button" class="kv-sp-tools-item" id="kv-sp-open-dsp">
            <span class="kv-sp-tools-item-left">${SP_ICONS.sliders}<span>HD Audio DSP</span></span>
            <span class="kv-sp-tools-item-tag">24-bit</span>
          </button>
          <button type="button" class="kv-sp-tools-item" id="kv-sp-open-presets">
            <span class="kv-sp-tools-item-left">${SP_ICONS.folder}<span>Saved Presets</span></span>
          </button>
          <button type="button" class="kv-sp-tools-item" id="kv-sp-toggle-lang">
            <span class="kv-sp-tools-item-left">${SP_ICONS.globe}<span>Language</span></span>
            <span id="kv-sp-lang-label" class="kv-sp-tools-item-tag">VI</span>
          </button>
        </div>
      </div>
    </div>
  `;

  // 2. 3-Pane Workspace
  const isDesktopWide = window.innerWidth >= 1100;
  const workspace = document.createElement('div');
  workspace.id = 'kv-sp-workspace';
  if (!isDesktopWide) {
    workspace.className = 'right-closed';
  }

  // 2A. Full Left Navigation Rail (6 Zen Modes + 12 Pinned Creators + Always-On Prompt Cart)
  const leftSidebar = document.createElement('aside');
  leftSidebar.id = 'kv-sp-left-sidebar';
  leftSidebar.setAttribute('aria-label', 'Workspace Modes & Pinned Creators');
  leftSidebar.innerHTML = `
    <div class="kv-sp-lib-header">
      <button type="button" class="kv-sp-lib-title-btn" id="kv-sp-collapse-lib" title="Collapse or Expand Navigation Rail">
        ${SP_ICONS.library}
        <span class="kv-sp-lib-title-text">Workspace</span>
      </button>
      <div class="kv-sp-lib-actions">
        <button type="button" class="kv-sp-circle-btn" id="kv-sp-new-prompt-btn" title="Open Prompt Studio">${SP_ICONS.plus}</button>
      </div>
    </div>

    <div id="kv-sp-left-list">
      <div class="kv-sp-lib-section-label">Zen Modes</div>

      <button type="button" class="kv-sp-lib-item active" data-zen-mode="songs">
        <div class="kv-sp-lib-icon rose-tint">${SP_ICONS.flame}</div>
        <div class="kv-sp-lib-text-col">
          <span class="kv-sp-lib-name">Songs</span>
          <span class="kv-sp-lib-sub">Hot & Most Played Chart</span>
        </div>
        <span class="kv-sp-lib-badge hot-badge">HOT</span>
      </button>

      <button type="button" class="kv-sp-lib-item" data-zen-mode="creators">
        <div class="kv-sp-lib-icon amber-tint">${SP_ICONS.crown}</div>
        <div class="kv-sp-lib-text-col">
          <span class="kv-sp-lib-name">Creators</span>
          <span class="kv-sp-lib-sub">Hero & Hit Rankings</span>
        </div>
        <span class="kv-sp-lib-badge kv-tabular">32</span>
      </button>

      <button type="button" class="kv-sp-lib-item" data-zen-mode="discover">
        <div class="kv-sp-lib-icon green-tint">${SP_ICONS.browse}</div>
        <div class="kv-sp-lib-text-col">
          <span class="kv-sp-lib-name">Discover</span>
          <span class="kv-sp-lib-sub">Explorer & DNA Radio</span>
        </div>
      </button>

      <button type="button" class="kv-sp-lib-item" data-zen-mode="studio">
        <div class="kv-sp-lib-icon purple-tint">${SP_ICONS.sparkles}</div>
        <div class="kv-sp-lib-text-col">
          <span class="kv-sp-lib-name">Prompt Studio</span>
          <span class="kv-sp-lib-sub">Composer & 2D Matrix</span>
        </div>
      </button>

      <button type="button" class="kv-sp-lib-item" data-zen-mode="wiki">
        <div class="kv-sp-lib-icon cyan-tint">${SP_ICONS.book}</div>
        <div class="kv-sp-lib-text-col">
          <span class="kv-sp-lib-name">Sound Wiki</span>
          <span class="kv-sp-lib-sub">Genres, SFX & Guides</span>
        </div>
        <span class="kv-sp-lib-badge kv-tabular">200+</span>
      </button>

      <button type="button" class="kv-sp-lib-item" data-zen-mode="liked">
        <div class="kv-sp-lib-icon liked-gradient">${SP_ICONS.heartFilled}</div>
        <div class="kv-sp-lib-text-col">
          <span class="kv-sp-lib-name">Liked Songs</span>
          <span class="kv-sp-lib-sub">Saved Reference Tracks</span>
        </div>
        <span id="kv-sp-liked-count" class="kv-sp-lib-badge kv-tabular">${window.kvPlayer?.likedTracks?.size || 0}</span>
      </button>

      <!-- Pinned Vietnamese Community Creators List (Fills Left Sidebar) -->
      <div id="kv-sp-top-creators-header" class="kv-sp-lib-section-label kv-sp-creators-header-link" title="Open Creators Leaderboard & Hero Rankings" style="margin-top: 10px; display: flex; align-items: center; justify-content: space-between; cursor: pointer;">
        <span>Top Creators</span>
        <span id="kv-sp-channels-badge" style="font-size: 9px; color: var(--sp-green); cursor: pointer;">32 Channels</span>
      </div>

      <div id="kv-sp-left-creators" class="kv-sp-creators-rail">
        ${PINNED_CREATORS.map(
          (cr, idx) => `
          <button type="button" class="kv-sp-creator-item ${idx === 0 ? 'active' : ''}" data-creator-handle="${cr.handle}" title="Stream @${cr.handle} (${cr.style})">
            <div class="kv-sp-creator-avatar" style="background: ${cr.color}22; color: ${cr.color}; border: 1px solid ${cr.color}44;">
              ${cr.name.replace(/[^a-zA-Z0-9À-ỹ]/g, '').charAt(0).toUpperCase() || 'S'}
            </div>
            <div class="kv-sp-lib-text-col">
              <span class="kv-sp-creator-name">${cr.name}</span>
              <span class="kv-sp-lib-sub">@${cr.handle} • ${cr.style}</span>
            </div>
            <span class="kv-sp-creator-count kv-tabular">${cr.count}</span>
          </button>
        `
        ).join('')}
      </div>
    </div>

    <!-- Always-Visible Bottom Prompt Cart & Quick Starter Tags -->
    <div id="kv-sp-left-studio-summary">
      <div class="kv-sp-studio-sum-header">
        <span class="kv-sp-studio-summary-title">✦ Studio Prompt</span>
        <span id="kv-sp-left-studio-count" class="kv-sp-studio-summary-badge kv-tabular">0 tags</span>
      </div>
      <div id="kv-sp-left-studio-preview" class="kv-sp-studio-sum-preview empty">Click a quick tag or track DNA to build your prompt...</div>
      <div class="kv-sp-quick-tags-row" id="kv-sp-left-quick-tags">
        <button type="button" class="kv-sp-mini-tag" data-quick-tag="Vietnamese V-Pop Ballad, emotional vocal">+ V-Pop</button>
        <button type="button" class="kv-sp-mini-tag" data-quick-tag="Vietnamese folk trap, 808 sub bass, dan tranh">+ Folk Trap</button>
        <button type="button" class="kv-sp-mini-tag" data-quick-tag="Sapa Lo-Fi chillhop, warm vinyl, acoustic guitar">+ Lo-Fi</button>
      </div>
      <div class="kv-sp-studio-sum-actions">
        <button type="button" class="kv-sp-studio-sum-btn primary" id="kv-sp-left-open-studio">Studio →</button>
        <button type="button" class="kv-sp-studio-sum-btn" id="kv-sp-left-copy-prompt">Copy</button>
      </div>
    </div>
  `;

  // 2B. Center Mode-Based Main Canvas (#kv-sp-main-view)
  const mainView = document.createElement('div');
  mainView.id = 'kv-sp-main-view';
  mainView.innerHTML = `
    <div id="kv-sp-ambient-wash"></div>

    <!-- Unified Streaming Top Tab Bar (Visible across Songs, Creators, and Discover) -->
    <div id="kv-zen-discover-bar" class="kv-zen-subbar">
      <div class="kv-zen-subbar-pills" role="tablist" aria-label="Streaming Views">
        <button type="button" class="kv-zen-sub-pill active" id="kv-zen-jump-songs" data-top-tab="songs">
          <span>🔥 Hot &amp; Most Played Songs</span>
        </button>
        <button type="button" class="kv-zen-sub-pill" id="kv-zen-jump-creators" data-top-tab="creators">
          <span>👑 Creators Leaderboard</span>
        </button>
        <button type="button" class="kv-zen-sub-pill" data-discover-sub="creators" data-top-tab="discover-creators">
          ${SP_ICONS.browse}
          <span>All 32 Community Creators</span>
        </button>
        <button type="button" class="kv-zen-sub-pill" data-discover-sub="showcase" data-top-tab="discover-showcase">
          ${SP_ICONS.disc}
          <span>Featured 20-Track Stream &amp; Lyrics</span>
        </button>
      </div>
      <button type="button" class="kv-btn-play-all" id="kv-zen-discover-play-all">
        ${SP_ICONS.play}
        <span>Play Stream</span>
      </button>
    </div>

    <!-- Sound Wiki Category Bar -->
    <div id="kv-zen-wiki-bar" class="kv-zen-subbar">
      <div class="kv-zen-subbar-pills">
        <button type="button" class="kv-zen-sub-pill active" data-wiki-tab="Genres">${SP_ICONS.disc}<span>Genres</span></button>
        <button type="button" class="kv-zen-sub-pill" data-wiki-tab="Instruments">${SP_ICONS.guitar}<span>Instruments</span></button>
        <button type="button" class="kv-zen-sub-pill" data-wiki-tab="SFX">${SP_ICONS.sliders}<span>SFX & Mastering</span></button>
        <button type="button" class="kv-zen-sub-pill" data-wiki-tab="SunoSfx">${SP_ICONS.brandWave}<span>3D Audio Lab</span></button>
        <button type="button" class="kv-zen-sub-pill" data-wiki-tab="Metatags">${SP_ICONS.code}<span>Metatags</span></button>
        <button type="button" class="kv-zen-sub-pill" data-wiki-tab="Guides">${SP_ICONS.book}<span>55 Masterclasses</span></button>
        <button type="button" class="kv-zen-sub-pill" data-wiki-tab="Favorites">${SP_ICONS.star}<span>Saved Tags</span></button>
      </div>
    </div>

    <div id="kv-sp-liked-playlist-view"></div>
  `;

  // 2C. Single Unified Right "Now Playing DNA & Lyrics" Inspector (#kv-sp-right-sidebar)
  const track = window.kvPlayer?.currentTrack || DEFAULT_TRACK;
  const rightSidebar = document.createElement('aside');
  rightSidebar.id = 'kv-sp-right-sidebar';
  rightSidebar.setAttribute('aria-label', 'Now Playing Track DNA & Lyrics Inspector');
  rightSidebar.innerHTML = `
    <div class="kv-sp-np-header">
      <span class="kv-sp-np-source"><span class="kv-sp-np-live-dot"></span><span>Now Playing DNA & Lyrics</span></span>
      <button type="button" class="kv-sp-circle-btn" id="kv-sp-close-right" title="Close Inspector">${SP_ICONS.close}</button>
    </div>

    <div id="kv-sp-right-body">
      <!-- Compact 56x56 Horizontal Track Header Card -->
      <div class="kv-sp-np-compact-track">
        <div class="kv-sp-np-compact-left">
          <div class="kv-sp-np-cover-wrap">
            <img id="kv-sp-np-cover" class="kv-sp-np-cover" src="${track.image_url || '/favicon.png'}" alt="${track.title || ''}" />
          </div>
          <div class="kv-sp-np-meta">
            <h2 id="kv-sp-np-title" class="kv-sp-np-title">${track.title || 'Duyên Lỡ .. Lỡ Duyên'}</h2>
            <a id="kv-sp-np-artist" class="kv-sp-np-artist" href="https://suno.com/@${track.handle || 'futurescall'}" target="_blank" rel="noreferrer">@${track.handle || 'futurescall'}</a>
          </div>
        </div>
        <div class="kv-sp-np-actions-row">
          <button type="button" id="kv-sp-np-like" class="kv-player-like-btn" title="Save to Liked Songs">${SP_ICONS.heartOutline}</button>
          <a id="kv-sp-np-download" class="kv-sp-circle-btn" href="/api/suno-audio-proxy?input=${track.id}" download="${track.id}.m4a" title="Download .m4a Audio">${SP_ICONS.download}</a>
        </div>
      </div>

      <!-- Style DNA Tags Card -->
      <div class="kv-sp-np-card">
        <div class="kv-sp-np-card-header">
          <h3 class="kv-sp-np-card-title">Style DNA Tags</h3>
          <span id="kv-sp-np-dna-count" class="kv-sp-studio-sum-count kv-tabular">6 tags</span>
        </div>
        <div id="kv-sp-np-dna-tags" class="kv-sp-dna-tags"></div>
        <button type="button" class="kv-sp-np-use-prompt-btn" id="kv-sp-np-use-all-btn">
          ${SP_ICONS.sparkles}
          <span>+ Load Full Style into Studio</span>
        </button>
      </div>

      <!-- Lyrics & Structure Card (Expanded to fill remaining height) -->
      <div class="kv-sp-np-card kv-sp-np-lyrics-card">
        <div class="kv-sp-np-card-header">
          <h3 class="kv-sp-np-card-title">🎤 Lyrics & Structure</h3>
          <div style="display: flex; align-items: center; gap: 6px;">
            <button type="button" class="kv-sp-np-mini-btn" id="kv-sp-np-copy-lyrics">Copy Lyrics</button>
          </div>
        </div>
        <div id="kv-sp-np-lyrics" class="kv-sp-np-lyrics-box kv-sp-scrollable"></div>
      </div>
    </div>
  `;

  rootEl.parentNode.insertBefore(shell, rootEl);
  mainView.appendChild(rootEl);
  if (typeof ensureZenCustomTabsMounted === 'function') {
    ensureZenCustomTabsMounted(mainView);
  }
  workspace.appendChild(leftSidebar);
  workspace.appendChild(mainView);
  workspace.appendChild(rightSidebar);

  // Mobile Bottom Sheet Backdrop (for Now Playing DNA & Lyrics Inspector)
  const mobileBackdrop = document.createElement('div');
  mobileBackdrop.id = 'kv-sp-mobile-backdrop';
  mobileBackdrop.setAttribute('aria-hidden', 'true');
  mobileBackdrop.addEventListener('click', () => {
    window.kvPlayer?.toggleLyrics(false);
  });

  // Mobile Bottom Navigation Bar (5 thumb-friendly primary tabs)
  const mobileNav = document.createElement('nav');
  mobileNav.id = 'kv-mobile-bottom-nav';
  mobileNav.setAttribute('aria-label', 'Mobile Primary Navigation');
  mobileNav.innerHTML = `
    <button type="button" class="kv-mobile-nav-item active" data-zen-mode="songs">
      <span class="kv-mobile-nav-icon">${SP_ICONS.flame}</span>
      <span class="kv-mobile-nav-label">Hot Songs</span>
    </button>
    <button type="button" class="kv-mobile-nav-item" data-zen-mode="creators">
      <span class="kv-mobile-nav-icon">${SP_ICONS.crown}</span>
      <span class="kv-mobile-nav-label">Creators</span>
    </button>
    <button type="button" class="kv-mobile-nav-item" data-zen-mode="discover">
      <span class="kv-mobile-nav-icon">${SP_ICONS.browse}</span>
      <span class="kv-mobile-nav-label">Discover</span>
    </button>
    <button type="button" class="kv-mobile-nav-item" data-zen-mode="studio">
      <span class="kv-mobile-nav-icon">${SP_ICONS.sparkles}</span>
      <span class="kv-mobile-nav-label">Studio</span>
      <span id="kv-mobile-studio-badge" class="kv-mobile-nav-badge kv-tabular">0</span>
    </button>
    <button type="button" class="kv-mobile-nav-item" data-zen-mode="wiki">
      <span class="kv-mobile-nav-icon">${SP_ICONS.book}</span>
      <span class="kv-mobile-nav-label">Wiki</span>
    </button>
  `;

  shell.appendChild(topbar);
  shell.appendChild(workspace);
  shell.appendChild(mobileBackdrop);
  shell.appendChild(mobileNav);

  // Topbar listeners (Brand, Home, Back, Forward, Studio)
  document.getElementById('kv-sp-brand-home')?.addEventListener('click', () => switchZenMode('songs'));
  document.getElementById('kv-sp-home-btn')?.addEventListener('click', () => switchZenMode('songs'));
  document.getElementById('kv-sp-nav-back')?.addEventListener('click', () => goBackZen());
  document.getElementById('kv-sp-nav-forward')?.addEventListener('click', () => goForwardZen());
  document.getElementById('kv-sp-back-pill')?.addEventListener('click', () => goBackZen());
  document.getElementById('kv-sp-open-studio')?.addEventListener('click', () => switchZenMode('studio', { focusTextarea: true }));

  // Consolidated Studio Tools Dropdown
  const toolsDropdown = document.getElementById('kv-sp-tools-dropdown');
  const toolsMenuBtn = document.getElementById('kv-sp-tools-menu-btn');
  toolsMenuBtn?.addEventListener('click', e => {
    e.stopPropagation();
    toolsDropdown?.classList.toggle('open');
  });
  document.addEventListener('click', e => {
    if (toolsDropdown && !toolsDropdown.contains(e.target)) {
      toolsDropdown.classList.remove('open');
    }
  });

  document.getElementById('kv-sp-open-liked')?.addEventListener('click', () => {
    toolsDropdown?.classList.remove('open');
    switchZenMode('liked');
  });
  document.getElementById('kv-sp-pwa-bg-status')?.addEventListener('click', () => {
    toolsDropdown?.classList.remove('open');
    if (window.kvPlayer?.currentTrack) {
      window.kvPlayer.ensureBackgroundAudioKeepAlive(true);
      window.kvPlayer.updateMediaSession(window.kvPlayer.currentTrack);
    }
    showToast('Lock-Screen Media Control & Background Play Active');
  });
  const installPwaBtn = document.getElementById('kv-sp-install-pwa');
  if (window._kvDeferredPwaPrompt && installPwaBtn) {
    installPwaBtn.style.display = 'flex';
  }
  installPwaBtn?.addEventListener('click', async () => {
    toolsDropdown?.classList.remove('open');
    const promptEvent = window._kvDeferredPwaPrompt;
    if (promptEvent && typeof promptEvent.prompt === 'function') {
      promptEvent.prompt();
      try {
        await promptEvent.userChoice;
      } catch (e) {}
      window._kvDeferredPwaPrompt = null;
      installPwaBtn.style.display = 'none';
    } else {
      showToast('Use browser menu → "Add to Home Screen" / "Install App"');
    }
  });
  document.getElementById('kv-sp-open-dsp')?.addEventListener('click', () => {
    toolsDropdown?.classList.remove('open');
    triggerReactHeaderAction('HdAudio');
  });
  document.getElementById('kv-sp-open-presets')?.addEventListener('click', () => {
    toolsDropdown?.classList.remove('open');
    triggerReactHeaderAction('Presets');
  });
  document.getElementById('kv-sp-toggle-lang')?.addEventListener('click', () => {
    triggerReactHeaderAction('Language');
  });

  // Search Omnibox
  const searchInput = document.getElementById('kv-sp-search-input');
  const clearBtn = document.getElementById('kv-sp-search-clear');
  if (searchInput) {
    searchInput.addEventListener('input', e => {
      const val = e.target.value;
      if (clearBtn) clearBtn.style.display = val ? 'inline-flex' : 'none';
      syncSearchToReact(val);
    });
  }
  if (clearBtn && searchInput) {
    clearBtn.addEventListener('click', () => {
      searchInput.value = '';
      clearBtn.style.display = 'none';
      syncSearchToReact('');
    });
  }

  // Left Sidebar Collapse & 6-Mode Navigation
  document.getElementById('kv-sp-collapse-lib')?.addEventListener('click', () => {
    workspace.classList.toggle('left-collapsed');
  });
  document.getElementById('kv-sp-new-prompt-btn')?.addEventListener('click', () => switchZenMode('studio', { focusTextarea: true }));

  shell.querySelectorAll('[data-zen-mode]').forEach(el => {
    el.addEventListener('click', () => {
      const mode = el.getAttribute('data-zen-mode');
      if (mode) switchZenMode(mode);
    });
  });

  // Left Sidebar "Top Creators" / "32 Channels" Header Click -> Creators Leaderboard
  document.getElementById('kv-sp-top-creators-header')?.addEventListener('click', () => {
    switchZenMode('creators');
  });
  document.getElementById('kv-sp-channels-badge')?.addEventListener('click', e => {
    e.stopPropagation();
    switchZenMode('creators');
  });

  // Left Sidebar Pinned Creators Click Listeners
  shell.querySelectorAll('.kv-sp-creator-item[data-creator-handle]').forEach(btn => {
    btn.addEventListener('click', () => {
      const handle = btn.getAttribute('data-creator-handle');
      if (handle) selectCreatorChannel(handle);
    });
  });

  // Left Sidebar Quick Starter Tag Chips
  shell.querySelectorAll('[data-quick-tag]').forEach(btn => {
    btn.addEventListener('click', () => {
      const tag = btn.getAttribute('data-quick-tag');
      if (tag) appendTagToPrompt(tag);
    });
  });

  // Left Sidebar Active Studio Prompt Sub-Panel Actions
  document.getElementById('kv-sp-left-open-studio')?.addEventListener('click', () => {
    switchZenMode('studio', { focusTextarea: true });
  });
  document.getElementById('kv-sp-left-copy-prompt')?.addEventListener('click', () => {
    const raw = getActiveStudioPromptValue();
    if (raw) {
      navigator.clipboard?.writeText(raw);
    } else {
      showToast('Prompt Studio is empty — click a + tag first');
    }
  });

  // Unified Streaming Top Tab Bar Listeners
  shell.querySelectorAll('#kv-zen-discover-bar [data-discover-sub]').forEach(btn => {
    btn.addEventListener('click', () => {
      const sub = btn.getAttribute('data-discover-sub');
      if (sub) switchZenMode('discover', { discoverSub: sub });
    });
  });
  document.getElementById('kv-zen-jump-songs')?.addEventListener('click', () => {
    switchZenMode('songs');
  });
  document.getElementById('kv-zen-jump-creators')?.addEventListener('click', () => {
    switchZenMode('creators');
  });
  document.getElementById('kv-zen-discover-play-all')?.addEventListener('click', () => {
    window.kvPlayer.togglePlay();
  });

  // Sound Wiki Category Bar Listeners
  shell.querySelectorAll('#kv-zen-wiki-bar [data-wiki-tab]').forEach(btn => {
    btn.addEventListener('click', () => {
      const tab = btn.getAttribute('data-wiki-tab');
      if (tab) switchZenMode('wiki', { wikiTab: tab });
    });
  });

  // Right Inspector Controls
  document.getElementById('kv-sp-close-right')?.addEventListener('click', () => {
    window.kvPlayer.toggleLyrics(false);
  });
  document.getElementById('kv-sp-np-like')?.addEventListener('click', () => {
    window.kvPlayer.toggleLike();
  });
  document.getElementById('kv-sp-np-use-all-btn')?.addEventListener('click', () => {
    const cur = window.kvPlayer.currentTrack;
    if (cur) {
      const tags = cur.metadata?.tags || cur.title || '';
      injectPromptIntoTextarea(tags);
      switchZenMode('studio', { focusTextarea: true });
    }
  });
  document.getElementById('kv-sp-np-copy-lyrics')?.addEventListener('click', () => {
    const cur = window.kvPlayer.currentTrack;
    const rawLyrics = getFullTrackLyrics(cur);
    if (rawLyrics.trim()) {
      navigator.clipboard?.writeText(rawLyrics);
    } else {
      showToast('No indexed lyrics for this track');
    }
  });

  window.kvPlayer.updateNowPlayingPane(track);
  window.kvPlayer.updateLyricsContent(track);
  updateAmbientColorFromTrack(track);

  // Immediately activate "Hot & Most Played Songs" as the default landing view
  switchZenMode('songs', { _fromHistory: true });
  updateZenHistoryButtonsUI();
}

// ============================================================================
// 8. Center Showcase Live Lyrics Stage & Multi-Creator Hub (#block-suno-api)
// ============================================================================
function updateCenterLyricsStage(track) {
  const stage = document.getElementById('kv-center-lyrics-stage');
  if (!stage || !track) return;

  const coverEl = stage.querySelector('#kv-stage-cover');
  const titleEl = stage.querySelector('#kv-stage-title');
  const handleEl = stage.querySelector('#kv-stage-handle');
  const tagsEl = stage.querySelector('#kv-stage-tags');
  const lyricsEl = stage.querySelector('#kv-stage-lyrics');

  if (coverEl) coverEl.src = track.image_url || '/favicon.png';
  if (titleEl) titleEl.textContent = track.title || 'Untitled Track';
  if (handleEl) {
    const h = (track.handle || 'futurescall').replace(/^@/, '');
    handleEl.textContent = `@${h}`;
  }
  if (tagsEl) {
    const rawTags = track.metadata?.tags || 'Vietnamese V-Pop, Studio Master';
    const tags = rawTags
      .split(/[,;\n]+/)
      .map(t => t.trim())
      .filter(t => t && !t.includes('='))
      .slice(0, 6);
    tagsEl.innerHTML = tags
      .map(tg => `<span class="kv-stage-tag-chip">${tg}</span>`)
      .join('');
  }
  if (lyricsEl) {
    const fullLyrics = getFullTrackLyrics(track);
    lyricsEl.innerHTML = fullLyrics
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/\[(.*?)\]/g, '<span class="tag-highlight">[$1]</span>')
      .replace(/\n/g, '<br/>');
  }
}

function ensureShowcaseRichSections() {
  const showcaseContainer = document.getElementById('block-suno-api');
  if (!showcaseContainer) return;

  // 1. Top Live Lyrics & Active Track Stage Banner inside Featured Showcase
  if (!document.getElementById('kv-center-lyrics-stage')) {
    const cur = window.kvPlayer?.currentTrack || DEFAULT_TRACK;
    const stageEl = document.createElement('div');
    stageEl.id = 'kv-center-lyrics-stage';
    stageEl.className = 'kv-center-lyrics-stage';
    stageEl.innerHTML = `
      <div class="kv-stage-left">
        <div class="kv-stage-track-row">
          <img id="kv-stage-cover" src="${cur.image_url || '/favicon.png'}" alt="" class="kv-stage-cover" />
          <div style="min-width: 0; flex: 1;">
            <span class="kv-stage-badge">🎤 NOW PLAYING & FULL LYRICS</span>
            <h3 id="kv-stage-title" class="kv-stage-title">${cur.title || 'Duyên Lỡ .. Lỡ Duyên'}</h3>
            <p id="kv-stage-handle" class="kv-stage-handle">@${(cur.handle || 'futurescall').replace(/^@/, '')}</p>
          </div>
        </div>
        <div id="kv-stage-tags" class="kv-stage-tags"></div>
        <div class="kv-stage-actions">
          <button type="button" class="kv-sp-pill-btn primary" id="kv-stage-play-btn">
            ${SP_ICONS.play}
            <span>Play / Pause</span>
          </button>
          <button type="button" class="kv-sp-pill-btn" id="kv-stage-copy-lyrics-btn">
            <span>Copy Full Lyrics</span>
          </button>
        </div>
      </div>
      <div class="kv-stage-right">
        <div class="kv-stage-lyrics-header">
          <span>SONG LYRICS & VOCAL STRUCTURE</span>
          <span style="color: var(--sp-green); font-size: 10px;">Click any track card below to switch lyrics</span>
        </div>
        <div id="kv-stage-lyrics" class="kv-stage-lyrics-body kv-sp-scrollable"></div>
      </div>
    `;

    // Insert before the track grid inside #block-suno-api
    const gridEl = showcaseContainer.querySelector('.grid');
    if (gridEl && gridEl.parentNode) {
      gridEl.parentNode.insertBefore(stageEl, gridEl);
    } else {
      showcaseContainer.prepend(stageEl);
    }

    stageEl.querySelector('#kv-stage-play-btn')?.addEventListener('click', () => {
      window.kvPlayer.togglePlay();
    });
    stageEl.querySelector('#kv-stage-copy-lyrics-btn')?.addEventListener('click', () => {
      const text = getFullTrackLyrics(window.kvPlayer.currentTrack);
      if (text) navigator.clipboard?.writeText(text);
    });

    updateCenterLyricsStage(cur);
  }

  // 2. Bottom Community Creator Channels Quick-Jump Grid inside #block-suno-api
  if (!document.getElementById('kv-showcase-creator-hub')) {
    const hubEl = document.createElement('div');
    hubEl.id = 'kv-showcase-creator-hub';
    hubEl.className = 'kv-showcase-creator-hub';
    hubEl.innerHTML = `
      <div class="kv-hub-header">
        <div>
          <h3 class="kv-hub-title">Explore All 32 Vietnamese Suno Channels</h3>
          <p class="kv-hub-sub">Jump directly to any community creator to stream their full discography, style prompts & lyrics</p>
        </div>
      </div>
      <div class="kv-hub-grid">
        ${PINNED_CREATORS.map(
          cr => `
          <button type="button" class="kv-hub-creator-card" data-hub-creator="${cr.handle}">
            <div class="kv-sp-creator-avatar" style="background: ${cr.color}22; color: ${cr.color}; border: 1px solid ${cr.color}44;">
              ${cr.name.replace(/[^a-zA-Z0-9À-ỹ]/g, '').charAt(0).toUpperCase() || 'S'}
            </div>
            <div style="min-width: 0; flex: 1; text-align: left;">
              <div style="font-size: 13px; font-weight: 700; color: #fff; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">${cr.name}</div>
              <div style="font-size: 11px; color: var(--sp-text-subdued); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">@${cr.handle} • ${cr.style}</div>
            </div>
            <span class="kv-sp-creator-count">${cr.count} tracks</span>
          </button>
        `
        ).join('')}
      </div>
    `;
    showcaseContainer.appendChild(hubEl);

    hubEl.querySelectorAll('[data-hub-creator]').forEach(btn => {
      btn.addEventListener('click', () => {
        const h = btn.getAttribute('data-hub-creator');
        if (h) selectCreatorChannel(h);
      });
    });
  }
}

// ============================================================================
// 9. Mount Persistent Bottom Transport Bar (#kv-music-player)
// ============================================================================
function mountBottomPlayer() {
  if (document.getElementById('kv-music-player')) return;

  const track = window.kvPlayer?.currentTrack || DEFAULT_TRACK;
  const initialDur = window.kvPlayer?.formatTime(track.metadata?.duration || 181.2) || '3:01';
  const isRightOpen = !document.getElementById('kv-sp-workspace')?.classList.contains('right-closed');

  const playerEl = document.createElement('footer');
  playerEl.id = 'kv-music-player';
  playerEl.setAttribute('aria-label', 'Audio Player Controls');
  playerEl.innerHTML = `
    <div class="kv-player-track">
      <div class="kv-player-cover-wrap">
        <img id="kv-player-cover" class="kv-player-cover" src="${track.image_url || '/favicon.png'}" alt="Cover Art" />
      </div>
      <div class="kv-player-meta">
        <div id="kv-player-title" class="kv-player-title" title="${track.title || ''}">${track.title || 'Duyên Lỡ .. Lỡ Duyên'}</div>
        <div id="kv-player-artist" class="kv-player-artist"><a href="https://suno.com/@${track.handle || 'futurescall'}" target="_blank" rel="noreferrer">@${track.handle || 'futurescall'}</a></div>
      </div>
      <button id="kv-btn-like" class="kv-player-like-btn" title="Save to Liked Songs">${SP_ICONS.heartOutline}</button>
    </div>

    <div class="kv-player-center">
      <div class="kv-player-controls">
        <button id="kv-btn-shuffle" class="kv-ctrl-btn" title="Shuffle">${SP_ICONS.shuffle}</button>
        <button id="kv-btn-prev" class="kv-ctrl-btn" title="Previous">${SP_ICONS.prev}</button>
        <button id="kv-btn-play" class="kv-play-btn" title="Play / Pause (Space)">${SP_ICONS.play}</button>
        <button id="kv-btn-next" class="kv-ctrl-btn" title="Next">${SP_ICONS.next}</button>
        <button id="kv-btn-repeat" class="kv-ctrl-btn" title="Repeat">${SP_ICONS.repeat}</button>
      </div>

      <div class="kv-player-timeline">
        <span id="kv-player-current" class="kv-time-display">0:00</span>
        <div id="kv-scrubber" class="kv-scrubber-wrap">
          <div class="kv-scrubber-track">
            <div id="kv-scrubber-fill" class="kv-scrubber-fill"></div>
            <div id="kv-scrubber-thumb" class="kv-scrubber-thumb"></div>
          </div>
        </div>
        <span id="kv-player-duration" class="kv-time-display">${initialDur}</span>
      </div>
    </div>

    <div class="kv-player-right">
      <div class="kv-row-eq kv-player-eq" title="Audio Spectrum">
        <span></span><span></span><span></span>
      </div>

      <button id="kv-btn-np-pane" class="kv-ctrl-btn ${isRightOpen ? 'active' : ''}" title="Toggle Right Track DNA Inspector">${SP_ICONS.nowPlayingPane}</button>
      <button id="kv-btn-lyrics-toggle" class="kv-ctrl-btn ${isRightOpen ? 'active' : ''}" title="Toggle Lyrics & DNA Inspector (L)">${SP_ICONS.mic}</button>
      <button id="kv-btn-prompt-send" class="kv-sp-pill-btn" style="height: 28px; padding: 0 10px; font-size: 11px;" title="Load track style into Prompt Studio">
        ${SP_ICONS.sparkles}
        <span>+ Prompt</span>
      </button>

      <div class="kv-vol-wrap">
        <button id="kv-btn-vol" class="kv-ctrl-btn" title="Mute / Unmute (M)">${SP_ICONS.volHigh}</button>
        <input id="kv-vol-slider" class="kv-vol-slider" type="range" min="0" max="100" value="85" title="Volume" />
      </div>

      <a id="kv-player-suno-link" href="https://suno.com/song/${track.id}" target="_blank" rel="noreferrer" class="kv-ctrl-btn" title="Open on Suno.com">${SP_ICONS.external}</a>
    </div>
  `;

  document.body.appendChild(playerEl);

  if (track) {
    window.kvPlayer?.updateLyricsContent(track);
  }

  document.getElementById('kv-btn-play')?.addEventListener('click', () => window.kvPlayer.togglePlay());
  document.getElementById('kv-btn-next')?.addEventListener('click', () => window.kvPlayer.next());
  document.getElementById('kv-btn-prev')?.addEventListener('click', () => window.kvPlayer.prev());
  document.getElementById('kv-btn-shuffle')?.addEventListener('click', () => window.kvPlayer.toggleShuffle());
  document.getElementById('kv-btn-repeat')?.addEventListener('click', () => window.kvPlayer.toggleRepeat());
  document.getElementById('kv-btn-like')?.addEventListener('click', () => window.kvPlayer.toggleLike());
  document.getElementById('kv-btn-lyrics-toggle')?.addEventListener('click', () => window.kvPlayer.toggleLyrics());
  document.getElementById('kv-btn-np-pane')?.addEventListener('click', () => window.kvPlayer.toggleLyrics());
  document.getElementById('kv-btn-vol')?.addEventListener('click', () => window.kvPlayer.toggleMute());

  // On mobile/tablet, tapping the mini-player cover art or title opens the Lyrics & DNA sheet
  playerEl.querySelector('.kv-player-cover-wrap')?.addEventListener('click', () => {
    if (window.innerWidth <= 1023) window.kvPlayer.toggleLyrics();
  });
  document.getElementById('kv-player-title')?.addEventListener('click', () => {
    if (window.innerWidth <= 1023) window.kvPlayer.toggleLyrics();
  });

  document.getElementById('kv-vol-slider')?.addEventListener('input', e => {
    window.kvPlayer.setVolume(parseInt(e.target.value, 10) / 100);
  });

  const scrubber = document.getElementById('kv-scrubber');
  if (scrubber) {
    let isDragging = false;
    const getClientX = e => (e.touches && e.touches[0] ? e.touches[0].clientX : e.clientX);
    const seekWithEvent = e => {
      const rect = scrubber.getBoundingClientRect();
      const clientX = getClientX(e);
      const x = Math.max(0, Math.min(clientX - rect.left, rect.width));
      const pct = x / rect.width;
      if (window.kvPlayer.audio.duration && !isNaN(window.kvPlayer.audio.duration)) {
        window.kvPlayer.seek(pct * window.kvPlayer.audio.duration);
      }
    };

    scrubber.addEventListener('mousedown', e => {
      isDragging = true;
      seekWithEvent(e);
    });
    window.addEventListener('mousemove', e => {
      if (isDragging) seekWithEvent(e);
    });
    window.addEventListener('mouseup', () => {
      isDragging = false;
    });
    scrubber.addEventListener('touchstart', e => {
      isDragging = true;
      seekWithEvent(e);
    }, { passive: true });
    window.addEventListener('touchmove', e => {
      if (isDragging) seekWithEvent(e);
    }, { passive: true });
    window.addEventListener('touchend', () => {
      isDragging = false;
    });
  }

  document.getElementById('kv-btn-prompt-send')?.addEventListener('click', () => {
    const cur = window.kvPlayer.currentTrack;
    if (cur) {
      const tagText = cur.metadata?.tags || cur.title || '';
      injectPromptIntoTextarea(tagText);
    } else {
      showToast('Select a track to extract its style prompt');
    }
  });

  window.kvPlayer.updateVolumeUI();
  window.kvPlayer.updateLikeButtonUI();
}

// ============================================================================
// 10. Prompt Studio Helpers & Live Tag Counter Sync
// ============================================================================
function getStylePromptTextarea() {
  const studioDrawer = document.querySelector('#root > div > div.fixed.bottom-0');
  if (studioDrawer) {
    const ta = studioDrawer.querySelector('textarea');
    if (ta) return ta;
  }
  const textareas = document.querySelectorAll('textarea');
  return textareas.length > 0 ? textareas[0] : null;
}

function getActiveStudioPromptValue() {
  const ta = getStylePromptTextarea();
  return (ta?.value || '').trim();
}

function syncStudioPromptCounters() {
  const raw = getActiveStudioPromptValue();
  const tags = raw
    ? raw
        .split(',')
        .map(t => t.trim())
        .filter(Boolean)
    : [];
  const count = tags.length;

  const topBadge = document.getElementById('kv-sp-studio-count');
  if (topBadge) {
    topBadge.textContent = String(count);
  }

  const mobileBadge = document.getElementById('kv-mobile-studio-badge');
  if (mobileBadge) {
    mobileBadge.textContent = String(count);
    mobileBadge.style.display = count > 0 ? 'inline-flex' : 'none';
  }

  const leftCount = document.getElementById('kv-sp-left-studio-count');
  if (leftCount) {
    leftCount.textContent = `${count} ${count === 1 ? 'tag' : 'tags'}`;
  }

  const leftPreview = document.getElementById('kv-sp-left-studio-preview');
  if (leftPreview) {
    if (raw) {
      leftPreview.textContent = raw;
      leftPreview.classList.remove('empty');
    } else {
      leftPreview.textContent = 'Click a quick tag below or track DNA to build your prompt...';
      leftPreview.classList.add('empty');
    }
  }
}

export function injectPromptIntoTextarea(text) {
  ensureStudioComposerExpanded();
  const target = getStylePromptTextarea();
  if (target) {
    const nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value')?.set;
    if (nativeSetter) {
      nativeSetter.call(target, text);
    } else {
      target.value = text;
    }
    target.dispatchEvent(new Event('input', { bubbles: true }));
    target.dispatchEvent(new Event('change', { bubbles: true }));
    syncStudioPromptCounters();
    showToast('Loaded style into Prompt Studio');
  } else {
    navigator.clipboard?.writeText(text);
  }
}

export function appendTagToPrompt(tag) {
  ensureStudioComposerExpanded();
  const target = getStylePromptTextarea();
  if (!target) {
    navigator.clipboard?.writeText(tag);
    return;
  }
  const cur = (target.value || '').trim();

  if (cur.toLowerCase().includes(tag.toLowerCase())) {
    showToast(`"${tag}" is already in your prompt`);
    return;
  }

  const updated = cur ? `${cur}, ${tag}` : tag;
  const nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value')?.set;
  if (nativeSetter) {
    nativeSetter.call(target, updated);
  } else {
    target.value = updated;
  }
  target.dispatchEvent(new Event('input', { bubbles: true }));
  target.dispatchEvent(new Event('change', { bubbles: true }));
  syncStudioPromptCounters();
  showToast(`Added +${tag}`);
}

window.kvInjectPromptIntoTextarea = injectPromptIntoTextarea;
window.kvAppendTagToPrompt = appendTagToPrompt;
window.kvShowToast = showToast;
window.kvGetFullTrackLyrics = getFullTrackLyrics;
window.kvCleanSunoLyrics = cleanSunoLyrics;
window.kvRegisterClips = registerClipsInGlobalRegistry;

// ============================================================================
// 11. Card Play FAB, Inline Lyrics Preview & Song Card Hooks
// ============================================================================
function ensureCardPlayFab(card, onPlayClick) {
  if (card.querySelector('.kv-sp-card-play-fab')) return;

  const fab = document.createElement('button');
  fab.type = 'button';
  fab.className = 'kv-sp-card-play-fab';
  fab.setAttribute('data-state', 'play');
  fab.title = 'Play Track & View Lyrics';
  fab.innerHTML = SP_ICONS.play;

  fab.addEventListener('click', e => {
    e.stopPropagation();
    onPlayClick(true);
  });

  card.appendChild(fab);
}

function injectLyricsIntoDomCards() {
  document.querySelectorAll('[data-kv-track-id]').forEach(card => {
    const id = card.getAttribute('data-kv-track-id');
    if (!id) return;
    const reg = window.kvTrackRegistry[id];
    const snippetEl = card.querySelector('.kv-card-lyrics-snippet');
    if (snippetEl && reg) {
      snippetEl.textContent = getShortLyricsPreview(reg);
    }
  });
}

function hookSongCards() {
  const cards = document.querySelectorAll('.min-h-\\[580px\\] .grid > div:not([data-kv-hooked]), .min-h-\\[720px\\] .grid > div:not([data-kv-hooked])');
  if (cards.length === 0) return;

  cards.forEach(card => {
    const titleEl = card.querySelector('h4') || card.querySelector('p.text-xs.font-semibold') || card.querySelector('p');
    const imgEl = card.querySelector('img');
    const sunoLink = card.querySelector('a[href*="/song/"]');
    if (!titleEl || !sunoLink) return;

    const href = sunoLink.getAttribute('href') || '';
    const idMatch = href.match(/\/song\/([a-zA-Z0-9_-]+)/);
    if (!idMatch) return;

    const id = idMatch[1].toLowerCase();
    card.setAttribute('data-kv-hooked', 'true');
    card.setAttribute('data-kv-track-id', id);

    const reg = window.kvTrackRegistry[id] || {};
    const track = {
      id,
      title: titleEl.textContent.trim(),
      image_url: imgEl ? imgEl.src : reg.image_url || '/favicon.png',
      handle: reg.handle || 'futurescall',
      metadata: {
        tags: reg.metadata?.tags || '',
        prompt: reg.metadata?.prompt || ''
      }
    };

    ensureCardPlayFab(card, isExplicit => {
      window.kvPlayer.playTrack(track, null, isExplicit);
    });

    card.addEventListener('click', e => {
      if (e.target.closest('a') || e.target.closest('button')) return;
      window.kvPlayer.playTrack(track);
    });
  });

  window.kvPlayer.updateActiveTrackInDOM();
}

function hookHomeShowcaseCards() {
  const showcaseContainer = document.getElementById('block-suno-api');
  if (!showcaseContainer) return;

  ensureShowcaseRichSections();

  const explorerBtn = showcaseContainer.querySelector('button.bg-\\[\\#1f6feb\\]\\/20');
  if (explorerBtn && !explorerBtn.hasAttribute('data-kv-sub-hooked')) {
    explorerBtn.setAttribute('data-kv-sub-hooked', 'true');
    explorerBtn.addEventListener('click', () => {
      currentDiscoverSub = 'creators';
      document.querySelectorAll('#kv-zen-discover-bar [data-discover-sub]').forEach(pill => {
        pill.classList.toggle('active', pill.getAttribute('data-discover-sub') === 'creators');
      });
    });
  }

  const allCards = showcaseContainer.querySelectorAll('.grid > div');
  if (allCards.length === 0) return;

  const showcaseClips = [];

  allCards.forEach(card => {
    const sunoLink = card.querySelector('a[href*="/song/"]');
    const titleEl = card.querySelector('h4');
    const imgEl = card.querySelector('img');
    const descEl = card.querySelector('.font-mono.line-clamp-2') || card.querySelector('p');

    let id = '';
    if (sunoLink) {
      const match = (sunoLink.getAttribute('href') || '').match(/\/song\/([a-zA-Z0-9_-]+)/);
      if (match) id = match[1].toLowerCase();
    }

    if (id && titleEl) {
      const reg = window.kvTrackRegistry[id] || {};
      const rawTags = reg.metadata?.tags || (descEl ? descEl.textContent.trim() : 'Vietnamese folk trap, 808 sub bass');
      const realPrompt = reg.metadata?.prompt || '';
      const primaryGenre =
        rawTags
          .split(/[,;]+/)
          .map(t => t.trim())
          .filter(Boolean)[0] || 'Suno AI Studio';

      const clip = {
        id,
        title: titleEl.textContent.trim(),
        image_url: imgEl ? imgEl.src : reg.image_url || '',
        handle: reg.handle || 'futurescall',
        display_name: reg.display_name || 'TienNg',
        metadata: {
          tags: rawTags,
          prompt: realPrompt
        }
      };
      showcaseClips.push(clip);

      card.setAttribute('data-kv-track-id', id);

      ensureCardPlayFab(card, isExplicit => {
        window.kvPlayer.playTrack(clip, showcaseClips, isExplicit);
      });

      // Inject 2-line Lyrics Snippet Box inside each showcase card
      if (!card.querySelector('.kv-card-lyrics-snippet')) {
        const lyricsBox = document.createElement('div');
        lyricsBox.className = 'kv-card-lyrics-snippet';
        lyricsBox.textContent = getShortLyricsPreview(clip);
        const topWrap = card.querySelector('.space-y-3') || card;
        topWrap.appendChild(lyricsBox);
      }

      // Inject clean Genre Pill + "🎤 Lyrics" + "+ Studio" action row if not present
      if (!card.querySelector('.kv-zen-card-pill-row')) {
        const pillRow = document.createElement('div');
        pillRow.className = 'kv-zen-card-pill-row';
        pillRow.innerHTML = `
          <span class="kv-zen-genre-pill" title="${rawTags.replace(/"/g, '&quot;')}">${primaryGenre}</span>
          <div style="display: flex; align-items: center; gap: 6px;">
            <button type="button" class="kv-zen-card-lyrics-btn" title="Play & View Full Song Lyrics">🎤 Lyrics</button>
            <button type="button" class="kv-zen-card-add-tag" title="Append genre tag to Prompt Studio">+ Studio</button>
          </div>
        `;
        pillRow.querySelector('.kv-zen-card-lyrics-btn')?.addEventListener('click', e => {
          e.stopPropagation();
          window.kvPlayer.playTrack(clip, showcaseClips);
          window.kvPlayer.toggleLyrics(true);
          document.getElementById('kv-center-lyrics-stage')?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        });
        pillRow.querySelector('.kv-zen-card-add-tag')?.addEventListener('click', e => {
          e.stopPropagation();
          appendTagToPrompt(primaryGenre);
        });
        card.appendChild(pillRow);
      }

      if (!card.hasAttribute('data-kv-home-hooked')) {
        card.setAttribute('data-kv-home-hooked', 'true');
        card.style.cursor = 'pointer';

        card.addEventListener('click', e => {
          if (e.target.closest('a') || e.target.closest('button')) return;
          e.stopPropagation();
          window.kvPlayer.playTrack(clip, showcaseClips);
        });
      }
    }
  });

  if (showcaseClips.length > 0) {
    window.kvPlayer.setQueueIfEmpty(showcaseClips);
  }
  window.kvPlayer.updateActiveTrackInDOM();
}

// ============================================================================
// 12. Bootstrap with Debounced MutationObserver
// ============================================================================
function cleanupLegacyFcLinks() {
  document.querySelectorAll('a[href*="fc-player.onrender.com"], a[href*="fc-karaoke.vercel.app"]').forEach(el => {
    const li = el.closest('li');
    if (li) li.remove();
    else el.remove();
  });
}

function runAllEnhancements() {
  try {
    cleanupLegacyFcLinks();
    mountSpotifyAppShell();
    if (!initialDiscoverActivated && currentZenMode === 'discover') {
      if (clickUnderlyingReactTab('SunoExplorer')) {
        initialDiscoverActivated = true;
      }
    }
    ensureStudioComposerExpanded();
    hookSongCards();
    hookHomeShowcaseCards();
    injectLyricsIntoDomCards();
    syncStudioPromptCounters();
  } catch (err) {
    console.warn('[kv-suno] Enhancement warning:', err);
  }
}

window.addEventListener('beforeinstallprompt', e => {
  e.preventDefault();
  window._kvDeferredPwaPrompt = e;
  const installBtn = document.getElementById('kv-sp-install-pwa');
  if (installBtn) {
    installBtn.style.display = 'flex';
  }
});

function initAll() {
  runAllEnhancements();
  mountBottomPlayer();
  window.kvPlayer?.preloadDefaultShowcase();

  // Support PWA Home Screen Shortcut deep links (?mode=songs | creators | discover | studio | wiki | liked)
  try {
    const params = new URLSearchParams(window.location.search);
    const requestedMode = params.get('mode');
    if (requestedMode && ['songs', 'creators', 'discover', 'studio', 'wiki', 'liked'].includes(requestedMode)) {
      switchZenMode(requestedMode, { _fromHistory: true });
    }
  } catch (e) {}

  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('/sw.js').catch(() => {});
  }

  document.addEventListener('input', e => {
    if (e.target && e.target.tagName === 'TEXTAREA') {
      syncStudioPromptCounters();
    }
  });
  document.addEventListener('click', () => {
    setTimeout(syncStudioPromptCounters, 30);
  });

  let debounceTimer = null;
  const observer = new MutationObserver(() => {
    if (debounceTimer) clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      runAllEnhancements();
    }, 180);
  });

  const rootEl = document.getElementById('root');
  if (rootEl) {
    observer.observe(rootEl, { childList: true, subtree: true });
  }
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initAll);
} else {
  initAll();
}

