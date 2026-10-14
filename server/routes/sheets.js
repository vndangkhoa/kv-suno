import express from 'express';
import { refreshAllRankings } from '../services/sunoApi.js';

const router = express.Router();

/**
 * Curated top-listened Vietnamese & Popular Suno Creators
 * Automatically merged with the community Google Sheet in Suno Explorer.
 */
export const DEFAULT_VN_CREATORS = [
  'futurescall',             // TienNg (Nhạc Việt, Folk Trap, Bolero, V-Pop — 56+ tracks, 567+ plays)
  'muzikvn',                 // ♡ Muzik VN ♡ (638 tracks — 149,598+ plays)
  'daoxudeo',                // PylonStudio (360 tracks — 18,103+ plays)
  'aimusiccreater',          // AI Music Creator (Vietnam Sapa Lo-Fi & Chill — 18,701+ plays)
  'sambaguitarstrings724',   // ThreetN (Nhạc Việt & V-Pop Ballad — 6,359+ plays, top track 2,032 plays)
  'phamphungoc',             // Pham Phu Ngoc (2,008 Vietnamese tracks — 2,651+ plays)
  'saigonmusic',             // Sài Gòn....( Kymngọc ) (1,731 Vietnamese tracks — 2,087+ plays)
  'hiennguyendongtac88am',   // Hiennguyendongtac88am (34 Vietnamese tracks — 1,744+ plays)
  'rawenergy068',            // PASS1LIFE (320 tracks — Vietnamese Bolero & Dân Ca Nam Bộ, 1,660+ plays)
  'photorealisticcodec1927', // VietVerse AI (95 tracks — Nhạc Việt & Nhạc Sầu, 1,363+ plays)
  'luinight',                // NLN_LuiNight (43 V-Pop Ballad & Melodic Rap tracks — 1,332+ plays)
  'neomusiccafe',            // RaonBlue / LuneBlue (284 tracks — Vietnam Pop playlist, 1,192+ plays)
  'vietmusic',               // vietmusic (25 Vietnamese tracks — 1,011+ plays, "TÁI SINH")
  'hvu2000',                 // Hung Vu (105 Vietnamese homeland & folk tracks — 864+ plays)
  'relaxedsymphony601',      // RelaxedSymphony601 (36 V-Pop tracks — 740+ plays, "CHỜ TIN NHẮN")
  'calypsocrossfader0996',   // Họa Mi Đi Nắng (301 Vietnamese tracks — 574+ plays, "50 năm về sau")
  'twinh',                   // Viet EDM (21 Vietnamese EDM & House tracks — 560+ plays)
  'nguyenthechuong774',      // nguyenthechuong774 (28 Vietnamese Ballad tracks — 286+ plays)
  'duongcam332017',          // duongcam332017 (9 V-Pop Ballad tracks — 250+ plays)
  'tuannguyen',              // Tuân Nguyễn (30 Vietnamese tracks — 230+ plays)
  'huylenguyenquang111',     // HuyBanMe (33 Vietnamese Acoustic & Ballad tracks — 217+ plays)
  'trungnghiait1808py',      // trungnghiait1808py (Vietnamese Ballad — 183+ plays)
  'vne9697',                 // Việt Music (17 V-Pop & Chill Rap tracks — 167+ plays)
  'nancynguyenst',           // nancynguyenst (Vietnamese Acoustic — 162+ plays, "Đà Lạt trong tôi")
  'nhacremix',               // ở đây có nhạc hay (Vietnamese Remix & Vinahouse — 129+ plays)
  'diemmy07012009380',       // Zine (13 V-Pop Youth Ballad tracks — 113+ plays)
  'sonnguyen',               // SonNguyen (105 Vietnamese tracks — 103+ plays)
  'loudarrangements7357',    // LoudArrangements7357 (Vietnamese Nostalgic Pop — 94+ plays)
  'tuanle',                  // tuanle (11 Vietnamese Pop tracks — 88+ plays)
  'minhnguyen754',           // Minh Nguyen (130 tracks — Vietnamese Music playlist)
  'nhacviral',               // Music Trữ Tình - Bolero (Vietnamese Bolero & Nhạc Vàng)
  'hungtran'                 // Michael (17 Vietnamese Pop Ballad tracks)
];

/**
 * POST /api/fetch-drive-csv
 * Body: { url: string }
 * Fetches Google Spreadsheet CSV and merges with curated Vietnamese Suno creators
 */
router.post('/fetch-drive-csv', async (req, res) => {
  try {
    const { url } = req.body;
    let sheetHandles = [];
    let exportUrl = url || '';

    if (url) {
      if (url.includes('docs.google.com/spreadsheets')) {
        const gidMatch = url.match(/[?&#]gid=(\d+)/);
        const gid = gidMatch ? gidMatch[1] : '0';
        const baseDocUrl = url.split('/edit')[0];
        exportUrl = `${baseDocUrl}/export?format=csv&gid=${gid}`;
      }

      try {
        const response = await fetch(exportUrl, {
          headers: {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'
          }
        });

        if (response.ok) {
          const rawCsv = await response.text();
          sheetHandles = rawCsv
            .split(/\r?\n/)
            .map(l => l.split(',')[0].replace(/^["']|["']$/g, '').trim())
            .filter(h => h && !['username', 'user', 'handle', 'suno username', 'tên người dùng'].includes(h.toLowerCase()));
        }
      } catch (sheetErr) {
        console.warn('[fetch-drive-csv] Sheet fetch fallback to curated VN list:', sheetErr.message);
      }
    }

    const merged = [...new Set([...DEFAULT_VN_CREATORS, ...sheetHandles])];
    const content = ['username', ...merged].join('\r\n');

    // Trigger crawler to verify any newly added Google Sheet handles and update live rankings
    refreshAllRankings({ force: true, extraHandles: sheetHandles }).catch(() => {});

    res.json({
      success: true,
      content,
      creators: merged,
      resolvedUrl: exportUrl
    });
  } catch (err) {
    console.error('[fetch-drive-csv] Error:', err.message);
    res.status(500).json({ error: `Không thể tải dữ liệu Google Sheet: ${err.message}` });
  }
});

export default router;
