/**
 * kv-suno — Dedicated Creators Leaderboard & Hot Songs Chart Views (zen-tabs.js)
 * Exports:
 *   - ensureZenCustomTabsMounted(mainView)
 *   - activateZenCreatorsTab(options)
 *   - activateZenSongsTab(options)
 *   - filterCreatorsByHandle(handle)
 * Global bridge:
 *   - window.kvZenTabs = { ensureZenCustomTabsMounted, activateZenCreatorsTab, activateZenSongsTab, filterCreatorsByHandle }
 */

const ZEN_ICONS = {
  play: `<svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M7 4.5v15l12-7.5z"/></svg>`,
  pause: `<svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor"><path d="M6 4h4v16H6zm8 0h4v16h-4z"/></svg>`,
  shuffle: `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="16 3 21 3 21 8"/><line x1="4" y1="20" x2="21" y2="3"/><polyline points="21 16 21 21 16 21"/><line x1="15" y1="15" x2="21" y2="21"/><line x1="4" y1="4" x2="9" y2="9"/></svg>`,
  search: `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>`,
  close: `<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>`,
  verified: `<svg width="14" height="14" viewBox="0 0 24 24" fill="#1ed760" stroke="#000" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><polyline points="16.5 9 10.5 15 7.5 12"/></svg>`,
  sync: `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.1" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>`,
  check: `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>`,
  arrowLeft: `<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg>`
};

// ============================================================================
// 1. Built-In 32 Vietnamese & Global Suno Creators Leaderboard Seed Catalog
// ============================================================================
const SEED_CREATORS_DATA = [
  {
    username: 'muzikvn',
    display_name: '♡ Muzik VN ♡',
    avatar_url: 'https://cdn2.suno.ai/a0b5b2ef-4d8f-4ee7-aca0-164205ad3261.jpeg',
    style_category: 'V-Pop',
    styles: ['V-Pop', 'K-Pop & Future Bass', 'EDM & Remix'],
    total_plays: 149598,
    tracks_count: 638,
    total_upvotes: 4120,
    hot_score: 99.4,
    verified: true,
    color: '#ec4899',
    top_songs: [
      { id: '0242a69a-69c1-4fa1-b918-102eb1044e9b', title: 'Hipnotic ᄅ - SINB [VIVIZ]', play_count: 72505, upvote_count: 1630, duration: 165, image_url: 'https://cdn2.suno.ai/c6bfd5c2-ed4a-4db5-a009-c800ac55aedc.jpeg', tags: 'K-pop, Dub-Step, Future bass, Cinematic, Bounce drop, Girl band', prompt: '[Intro]\n(Neon synth arpeggio & heavy sub drop)\n[Verse 1]\nBước vào đêm rực sáng ánh đèn thành phố,\nTừng nhịp bass cuốn lấy tâm trí chẳng thể rời.\n[Chorus]\nHypnotic — nhịp điệu bùng nổ giữa ngân hà!' },
      { id: '4d8e56a4-5aa2-4740-8d7f-464bbc59c4cb', title: '❤️‍🔥 ‘뛰어’ ❤️‍🔥 JUMP - BLACKPINK', play_count: 10481, upvote_count: 347, duration: 175, image_url: 'https://cdn2.suno.ai/image_video_upload_bf486926-9865-4e2d-9719-8ce07347d2b9_snapshot_0s.jpeg', tags: 'K-pop, Dub-Step, Future bass, Cinematic, Bounce drop, Girl group', prompt: '[Intro]\nHands up in the stadium lights!\n[Drop]\nJump! Khi nhịp trống 808 vang lên rực lửa.' },
      { id: '10cfc44e-736b-48fe-a2a7-a5b8e925f0ba', title: '🏵️ Flowers - aespa 🏵️', play_count: 6478, upvote_count: 342, duration: 213, image_url: 'https://cdn2.suno.ai/10cfc44e-736b-48fe-a2a7-a5b8e925f0ba_348c378e.jpeg', tags: 'RnB, K-pop, Dreamy Synth, Smooth Vocal Harmony, Slow paced', prompt: '[Verse 1]\nTừng cánh hoa nở dịu dàng trong sương sớm,\nHương thơm dẫn lối giấc mơ êm đềm.' }
    ]
  },
  {
    username: 'aimusiccreater',
    display_name: 'AI Music Creator (Sapa Lo-Fi)',
    avatar_url: 'https://cdn1.suno.ai/a8539943.webp',
    style_category: 'Lo-Fi & Chill',
    styles: ['Lo-Fi & Chill', 'EDM & Remix', 'Vinahouse'],
    total_plays: 108450,
    tracks_count: 251,
    total_upvotes: 1290,
    hot_score: 96.8,
    verified: true,
    color: '#38bdf8',
    top_songs: [
      { id: '5f1e3166-50b1-4b81-9def-465bf04e61e6', title: 'Game On (Sapa Chill Session)', play_count: 56410, upvote_count: 273, duration: 62, image_url: 'https://cdn2.suno.ai/image_5f1e3166-50b1-4b81-9def-465bf04e61e6.jpeg', tags: 'Lo-Fi track, medium tempo, light warm bass, chill dynamic gaming theme', prompt: '[Instrumental Intro]\n(Warm vinyl crackle, Sapa mist lo-fi electric piano & crisp boom-bap snare)' },
      { id: 'bbe0adf5-f3a3-43ba-ae75-f3587ba6328b', title: 'Please Remember', play_count: 27066, upvote_count: 168, duration: 240, image_url: 'https://cdn2.suno.ai/image_bbe0adf5-f3a3-43ba-ae75-f3587ba6328b.jpeg', tags: 'EDM theme, Touch Relaxation, Smooth male vocals, Strong bass, Dark pop', prompt: '[Verse 1]\nPlease remember every melody we shared under the rain,\nEchoing softly through the midnight air.' },
      { id: '50ff5738-5bde-4676-bf13-005baf1c9eba', title: 'Violence (Pure VinaHouse Drop)', play_count: 18701, upvote_count: 68, duration: 193, image_url: 'https://cdn2.suno.ai/image_50ff5738-5bde-4676-bf13-005baf1c9eba.jpeg', tags: 'Hard DJ instrumental, pure VinaHouse, deep distorted sub-bass, club festival lead', prompt: '[Build-up]\n(Rising club snare roll & high-energy VinaHouse synth lead)\n[Drop]\n(Bouncy off-beat bassline)' }
    ]
  },
  {
    username: 'daoxudeo',
    display_name: 'PylonStudio',
    avatar_url: 'https://cdn1.suno.ai/aa21c05f.webp',
    style_category: 'Indie & Acoustic',
    styles: ['Indie & Acoustic', 'Cinematic Pop', 'V-Pop'],
    total_plays: 18103,
    tracks_count: 360,
    total_upvotes: 485,
    hot_score: 93.2,
    verified: true,
    color: '#8b5cf6',
    top_songs: [
      { id: '9dacaa41-ad8a-44ed-9ad1-dbc9b18dc10f', title: 'Postcad: Conference Hóc Môn Persic', play_count: 3337, upvote_count: 44, duration: 268, image_url: 'https://cdn2.suno.ai/9dacaa41-ad8a-44ed-9ad1-dbc9b18dc10f_9a79d18e.jpeg', tags: 'Deep male voice, documentary storytelling tone, ambient acoustic guitar', prompt: '[Narrator]\nCâu chuyện chiều muộn bên hiên phố cũ, nơi thanh âm mộc mạc ngân dài.' },
      { id: '2fabe257-05bd-428a-b099-0593db8d0889', title: 'Skyscrapers CA', play_count: 1066, upvote_count: 29, duration: 234, image_url: 'https://cdn2.suno.ai/2fabe257-05bd-428a-b099-0593db8d0889_0e753587.jpeg', tags: 'Male and female duet, deep husky vocals, dark pop with cinematic EDM, slow tempo', prompt: '[Verse 1]\nCity lights reflecting on glass towers,\nTwo voices intertwined in the quiet night.' },
      { id: '279a07e8-75ec-4e93-8492-d34dfea3f2e8', title: 'Mùi Thương', play_count: 1055, upvote_count: 36, duration: 262, image_url: 'https://cdn2.suno.ai/279a07e8-75ec-4e93-8492-d34dfea3f2e8_62a9794b.jpeg', tags: 'Vietnamese emotional anthem, 68 BPM, grand piano, lush orchestral strings', prompt: '[Verse 1]\nMùi thương vương trên mái tóc chiều quê,\nNghe tiếng gió đưa lời hẹn ước thuở nào.' }
    ]
  },
  {
    username: 'sambaguitarstrings724',
    display_name: 'ThreetN',
    avatar_url: 'https://cdn1.suno.ai/defaultOrange.webp',
    style_category: 'Indie & Acoustic',
    styles: ['Indie & Acoustic', 'V-Pop', 'Tropical House'],
    total_plays: 6359,
    tracks_count: 24,
    total_upvotes: 215,
    hot_score: 91.5,
    verified: true,
    color: '#facc15',
    top_songs: [
      { id: '777f1675-6e62-4e2a-a118-e73fc582a3a6', title: 'Những Giấc Mơ Bay Cao', play_count: 2032, upvote_count: 81, duration: 174, image_url: 'https://cdn2.suno.ai/image_777f1675-6e62-4e2a-a118-e73fc582a3a6.jpeg', tags: 'Tropical House, V-Pop, Bright Pluck Synth, Warm Acoustic Guitar, Uplifting Vocal', prompt: '[Verse 1]\nĐưa tay chạm lấy nắng mai dịu êm,\nNhững giấc mơ bay cao qua muôn trùng sóng.' },
      { id: 'f8593e11-03b4-42d9-b829-6357d87fe42c', title: 'Giấc Mơ Dưới Ánh Sao', play_count: 770, upvote_count: 13, duration: 199, image_url: 'https://cdn2.suno.ai/image_f8593e11-03b4-42d9-b829-6357d87fe42c.jpeg', tags: 'Indie Pop, Guitar Acoustic, Dreamy Synth Pads, Gentle Vietnamese Vocal', prompt: '[Chorus]\nDưới ánh sao đêm thu hiền hòa,\nTa viết tiếp bản tình ca tuổi đôi mươi.' },
      { id: '145636b2-8596-4e86-b2ab-5d1e6ba5a496', title: 'Giấc Mơ Dưới Ánh Sao (Acoustic Ver.)', play_count: 628, upvote_count: 7, duration: 240, image_url: 'https://cdn2.suno.ai/image_145636b2-8596-4e86-b2ab-5d1e6ba5a496.jpeg', tags: 'Indie Pop, Fingerpicked Acoustic Guitar, Ambient Pad, Nostalgic V-Pop', prompt: '[Intro]\n(Tiếng guitar thùng mộc mạc vang lên giữa đêm yên tĩnh)' }
    ]
  },
  {
    username: 'futurescall',
    display_name: 'TienNg',
    avatar_url: 'https://cdn1.suno.ai/024c756a.webp',
    style_category: 'Folk Trap',
    styles: ['Folk Trap', 'V-Pop', 'Bolero & Trữ Tình'],
    total_plays: 5840,
    tracks_count: 165,
    total_upvotes: 198,
    hot_score: 90.8,
    verified: true,
    color: '#1ed760',
    top_songs: [
      { id: '44bb6ce5-4928-4d50-87bb-7d60f7017c6b', title: 'Bóng em xa dần', play_count: 567, upvote_count: 9, duration: 240, image_url: 'https://cdn2.suno.ai/image_44bb6ce5-4928-4d50-87bb-7d60f7017c6b.jpeg', tags: 'Pop, Ballad, Emotional, Cinematic, Reflective, Modern Vietnamese Pop, Piano, Acoustic Guitar', prompt: '[INTRO]\n(Ừm… đêm nay vắng lặng…)\n[VERSE 1]\nBóng em xa dần cuối con đường mưa bay,\nChỉ còn tiếng dương cầm ngân dài trong nỗi nhớ.' },
      { id: 'd0df61e1-0658-4dc1-bb43-b2d6cff03e93', title: 'Thị trường không ngừng trôi', play_count: 522, upvote_count: 12, duration: 224, image_url: 'https://cdn2.suno.ai/image_d0df61e1-0658-4dc1-bb43-b2d6cff03e93.jpeg', tags: 'Pop Ballad, Acoustic, Vietnamese POP, Upbeat Electronic Pulse', prompt: '[Verse 1]\nNhịp sóng cuốn theo bao giấc mơ lớn,\nGiữa dòng đời hối hả ta vẫn vững niềm tin.' },
      { id: 'a15a3e62-b002-4e5b-a571-0d86adc503f2', title: 'Duyên Lỡ .. Lỡ Duyên', play_count: 489, upvote_count: 19, duration: 181, image_url: 'https://cdn2.suno.ai/746f5806-0e35-4ee5-94fd-e64d2f9a333b.jpeg', tags: 'Vietnamese folk trap, fast driving tempo, aggressive male rap, hard 808 sub bass, bamboo flute, dan tranh', prompt: '[INTRO]\n(Nhịp trống Trap dồn dập, tiếng đàn tranh rải mộc mạc)\n[CHORUS]\nDuyên âm không thành, đừng thành âm duyên.' }
    ]
  },
  {
    username: 'calypsocrossfader0996',
    display_name: 'Họa Mi Đi Nắng',
    avatar_url: 'https://cdn1.suno.ai/3bbb229c.webp',
    style_category: 'V-Pop',
    styles: ['V-Pop', 'Lo-Fi & Chill', 'Cinematic Ballad'],
    total_plays: 4765,
    tracks_count: 301,
    total_upvotes: 185,
    hot_score: 88.9,
    verified: true,
    color: '#f43f5e',
    top_songs: [
      { id: '7ac74e33-604b-40d3-95b2-cf9e6e3e07ab', title: '50 năm về sau', play_count: 574, upvote_count: 10, duration: 310, image_url: 'https://cdn2.suno.ai/image_7ac74e33-604b-40d3-95b2-cf9e6e3e07ab.jpeg', tags: 'Romantic chillout ballad, soulful emotional male vocals, warm intimate Vietnamese V-Pop', prompt: '[Verse 1]\nNăm mươi năm về sau khi mái đầu điểm sương,\nTa vẫn giữ nguyên vẹn lời thương buổi ban đầu.' },
      { id: '1ce6d7c4-c858-45c5-9f5a-2e5c24f842a2', title: 'River Flows in You (Midnight Chill)', play_count: 506, upvote_count: 17, duration: 245, image_url: 'https://cdn2.suno.ai/image_1ce6d7c4-c858-45c5-9f5a-2e5c24f842a2.jpeg', tags: 'Deep chillout, midnight lounge, slow groovy bass, lush ambient pads, soft filtered synths', prompt: '[Instrumental]\n(Grand piano melody floating over deep midnight lounge sub-bass)' },
      { id: 'a1afd97e-48cc-4811-bf14-326b2feed704', title: 'Họa Mi Hót Trong Mưa', play_count: 264, upvote_count: 48, duration: 307, image_url: 'https://cdn2.suno.ai/image_a1afd97e-48cc-4811-bf14-326b2feed704.jpeg', tags: 'Cinematic orchestral ballad, soaring violin lead melody, deeply melancholic acoustic', prompt: '[Chorus]\nTiếng chim họa mi ngân vang giữa màn mưa trắng xóa.' }
    ]
  },
  {
    username: 'phamphungoc',
    display_name: 'Pham Phu Ngoc',
    avatar_url: 'https://cdn1.suno.ai/a39833aa.webp',
    style_category: 'Bolero & Trữ Tình',
    styles: ['Bolero & Trữ Tình', 'Nhạc Quê Hương', 'Cinematic'],
    total_plays: 4651,
    tracks_count: 2008,
    total_upvotes: 172,
    hot_score: 88.2,
    verified: true,
    color: '#10b981',
    top_songs: [
      { id: 'cf0b4df3-4298-4504-b5ca-32a7c98da253', title: 'Trần Quý Cáp • 35 Năm Hội Khóa', play_count: 296, upvote_count: 17, duration: 400, image_url: 'https://cdn2.suno.ai/cf0b4df3-4298-4504-b5ca-32a7c98da253_878fbd32.jpeg', tags: 'Emotional Vietnamese reunion anthem, nostalgic cinematic pop ballad, warm strings', prompt: '[Verse 1]\nBa mươi lăm năm ngày trở lại mái trường xưa,\nBao kỷ niệm tuổi học trò ùa về trong nắng hạ.' },
      { id: '0153e403-fd15-4754-b62f-e1133d38a87b', title: 'Spanish Paso Doble 🇪🇸🎸', play_count: 257, upvote_count: 22, duration: 136, image_url: 'https://cdn2.suno.ai/image_0153e403-fd15-4754-b62f-e1133d38a87b.jpeg', tags: 'Instrumental Paso Doble, Spanish brass band, snare drums, flamenco guitar', prompt: '[Instrumental]\n(Energetic Spanish flamenco guitar strumming & triumphant brass fanfare)' },
      { id: 'e4c6c6ed-154d-45f6-8083-b08fc70c7f34', title: 'Mẹ Của Tôi', play_count: 197, upvote_count: 14, duration: 429, image_url: 'https://cdn2.suno.ai/image_e4c6c6ed-154d-45f6-8083-b08fc70c7f34.jpeg', tags: 'Grand Piano, Solo Violin, Muted Trumpet, Accordion, Vietnamese Homeland Ballad', prompt: '[Chorus]\nDáng mẹ hao gầy qua bao mùa mưa nắng vì con.' }
    ]
  },
  {
    username: 'saigonmusic',
    display_name: 'Sài Gòn Bolero (Kymngọc)',
    avatar_url: 'https://cdn1.suno.ai/ed9ff5c3.webp',
    style_category: 'Bolero & Trữ Tình',
    styles: ['Bolero & Trữ Tình', 'Sacred Choir', 'Cinematic Ballad'],
    total_plays: 3890,
    tracks_count: 1731,
    total_upvotes: 145,
    hot_score: 86.7,
    verified: true,
    color: '#f59e0b',
    top_songs: [
      { id: '18a26210-8d81-4160-a884-aa3edfcf8ea6', title: 'TC - Niềm Tin Vào Chúa (Kymngọc)', play_count: 256, upvote_count: 14, duration: 391, image_url: 'https://cdn2.suno.ai/18a26210-8d81-4160-a884-aa3edfcf8ea6_5a4e87e8.jpeg', tags: 'Sacred choir, cinematic ballad, church organ intro, SATB choir, emotional Vietnamese vocal', prompt: '[Intro]\n(Tiếng đàn organ nhà thờ ngân vang trang nghiêm)\n[Verse 1]\nLời cầu nguyện dâng lên giữa đêm thanh tĩnh.' },
      { id: '4566a2fe-2b17-459a-b55b-8cc81e8b1215', title: 'TC - Lời Ru Của Mẹ (Kymngọc)', play_count: 238, upvote_count: 16, duration: 298, image_url: 'https://cdn2.suno.ai/4566a2fe-2b17-459a-b55b-8cc81e8b1215_b063da25.jpeg', tags: 'Cinematic Vietnamese ballad, warm strings, gentle acoustic piano, emotional soprano', prompt: '[Chorus]\nLời ru của mẹ êm đềm như dòng sông quê hương.' },
      { id: '3aee09b4-ecd4-49d2-a0a3-b0aa50869cd9', title: 'TC - Chuỗi Mân Côi Mẹ Dạy', play_count: 176, upvote_count: 11, duration: 352, image_url: 'https://cdn2.suno.ai/3aee09b4-ecd4-49d2-a0a3-b0aa50869cd9_fee8f58c.jpeg', tags: 'Sacred choir, cinematic ballad, pipe organ, soaring strings', prompt: '[Verse 1]\nTừng lời kinh mẹ dạy khắc ghi suốt cuộc đời.' }
    ]
  },
  {
    username: 'rawenergy068',
    display_name: 'PASS1LIFE',
    avatar_url: 'https://cdn1.suno.ai/75729a3b.webp',
    style_category: 'Folk Trap',
    styles: ['Folk Trap', 'Dân Ca & Rock', 'EDM & Remix'],
    total_plays: 3120,
    tracks_count: 320,
    total_upvotes: 134,
    hot_score: 85.4,
    verified: true,
    color: '#fb923c',
    top_songs: [
      { id: '3f9f82a0-c1c0-4394-87b0-0ab3ef031769', title: 'Sommar Vals', play_count: 1660, upvote_count: 46, duration: 158, image_url: 'https://cdn2.suno.ai/3f9f82a0-c1c0-4394-87b0-0ab3ef031769_1965be6b.jpeg', tags: 'Folk Waltz, Violin solo, Accordion, Nyckelharpa, Acoustic Folk', prompt: '[Instrumental]\n(Lively traditional violin and accordion folk waltz)' },
      { id: 'b64a0e44-452b-4e2d-afbf-b2f74531e10b', title: 'Midnight Pulse', play_count: 349, upvote_count: 9, duration: 108, image_url: 'https://cdn2.suno.ai/image_b64a0e44-452b-4e2d-afbf-b2f74531e10b.jpeg', tags: 'Driving Electronic, Heavy Bass, Energetic Rhythm', prompt: '[Drop]\n(High-voltage bassline & punchy drums)' },
      { id: '65308740-5dd5-40d5-9a43-fed077b922d4', title: 'Acid Rules Earth', play_count: 268, upvote_count: 33, duration: 122, image_url: 'https://cdn2.suno.ai/65308740-5dd5-40d5-9a43-fed077b922d4_3f68c60b.jpeg', tags: 'Acid Techno, TB-303 Squelch, Rave Synthesizer, 135 BPM', prompt: '[Synth Solo]\n(Resonant 303 acid filter sweep over four-on-the-floor kick)' }
    ]
  },
  {
    username: 'neomusiccafe',
    display_name: 'RaonBlue / LuneBlue',
    avatar_url: 'https://cdn1.suno.ai/7414f1d2.webp',
    style_category: 'V-Pop',
    styles: ['V-Pop', 'Asian Pop Ballad', 'Indie & Acoustic'],
    total_plays: 2680,
    tracks_count: 284,
    total_upvotes: 112,
    hot_score: 84.1,
    verified: true,
    color: '#60a5fa',
    top_songs: [
      { id: '198d285f-ace8-4b8d-9c17-376a798e9df3', title: '서툰 우리 (Memory Folk Pop)', play_count: 1192, upvote_count: 42, duration: 234, image_url: 'https://cdn2.suno.ai/video_upload_889e5d0f-45f4-4e01-abc2-f6e0c774e848_video_upload_889e5d0f-45f4-4e01-abc2-f6e0c774e848_cover_snapshot_0s_1780153431_image.jpeg', tags: 'Memory Folk Pop, Warm acoustic guitar, Sweet vocal duet, Romantic ballad', prompt: '[Verse 1]\nWarm acoustic strumming as two shy hearts slowly fall in love.' },
      { id: 'f6f95fa0-8ffd-4284-aece-cac0eeb28a26', title: '消えてゆく記憶 (Fading Memories)', play_count: 676, upvote_count: 14, duration: 255, image_url: 'https://cdn2.suno.ai/image_video_upload_1a606c13-a81a-4663-a0c3-0e74b9b06ca8_cover_snapshot_0s_1759736662.jpeg', tags: 'Emotional Asian Ballad, slow tempo 70 BPM, grand piano, strings', prompt: '[Chorus]\nMelancholic piano and strings echoing through fading autumn memories.' },
      { id: 'c0c72318-085e-4c5a-87c7-e3156f0654b6', title: '빈 가지 끝에서 (At the End of Bare Branches)', play_count: 343, upvote_count: 18, duration: 255, image_url: 'https://cdn2.suno.ai/c0c72318-085e-4c5a-87c7-e3156f0654b6_a6163e66.jpeg', tags: 'Indie folk alternative ballad, delicate fingerpicked acoustic guitar', prompt: '[Intro]\n(Delicate fingerpicked acoustic guitar in a quiet winter room)' }
    ]
  },
  {
    username: 'hiennguyendongtac88am',
    display_name: 'Hiennguyendongtac88am',
    avatar_url: 'https://cdn2.suno.ai/ca9fae33-60c9-462e-ab0c-d53f9d7868eb.jpeg',
    style_category: 'Folk Trap',
    styles: ['Folk Trap', 'Xẩm & Dân Ca Bắc Bộ', 'Cinematic'],
    total_plays: 1744,
    tracks_count: 34,
    total_upvotes: 48,
    hot_score: 82.0,
    verified: true,
    color: '#eab308',
    top_songs: [
      { id: 'ada3b524-963e-403d-9b25-b36d5919faae', title: 'Kính Gửi Thầy Học Tri Huyện Tiên Minh', play_count: 313, upvote_count: 4, duration: 317, image_url: 'https://cdn2.suno.ai/image_ada3b524-963e-403d-9b25-b36d5919faae.jpeg', tags: 'Vietnamese Contemporary Xẩm, Northern Vietnamese Folk, Cinematic Spiritual Folk, Đàn Nhị', prompt: '[Intro]\n(Tiếng đàn nhị và phách gỗ Xẩm cổ truyền ngân vang)\n[Verse 1]\nNgàn năm văn hiến khắc ghi công đức tiền nhân.' },
      { id: '29751c35-4e3c-4328-9ec4-a8794d0d8ea8', title: 'Hạng A Dình Người Mông Lai Châu', play_count: 229, upvote_count: 3, duration: 373, image_url: 'https://cdn2.suno.ai/image_29751c35-4e3c-4328-9ec4-a8794d0d8ea8.jpeg', tags: 'Northern Vietnamese Folk, H’Mông Flute, Cinematic Highland Anthem', prompt: '[Verse 1]\nTiếng khèn Mông gọi nắng trên đỉnh đèo Lai Châu hùng vĩ.' },
      { id: 'a307dc41-dce9-47c9-8e9d-7fca8ceba7b7', title: 'Đông Tác – Trung Tự – Thăng Long Ngàn Năm', play_count: 172, upvote_count: 5, duration: 454, image_url: 'https://cdn2.suno.ai/image_a307dc41-dce9-47c9-8e9d-7fca8ceba7b7.jpeg', tags: 'Vietnamese contemporary folk cinematic ballad, Northern Vietnam cultural heritage', prompt: '[Chorus]\nThăng Long ngàn năm hồn thiêng sông núi hội tụ.' }
    ]
  },
  {
    username: 'photorealisticcodec1927',
    display_name: 'VietVerse AI',
    avatar_url: 'https://cdn1.suno.ai/sAura20.jpg',
    style_category: 'Bolero & Trữ Tình',
    styles: ['Bolero & Trữ Tình', 'V-Pop', 'Nhạc Thiền'],
    total_plays: 1590,
    tracks_count: 95,
    total_upvotes: 42,
    hot_score: 80.5,
    verified: true,
    color: '#14b8a6',
    top_songs: [
      { id: 'bfef8627-cb82-4bb7-a4a0-a940ca4e1a74', title: 'Mẹ Về Cõi Tịnh (Lời: Phúc Hậu)', play_count: 123, upvote_count: 4, duration: 195, image_url: 'https://cdn2.suno.ai/image_bfef8627-cb82-4bb7-a4a0-a940ca4e1a74.jpeg', tags: 'Vietnamese spiritual ballad, slow tempo, soft piano, đàn tranh, temple bell', prompt: '[Intro]\n(Tiếng chuông chùa ngân nhẹ cùng tiếng đàn tranh thanh tịnh)' },
      { id: 'f1fbb0c9-5e2e-443c-b0c6-42847dbf5954', title: 'Tôi Yêu Tiếng Việt Miền Nam', play_count: 121, upvote_count: 3, duration: 240, image_url: 'https://cdn2.suno.ai/image_f1fbb0c9-5e2e-443c-b0c6-42847dbf5954.jpeg', tags: 'Vietnamese Pop Ballad, Contemplative, Soulful Acoustic', prompt: '[Verse 1]\nThương giọng nói miền Nam ngọt ngào như phù sa Cửu Long.' },
      { id: 'e326d3d1-3da3-4c47-b81e-a6cb5ccf40b3', title: 'Tình Đời (Lời: Phúc Hậu)', play_count: 114, upvote_count: 5, duration: 188, image_url: 'https://cdn2.suno.ai/image_e326d3d1-3da3-4c47-b81e-a6cb5ccf40b3.jpeg', tags: 'Contemporary V-Pop, Melancholic Synth, Emotional Male Vocal', prompt: '[Chorus]\nTình đời hợp tan như mây trắng bay ngang trời.' }
    ]
  },
  {
    username: 'minhnguyen754',
    display_name: 'Minh Nguyen',
    avatar_url: 'https://cdn1.suno.ai/1ec2f281.webp',
    style_category: 'Bolero & Trữ Tình',
    styles: ['Bolero & Trữ Tình', 'Soft Rock Ballad', 'V-Pop'],
    total_plays: 1452,
    tracks_count: 130,
    total_upvotes: 38,
    hot_score: 79.4,
    verified: true,
    color: '#06b6d4',
    top_songs: [
      { id: 'e0b95683-e129-4cea-a030-3eec46b8f5ac', title: 'Love In First Sight', play_count: 450, upvote_count: 5, duration: 241, image_url: 'https://cdn2.suno.ai/image_e0b95683-e129-4cea-a030-3eec46b8f5ac.jpeg', tags: 'Soft Rock, Mid-Tempo Ballad, G Major, Warm Electric Guitar Solo', prompt: '[Verse 1]\nFrom the very first glance, the melody began to play.' },
      { id: 'd19bd2a2-1903-4d66-bec8-5d340931add1', title: 'Phận Nghèo (Bolero Sầu)', play_count: 169, upvote_count: 4, duration: 215, image_url: 'https://cdn2.suno.ai/image_d19bd2a2-1903-4d66-bec8-5d340931add1.jpeg', tags: 'Vietnamese Bolero, Slow Deep Guitar Intro, Emotional Female Vocal', prompt: '[Verse 1]\nĐêm mưa ngoại ô nghe tiếng đàn guitar nức nở phận nghèo.' },
      { id: '5ae0bfa7-3381-4803-8fee-dce4c764715c', title: 'Tình Yêu Mùa Đông', play_count: 101, upvote_count: 2, duration: 90, image_url: 'https://cdn2.suno.ai/image_5ae0bfa7-3381-4803-8fee-dce4c764715c.jpeg', tags: 'Vietnamese Poetic Ballad, Slow Piano & Violin', prompt: '[Intro]\nCơn gió mùa đông khẽ chạm vào ký ức xưa.' }
    ]
  },
  {
    username: 'luinight',
    display_name: 'NLN_LuiNight',
    avatar_url: 'https://cdn2.suno.ai/a37b4e1a-3104-4a22-b531-1cb2dc4f1e14.jpeg',
    style_category: 'V-Pop',
    styles: ['V-Pop', 'Melodic Rap', 'Ballad'],
    total_plays: 1332,
    tracks_count: 43,
    total_upvotes: 35,
    hot_score: 78.6,
    verified: true,
    color: '#a78bfa',
    top_songs: [
      { id: '1f13316b-deb6-414e-82b3-776376b85ef9', title: 'Thương Lắm Ai Ơi', play_count: 145, upvote_count: 6, duration: 194, image_url: 'https://cdn2.suno.ai/image_1f13316b-deb6-414e-82b3-776376b85ef9.jpeg', tags: 'Vietnamese emotional pop ballad, male vocal, soft piano and acoustic guitar', prompt: '[Chorus]\nThương lắm ai ơi bên kia dòng sông nắng chiều buông xuống.' },
      { id: 'ff9b7057-befd-4590-a117-ca39c5a8c019', title: 'Hoa Nở Dưới Biển Sâu', play_count: 111, upvote_count: 4, duration: 212, image_url: 'https://cdn2.suno.ai/image_ff9b7057-befd-4590-a117-ca39c5a8c019.jpeg', tags: 'Vietnamese modern rap, emotional chill love rap, melodic hook, smooth male vocal', prompt: '[Rap Verse]\nNhư đóa hoa nở dưới biển sâu lặng lẽ đợi ánh mặt trời.' },
      { id: '255b5eec-a813-458b-b5bb-c8d560823e2e', title: 'Nguyện Hái Sao Trên Trời', play_count: 101, upvote_count: 3, duration: 154, image_url: 'https://cdn2.suno.ai/image_255b5eec-a813-458b-b5bb-c8d560823e2e.jpeg', tags: 'Vietnamese pop rap, cinematic, emotional male vocal, modern trap beat', prompt: '[Chorus]\nNguyện hái sao trên trời cao thắp sáng đêm dài cho em.' }
    ]
  },
  {
    username: 'vietmusic',
    display_name: 'vietmusic',
    avatar_url: 'https://cdn1.suno.ai/9e0e6cd6.webp',
    style_category: 'V-Pop',
    styles: ['V-Pop', 'Tango & Melodic Rap', 'Dance Rock'],
    total_plays: 1011,
    tracks_count: 25,
    total_upvotes: 31,
    hot_score: 77.2,
    verified: true,
    color: '#ec4899',
    top_songs: [
      { id: 'e63cef9f-243a-4658-b764-d8ee66a15282', title: 'TÁI SINH', play_count: 175, upvote_count: 7, duration: 206, image_url: 'https://cdn2.suno.ai/e63cef9f-243a-4658-b764-d8ee66a15282_c05574c1.jpeg', tags: 'Vietnamese Tango, Melodic Rap, Dramatic Strings, Driving Beat', prompt: '[Chorus]\nTái sinh từ tro tàn, rực rỡ giữa màn đêm.' },
      { id: '425b96f6-b3fc-43f1-beb1-ff9ea9140648', title: 'THƯƠNG BIỆT LY', play_count: 135, upvote_count: 8, duration: 203, image_url: 'https://cdn2.suno.ai/425b96f6-b3fc-43f1-beb1-ff9ea9140648_33b61c4e.jpeg', tags: 'Rock Dance, V-Pop Fusion, Electric Guitar & Synth', prompt: '[Verse 1]\nChén rượu tiễn đưa người đi xa mãi phương trời.' },
      { id: '20a7a95d-4b2c-440f-a3ae-c15b9fba67d8', title: 'THƯƠNG BIỆT LY (Remix)', play_count: 109, upvote_count: 6, duration: 221, image_url: 'https://cdn2.suno.ai/20a7a95d-4b2c-440f-a3ae-c15b9fba67d8_c974ae89.jpeg', tags: 'Dance Rock, Upbeat Club Remix, Vietnamese Vocal', prompt: '[Drop]\nNhịp trống dồn dập cuốn trôi nỗi sầu biệt ly.' }
    ]
  },
  {
    username: 'hvu2000',
    display_name: 'Hung Vu',
    avatar_url: 'https://cdn1.suno.ai/9b344e17.webp',
    style_category: 'Bolero & Trữ Tình',
    styles: ['Bolero & Trữ Tình', 'Nhạc Huế', 'Acoustic Duet'],
    total_plays: 864,
    tracks_count: 105,
    total_upvotes: 28,
    hot_score: 75.8,
    verified: true,
    color: '#f59e0b',
    top_songs: [
      { id: 'ad941339-9d54-4c8d-9673-8b6a77fced14', title: 'Huế Chiều Xưa', play_count: 70, upvote_count: 3, duration: 298, image_url: 'https://cdn2.suno.ai/ad941339-9d54-4c8d-9673-8b6a77fced14_79450242.jpeg', tags: 'Bolero Rhumba, Vietnamese duet, gentle Huế accent, nostalgic guitar', prompt: '[Verse 1]\nChiều trên sông Hương tím biếc tà áo dài xứ Huế.' },
      { id: '83537866-0243-422f-afb3-d299a1646a03', title: 'BÓNG ĐỔ CHIỀU THU', play_count: 56, upvote_count: 2, duration: 268, image_url: 'https://cdn2.suno.ai/83537866-0243-422f-afb3-d299a1646a03_c10df982.jpeg', tags: 'Bolero duet, Female & Male vocals, slow nostalgic guitar & accordion', prompt: '[Chorus]\nBóng đổ chiều thu bên thềm lá vàng rơi nhẹ.' },
      { id: 'f4054b7c-de46-4191-81ac-74d00173d19c', title: 'Nó Là Tôi', play_count: 50, upvote_count: 2, duration: 205, image_url: 'https://cdn2.suno.ai/f4054b7c-de46-4191-81ac-74d00173d19c_4a165527.jpeg', tags: 'Vietnamese Bolero, Warm Male Baritone, Acoustic Guitar', prompt: '[Verse 1]\nTự sự đời trai qua bao thăng trầm năm tháng.' }
    ]
  },
  {
    username: 'relaxedsymphony601',
    display_name: 'RelaxedSymphony601',
    avatar_url: 'https://cdn1.suno.ai/defaultOrange.webp',
    style_category: 'V-Pop',
    styles: ['V-Pop', 'Ballad', 'Pop Beat'],
    total_plays: 740,
    tracks_count: 36,
    total_upvotes: 22,
    hot_score: 74.3,
    verified: false,
    color: '#34d399',
    top_songs: [
      { id: 'db38a23d-7134-4599-b855-0340eeff0567', title: 'CHỜ TIN NHẮN', play_count: 93, upvote_count: 4, duration: 315, image_url: 'https://cdn2.suno.ai/image_db38a23d-7134-4599-b855-0340eeff0567.jpeg', tags: 'V-Pop Ballad, Tempo 70 BPM, Key Fm, Emotional Piano', prompt: '[Verse 1]\nĐêm khuya nhìn màn hình sáng rồi lại tắt, chờ một dòng tin nhắn từ em.' },
      { id: 'dca5dc8d-a203-4c76-a9cb-5fa859681d3d', title: 'Sóng Tóc (Thơ: Thanh Trúc)', play_count: 56, upvote_count: 2, duration: 132, image_url: 'https://cdn2.suno.ai/image_dca5dc8d-a203-4c76-a9cb-5fa859681d3d.jpeg', tags: 'Pop Beat, Tempo 125 BPM, Key C Major, Bright Synth', prompt: '[Chorus]\nSóng tóc bềnh bồng trong gió biển chiều xanh.' },
      { id: '69f307cf-17b2-4724-bc73-99e22b58e693', title: 'Bão Cát Sa Mạc', play_count: 50, upvote_count: 2, duration: 311, image_url: 'https://cdn2.suno.ai/image_69f307cf-17b2-4724-bc73-99e22b58e693.jpeg', tags: 'Cinematic Rock, Middle Eastern Scale, Heavy Drums', prompt: '[Intro]\n(Dramatic percussion and desert strings)' }
    ]
  },
  {
    username: 'twinh',
    display_name: 'Viet EDM',
    avatar_url: 'https://cdn1.suno.ai/5c2a2671.webp',
    style_category: 'EDM & Remix',
    styles: ['EDM & Remix', 'Melodic House', 'Vinahouse'],
    total_plays: 560,
    tracks_count: 21,
    total_upvotes: 19,
    hot_score: 73.1,
    verified: true,
    color: '#2dd4bf',
    top_songs: [
      { id: 'aa97f0ed-c6fe-40d6-ba18-a6dd6f6d1420', title: 'Tiếng Yêu Từ Đâu - Where Is Love', play_count: 50, upvote_count: 3, duration: 196, image_url: 'https://cdn2.suno.ai/image_aa97f0ed-c6fe-40d6-ba18-a6dd6f6d1420.jpeg', tags: 'Powerful modern ballad, male and female duet vocals, progressive EDM drop', prompt: '[Chorus]\nTiếng yêu từ đâu ngân vang giữa bầu trời đêm.' },
      { id: 'dfcb8a3f-1f75-4a98-9750-745f7f22d349', title: 'Nếu Em Còn Ở Đây / If You’re Still Here', play_count: 45, upvote_count: 2, duration: 310, image_url: 'https://cdn2.suno.ai/image_dfcb8a3f-1f75-4a98-9750-745f7f22d349.jpeg', tags: 'Vietnamese house fusion, call-response duet vocals, rolling groovy bassline', prompt: '[Drop]\n(Rolling house bassline with emotional Vietnamese vocal chops)' },
      { id: '37997b1e-3db2-4832-aa84-ca343e1ea52a', title: 'Across the Sky (128 BPM)', play_count: 45, upvote_count: 4, duration: 277, image_url: 'https://cdn2.suno.ai/image_37997b1e-3db2-4832-aa84-ca343e1ea52a.jpeg', tags: 'Melodic House, Hopeful Romantic, 128 BPM, Synth Leads & Pads', prompt: '[Chorus]\nFlying across the sky on waves of melodic synth.' }
    ]
  },
  {
    username: 'nguyenthechuong774',
    display_name: 'nguyenthechuong774',
    avatar_url: 'https://cdn1.suno.ai/f37ffa47.webp',
    style_category: 'Bolero & Trữ Tình',
    styles: ['Bolero & Trữ Tình', 'Pop Ballad', 'Hard Rock'],
    total_plays: 286,
    tracks_count: 28,
    total_upvotes: 14,
    hot_score: 71.0,
    verified: false,
    color: '#fb7185',
    top_songs: [
      { id: '1285467d-ca36-4b92-88f6-bde588937a95', title: 'Tiếng Thở Dài', play_count: 45, upvote_count: 2, duration: 196, image_url: 'https://cdn2.suno.ai/image_1285467d-ca36-4b92-88f6-bde588937a95.jpeg', tags: 'Vietnamese Bolero, Classic Guitar, Nostalgic Male Vocal', prompt: '[Verse 1]\nTiếng thở dài rơi nhẹ giữa đêm khuya thanh vắng.' },
      { id: '3ac7e423-269b-41cb-b429-173916aa4739', title: 'Chỉ Có Tôi', play_count: 40, upvote_count: 2, duration: 223, image_url: 'https://cdn2.suno.ai/image_3ac7e423-269b-41cb-b429-173916aa4739.jpeg', tags: 'V-Pop Ballad, Emotional Piano, Solo Vocal', prompt: '[Chorus]\nChỉ có tôi đứng lại nơi góc phố ngày xưa.' },
      { id: '2ce92368-2ea7-47dc-a98f-ee138bc12a5d', title: 'Lòng Đất Mẹ', play_count: 26, upvote_count: 1, duration: 274, image_url: 'https://cdn2.suno.ai/image_2ce92368-2ea7-47dc-a98f-ee138bc12a5d.jpeg', tags: 'Electric Guitar, Hard Rock Anthem, Vietnamese Vocal', prompt: '[Guitar Solo]\n(Soaring overdriven electric guitar tribute to homeland)' }
    ]
  },
  {
    username: 'duongcam332017',
    display_name: 'Dương Cầm Studio',
    avatar_url: 'https://cdn1.suno.ai/sAura21.jpg',
    style_category: 'V-Pop',
    styles: ['V-Pop', 'Melodic Electronic', 'Female Vocal'],
    total_plays: 250,
    tracks_count: 9,
    total_upvotes: 11,
    hot_score: 69.8,
    verified: false,
    color: '#c084fc',
    top_songs: [
      { id: 'd9fdc4e8-1561-488c-ae2b-779cf302adc1', title: 'Đừng Để Em Mất Anh (Nữ)', play_count: 62, upvote_count: 3, duration: 296, image_url: 'https://cdn2.suno.ai/fa5cb01c-4889-4e5e-bdb4-bc964fbc6145.jpeg', tags: 'V-Pop Ballad, Clear Female Vocal, Emotional Piano & Strings', prompt: '[Chorus]\nXin đừng để em mất anh giữa dòng đời ngược xuôi.' },
      { id: '145a9cf6-9c14-4254-9386-4ba86b3b8429', title: 'Bình Minh – Hoàng Hôn (ST: Nguyễn Văn Hải)', play_count: 40, upvote_count: 2, duration: 431, image_url: 'https://cdn2.suno.ai/ca8a031f-4607-4fc8-abae-5a58667baf74.jpeg', tags: 'IDM, Melodic Electronic, Emotional Female Vocal, Ambient Pads', prompt: '[Verse 1]\nTừ bình minh rực nắng đến hoàng hôn tím biếc chân mây.' },
      { id: '72bc2fb3-0750-4670-9ed1-432155b62b57', title: 'Nắng Gọi Bình Minh', play_count: 40, upvote_count: 2, duration: 343, image_url: 'https://cdn2.suno.ai/20eda669-e75d-4a96-b695-a996836b3e66.jpeg', tags: 'Melodic Electronic, Uplifting V-Pop, Crisp Synth Arpeggio', prompt: '[Chorus]\nNắng gọi bình minh thức giấc trên từng nhành lá.' }
    ]
  },
  {
    username: 'tuannguyen',
    display_name: 'Tuân Nguyễn',
    avatar_url: 'https://cdn1.suno.ai/defaultPink.webp',
    style_category: 'Indie & Acoustic',
    styles: ['Indie & Acoustic', 'Delta Blues', 'Rock'],
    total_plays: 230,
    tracks_count: 30,
    total_upvotes: 10,
    hot_score: 68.5,
    verified: false,
    color: '#f472b6',
    top_songs: [
      { id: '9a326284-942d-4feb-b62f-c6668b6ae06c', title: 'Khúc Ngẫu Hứng Đêm', play_count: 26, upvote_count: 1, duration: 147, image_url: 'https://cdn2.suno.ai/image_9a326284-942d-4feb-b62f-c6668b6ae06c.jpeg', tags: 'Indie Rock, Raw Guitar Riff, Studio Jam', prompt: '[Instrumental]\n(Raw indie rock guitar riff and tight snare)' },
      { id: 'dd1a7932-ccae-4293-b739-afc6edc98ccc', title: 'Cá Sấu Đầm Sâu - Thích Tự Thi', play_count: 20, upvote_count: 1, duration: 120, image_url: 'https://cdn2.suno.ai/image_dd1a7932-ccae-4293-b739-afc6edc98ccc.jpeg', tags: 'Experimental Folk, Acoustic Storytelling', prompt: '[Verse 1]\nCâu chuyện ngụ ngôn bên đầm nước sâu tĩnh lặng.' },
      { id: 'f8b99604-61f2-4567-85f1-598b01a8cddd', title: 'Thức Đêm - Thích Tự Thi', play_count: 18, upvote_count: 2, duration: 153, image_url: 'https://cdn2.suno.ai/image_f8b99604-61f2-4567-85f1-598b01a8cddd.jpeg', tags: 'Emotional Delta Blues, Slide Guitar, Late Night Vibe', prompt: '[Blues Solo]\n(Smoky late-night delta blues slide guitar)' }
    ]
  },
  {
    username: 'huylenguyenquang111',
    display_name: 'HuyBanMe',
    avatar_url: 'https://cdn1.suno.ai/sAura9.jpg',
    style_category: 'Indie & Acoustic',
    styles: ['Indie & Acoustic', 'V-Pop Ballad', 'Acoustic Piano'],
    total_plays: 217,
    tracks_count: 33,
    total_upvotes: 12,
    hot_score: 67.9,
    verified: false,
    color: '#38bdf8',
    top_songs: [
      { id: 'a994829b-e85f-4824-beb8-cec60b8d577d', title: 'HƯƠNG SƯƠNG', play_count: 36, upvote_count: 2, duration: 270, image_url: 'https://cdn2.suno.ai/image_a994829b-e85f-4824-beb8-cec60b8d577d.jpeg', tags: 'Gentle Vietnamese Ballad, Deep Emotion, Slow Tempo, Acoustic Guitar', prompt: '[Verse 1]\nHương sương sớm Ban Mê vương trên nhành hoa cà phê trắng.' },
      { id: '92d2447f-8b7d-4b56-9c11-9549e7b4dca1', title: 'ĐOÁ HOA TỰ TẠI', play_count: 32, upvote_count: 2, duration: 215, image_url: 'https://cdn2.suno.ai/image_92d2447f-8b7d-4b56-9c11-9549e7b4dca1.jpeg', tags: 'Modern Vietnamese Pop Ballad, Soulful Warm Male Vocals, Acoustic Piano', prompt: '[Chorus]\nNở giữa đất trời một đóa hoa tự tại bình yên.' },
      { id: '5f350c88-f113-4b0e-b47f-da92e85eb439', title: 'CÔNG CHÚA TUỔI 13', play_count: 14, upvote_count: 1, duration: 278, image_url: 'https://cdn2.suno.ai/image_5f350c88-f113-4b0e-b47f-da92e85eb439.jpeg', tags: 'Emotional Vietnamese Ballad, 68 BPM, Warm Piano Lead, Gentle Strings', prompt: '[Verse 1]\nNụ cười trong veo tuổi mười ba ngập tràn nắng ấm.' }
    ]
  },
  {
    username: 'trungnghiait1808py',
    display_name: 'Trung Nghĩa Ballad',
    avatar_url: 'https://cdn1.suno.ai/sAura6.jpg',
    style_category: 'Bolero & Trữ Tình',
    styles: ['Bolero & Trữ Tình', 'V-Pop Ballad', 'Slow Groove'],
    total_plays: 183,
    tracks_count: 15,
    total_upvotes: 9,
    hot_score: 66.8,
    verified: false,
    color: '#fbbf24',
    top_songs: [
      { id: 'f504cb48-f4a2-4b75-a0fa-43d712f7ac9d', title: 'MƯỢN ÁNH TRĂNG NÓI THAY LỜI', play_count: 45, upvote_count: 2, duration: 204, image_url: 'https://cdn2.suno.ai/image_f504cb48-f4a2-4b75-a0fa-43d712f7ac9d.jpeg', tags: 'Ballad, Soft emotional male voice, Tempo 75 BPM, Acoustic Guitar', prompt: '[Chorus]\nMượn ánh trăng đêm rằm nói thay lời thương gửi đến em.' },
      { id: '0db92287-2cd2-4716-a220-0fbdbd67702f', title: 'Đôi Tình Nhân Trước Cổng Nhà Thờ', play_count: 40, upvote_count: 2, duration: 204, image_url: 'https://cdn2.suno.ai/image_0db92287-2cd2-4716-a220-0fbdbd67702f.jpeg', tags: 'Bolero Slow Ballad, Deep Warm Male Voice, Nostalgic Strings', prompt: '[Verse 1]\nChiều chủ nhật nghe tiếng chuông ngân trước cổng nhà thờ.' },
      { id: '0e582a55-0f6a-4435-a0b9-1d34574005cb', title: 'CHIA TAY LÀ ĐÚNG PHẢI KHÔNG EM', play_count: 40, upvote_count: 1, duration: 175, image_url: 'https://cdn2.suno.ai/image_0e582a55-0f6a-4435-a0b9-1d34574005cb.jpeg', tags: 'V-Pop Melancholic Ballad, Electric Piano, Smooth Vocal', prompt: '[Chorus]\nChia tay là đúng phải không em, khi hai lối rẽ chẳng còn chung đường.' }
    ]
  },
  {
    username: 'vne9697',
    display_name: 'Việt Music (VNE)',
    avatar_url: 'https://cdn1.suno.ai/sAura16.jpg',
    style_category: 'V-Pop',
    styles: ['V-Pop', 'Cinematic Pop', 'Chill Rap'],
    total_plays: 167,
    tracks_count: 17,
    total_upvotes: 8,
    hot_score: 65.9,
    verified: false,
    color: '#818cf8',
    top_songs: [
      { id: '2c10ea90-c1e2-4ae9-9828-0e7bb7544f1b', title: 'Đứt Duyên', play_count: 37, upvote_count: 2, duration: 297, image_url: 'https://cdn2.suno.ai/image_2c10ea90-c1e2-4ae9-9828-0e7bb7544f1b.jpeg', tags: 'Modern Pop Ballad, Slow Tempo, Soft Breathy Male Vocal, Legato Strings', prompt: '[Verse 1]\nTơ hồng đứt đoạn giữa chiều mưa, duyên xưa khép lại.' },
      { id: '7c1f6b43-72d4-4814-8fac-7ec8daa5effa', title: 'Bá Khí Vùng Trời', play_count: 18, upvote_count: 1, duration: 298, image_url: 'https://cdn2.suno.ai/image_7c1f6b43-72d4-4814-8fac-7ec8daa5effa.jpeg', tags: 'Cinematic Pop, Driving Pulse 100 BPM, Epic Orchestral Brass', prompt: '[Chorus]\nDang rộng đôi cánh tung hoành giữa vùng trời bao la.' },
      { id: 'd6af7d22-8191-43c8-a3eb-4ef3e5e574f1', title: 'Cạn Duyên', play_count: 16, upvote_count: 1, duration: 234, image_url: 'https://cdn2.suno.ai/image_d6af7d22-8191-43c8-a3eb-4ef3e5e574f1.jpeg', tags: 'Intimate Pop Ballad, Breathy Male Vocal, Clean Acoustic Guitar', prompt: '[Verse 1]\nChén trà đã nguội, duyên mình cũng cạn từ đây.' }
    ]
  },
  {
    username: 'nancynguyenst',
    display_name: 'Nancy Nguyễn Acoustic',
    avatar_url: 'https://cdn1.suno.ai/sAura4.jpg',
    style_category: 'Indie & Acoustic',
    styles: ['Indie & Acoustic', 'Pop Rock', 'Country Acoustic'],
    total_plays: 162,
    tracks_count: 12,
    total_upvotes: 9,
    hot_score: 65.2,
    verified: false,
    color: '#4ade80',
    top_songs: [
      { id: 'a6b7d4ff-2562-4566-819c-009de0f9a83b', title: 'Đà Lạt Trong Tôi', play_count: 70, upvote_count: 3, duration: 288, image_url: 'https://cdn2.suno.ai/a6b7d4ff-2562-4566-819c-009de0f9a83b_0e439cc9.jpeg', tags: 'Vietnamese Pop Rock, Country Rock, Raw Acoustic Guitar Intro', prompt: '[Verse 1]\nĐà Lạt sương mờ ôm lấy đồi thông xanh ngát trong tôi.' },
      { id: 'a79eca4d-cc28-4d16-936a-5e3663e751dc', title: '25 Năm Xa Quê', play_count: 47, upvote_count: 2, duration: 263, image_url: 'https://cdn2.suno.ai/video_upload_da911498-76d4-4b78-a0ab-24af3d6ed923_video_upload_da911498-76d4-4b78-a0ab-24af3d6ed923_cover_snapshot_0s_1767398534_image.jpeg', tags: 'Deep Pop Rock, Emotional Female Voice, Nostalgic Homeland', prompt: '[Chorus]\nHai mươi lăm năm xa quê, lòng vẫn hướng về mái nhà xưa.' },
      { id: '19141fde-3a45-40fc-9fda-84465b807f86', title: 'Kín Hay Hở', play_count: 17, upvote_count: 1, duration: 267, image_url: 'https://cdn2.suno.ai/19141fde-3a45-40fc-9fda-84465b807f86_7dce1de5.jpeg', tags: 'Vietnamese Female Vocal, Warm Husky Voice, Acoustic Lounge', prompt: '[Verse 1]\nNét duyên thầm kín đáo mà cuốn hút bao ánh nhìn.' }
    ]
  },
  {
    username: 'nhacremix',
    display_name: 'Ở Đây Có Nhạc Hay',
    avatar_url: 'https://cdn1.suno.ai/sAura6.jpg',
    style_category: 'EDM & Remix',
    styles: ['EDM & Remix', 'Vinahouse', 'V-Pop Dance'],
    total_plays: 129,
    tracks_count: 18,
    total_upvotes: 7,
    hot_score: 64.4,
    verified: false,
    color: '#22d3ee',
    top_songs: [
      { id: '6be78207-38b2-44b4-8464-79a1cf4fbb60', title: 'Lưu Niên (V-Pop Remix)', play_count: 26, upvote_count: 2, duration: 206, image_url: 'https://cdn2.suno.ai/image_6be78207-38b2-44b4-8464-79a1cf4fbb60.jpeg', tags: 'V-Pop Remix, Syncopated Synth Bass, Bright Staccato Lead, Lush Pads', prompt: '[Drop]\n(Upbeat V-Pop synth lead with punchy festival kick)' },
      { id: 'd667928f-d4a0-4830-a629-ce4cf68e7517', title: 'Anh Đã Lừa Dối Em Rồi (Remix)', play_count: 23, upvote_count: 1, duration: 162, image_url: 'https://cdn2.suno.ai/image_d667928f-d4a0-4830-a629-ce4cf68e7517.jpeg', tags: 'Electronic Club Remix, Sidechain Synth Pulse, Male Vocal', prompt: '[Chorus]\nNhịp bass dồn dập xóa tan những lời dối gian.' },
      { id: 'a5ce11d7-7608-4648-886b-a587d2f0fec0', title: 'Cắt Đôi Nỗi Sầu (128 BPM Club)', play_count: 23, upvote_count: 2, duration: 191, image_url: 'https://cdn2.suno.ai/image_a5ce11d7-7608-4648-886b-a587d2f0fec0.jpeg', tags: 'Vietnamese Dance Pop, Driving Beat 128 BPM, Minor Key Synth', prompt: '[Chorus]\nCắt đôi nỗi sầu, thả trôi theo điệu nhạc đêm nay!' }
    ]
  },
  {
    username: 'diemmy07012009380',
    display_name: 'Zine (Youth Ballad)',
    avatar_url: 'https://cdn1.suno.ai/sAura15.jpg',
    style_category: 'V-Pop',
    styles: ['V-Pop', 'Youth Ballad', 'Indie & Acoustic'],
    total_plays: 113,
    tracks_count: 13,
    total_upvotes: 6,
    hot_score: 63.5,
    verified: false,
    color: '#f472b6',
    top_songs: [
      { id: 'fea5e7c3-9adf-4f15-9574-e4583e7d559e', title: 'Thanh Xuân Bỏ Lỡ', play_count: 26, upvote_count: 2, duration: 200, image_url: 'https://cdn2.suno.ai/image_fea5e7c3-9adf-4f15-9574-e4583e7d559e.jpeg', tags: 'Dreamy V-Pop Ballad, Nostalgic Youth, Soft Piano', prompt: '[Chorus]\nThanh xuân bỏ lỡ một lời yêu chưa kịp nói.' },
      { id: '3b79814c-8778-46b5-9916-590add7d5f64', title: 'Tình Yêu Một Chiều', play_count: 16, upvote_count: 1, duration: 226, image_url: 'https://cdn2.suno.ai/image_3b79814c-8778-46b5-9916-590add7d5f64.jpeg', tags: 'Acoustic Ballad, Intimate Vocal, Gentle Guitar', prompt: '[Verse 1]\nLặng thầm bước phía sau người qua bao mùa nắng mưa.' },
      { id: 'd20b793f-270f-404d-a691-c201532ce69c', title: 'Thanh Xuân Bỏ Lỡ (Acoustic)', play_count: 12, upvote_count: 1, duration: 168, image_url: 'https://cdn2.suno.ai/image_d20b793f-270f-404d-a691-c201532ce69c.jpeg', tags: 'Acoustic V-Pop, Dreamy Guitar, Soft Vocal', prompt: '[Outro]\nKý ức tuổi trẻ mãi xanh trong tim.' }
    ]
  },
  {
    username: 'sonnguyen',
    display_name: 'SonNguyen',
    avatar_url: 'https://cdn1.suno.ai/defaultOrange.webp',
    style_category: 'Lo-Fi & Chill',
    styles: ['Lo-Fi & Chill', 'Orchestral', 'Soundtrack'],
    total_plays: 103,
    tracks_count: 105,
    total_upvotes: 6,
    hot_score: 62.8,
    verified: false,
    color: '#60a5fa',
    top_songs: [
      { id: '072cf8ee-3d8e-4ae6-801d-6a07205b5eb7', title: 'Orchestral Dawn Suite', play_count: 16, upvote_count: 2, duration: 120, image_url: 'https://cdn2.suno.ai/image_072cf8ee-3d8e-4ae6-801d-6a07205b5eb7.jpeg', tags: 'Cinematic Orchestral, Sweeping Strings, French Horn', prompt: '[Instrumental]\n(Majestic orchestral strings opening at sunrise)' },
      { id: 'e251b5b1-aca0-4c50-b16f-8b28e8894412', title: 'Mariachi Snooze (Studio Cover)', play_count: 9, upvote_count: 1, duration: 136, image_url: 'https://cdn2.suno.ai/image_e251b5b1-aca0-4c50-b16f-8b28e8894412.jpeg', tags: 'Acoustic Brass, Relaxed Lounge, Ambient', prompt: '[Instrumental]\n(Lighthearted acoustic guitar and warm brass)' },
      { id: 'bbaed120-a752-49c5-ae2b-066fe3e84616', title: 'Loping Sting (Ambient Mix)', play_count: 7, upvote_count: 1, duration: 145, image_url: 'https://cdn2.suno.ai/image_bbaed120-a752-49c5-ae2b-066fe3e84616.jpeg', tags: 'Chill Ambient, Game Soundtrack, Soft Plucks', prompt: '[Instrumental]\n(Calm ambient soundtrack loop)' }
    ]
  },
  {
    username: 'loudarrangements7357',
    display_name: 'LoudArrangements7357',
    avatar_url: 'https://cdn1.suno.ai/defaultBlue.webp',
    style_category: 'Indie & Acoustic',
    styles: ['Indie & Acoustic', 'V-Pop', 'Electronic Pop'],
    total_plays: 94,
    tracks_count: 14,
    total_upvotes: 5,
    hot_score: 61.9,
    verified: false,
    color: '#a3e635',
    top_songs: [
      { id: 'd9484e6e-e122-4b5e-9a06-db4761d4d217', title: 'Kỷ Niệm Lung Linh', play_count: 26, upvote_count: 1, duration: 210, image_url: 'https://cdn2.suno.ai/image_d9484e6e-e122-4b5e-9a06-db4761d4d217.jpeg', tags: 'Indie Chill, Romantic V-Pop, Warm Acoustic Guitar', prompt: '[Verse 1]\nLung linh kỷ niệm dưới hiên nhà đầy nắng.' },
      { id: '799515ce-d13e-4c5e-92c8-a41642534b24', title: 'Tình Bạn Chân Thật', play_count: 16, upvote_count: 1, duration: 169, image_url: 'https://cdn2.suno.ai/image_799515ce-d13e-4c5e-92c8-a41642534b24.jpeg', tags: 'Upbeat V-Pop, Cheerful Rhythm, Bright Guitar', prompt: '[Chorus]\nCảm ơn những năm tháng có bạn đồng hành bên ta.' },
      { id: 'aed49869-09dc-46b1-861c-18372d2fbdbc', title: 'Ngọc Trai Tỏa Sáng', play_count: 16, upvote_count: 1, duration: 151, image_url: 'https://cdn2.suno.ai/image_aed49869-09dc-46b1-861c-18372d2fbdbc.jpeg', tags: 'Electronic Pop, Shimmering Synth, Modern V-Pop', prompt: '[Chorus]\nTỏa sáng giữa đại dương xanh thẫm.' }
    ]
  },
  {
    username: 'tuanle',
    display_name: 'Tuấn Lê Official',
    avatar_url: 'https://cdn1.suno.ai/defaultOrange.webp',
    style_category: 'V-Pop',
    styles: ['V-Pop', 'Pop-Rock Anthem', 'Electronic Pop'],
    total_plays: 88,
    tracks_count: 11,
    total_upvotes: 5,
    hot_score: 61.2,
    verified: false,
    color: '#fb923c',
    top_songs: [
      { id: '95c96d39-b72a-4bd5-b92b-1ed27c866d61', title: 'Kết Nối Thành Công', play_count: 26, upvote_count: 2, duration: 179, image_url: 'https://cdn2.suno.ai/image_95c96d39-b72a-4bd5-b92b-1ed27c866d61.jpeg', tags: 'Uplifting Vietnamese Pop Anthem, Polished Modern Production', prompt: '[Chorus]\nChung tay kết nối vươn tới những đỉnh cao thành công!' },
      { id: 'aa1133ef-1ba7-4c87-81cb-26e26612501f', title: 'Kết Nối Thành Công (Remastered)', play_count: 12, upvote_count: 1, duration: 145, image_url: 'https://cdn2.suno.ai/image_aa1133ef-1ba7-4c87-81cb-26e26612501f.jpeg', tags: 'Vietnamese Pop-Rock Anthem, Male & Female Duet, 112 BPM', prompt: '[Verse 1]\nNhịp bước vững vàng tiến về phía mặt trời.' },
      { id: 'e147f307-9f8e-4ceb-bb84-6212f3d8e999', title: 'Kết Nối Thành Công (Electronic Mix)', play_count: 12, upvote_count: 1, duration: 178, image_url: 'https://cdn2.suno.ai/image_e147f307-9f8e-4ceb-bb84-6212f3d8e999.jpeg', tags: 'Electronic Pop, Inspiring Synth Leads, V-Pop', prompt: '[Outro]\nVững bước tương lai rạng ngời.' }
    ]
  },
  {
    username: 'nhacviral',
    display_name: 'Music Trữ Tình - Bolero',
    avatar_url: 'https://cdn1.suno.ai/sAura6.jpg',
    style_category: 'Bolero & Trữ Tình',
    styles: ['Bolero & Trữ Tình', 'Nhạc Vàng', 'Đàn Bầu & Đàn Tranh'],
    total_plays: 72,
    tracks_count: 16,
    total_upvotes: 6,
    hot_score: 60.5,
    verified: false,
    color: '#eab308',
    top_songs: [
      { id: '089f63e2-dffa-4094-8b57-d92cdf6a87f7', title: 'Hồn Cây, Lửa Tâm Can', play_count: 32, upvote_count: 2, duration: 298, image_url: 'https://cdn2.suno.ai/image_089f63e2-dffa-4094-8b57-d92cdf6a87f7.jpeg', tags: 'Rock Ballad, Blues Rock, Acoustic Guitar, Emotional Male Vocals', prompt: '[Verse 1]\nNgọn lửa tâm can cháy mãi giữa rừng chiều.' },
      { id: 'fe043d65-aacd-4ea4-8dbc-084850422be4', title: 'Hương Tình Dĩ Vãng', play_count: 18, upvote_count: 1, duration: 245, image_url: 'https://cdn2.suno.ai/image_fe043d65-aacd-4ea4-8dbc-084850422be4.jpeg', tags: 'Vietnamese Bolero, Subtle Percussion, Warm Acoustic Guitar', prompt: '[Chorus]\nHương tình dĩ vãng thoảng qua trong cơn mưa chiều.' },
      { id: '2c5a7dbd-c2ec-4937-9bea-02977bc3ef65', title: 'Điệu Xưa Khẽ Hát', play_count: 12, upvote_count: 1, duration: 245, image_url: 'https://cdn2.suno.ai/image_2c5a7dbd-c2ec-4937-9bea-02977bc3ef65.jpeg', tags: 'Bolero Ballad, Slow Swaying Groove, Gentle Đàn Bầu & Đàn Tranh', prompt: '[Intro]\n(Tiếng đàn bầu ngân nga hòa cùng nhịp trống Bolero chậm rãi)' }
    ]
  },
  {
    username: 'hungtran',
    display_name: 'Michael (Hùng Trần)',
    avatar_url: 'https://cdn1.suno.ai/defaultBlue.webp',
    style_category: 'V-Pop',
    styles: ['V-Pop', 'Pop Ballad', 'Tenor Vocal'],
    total_plays: 65,
    tracks_count: 17,
    total_upvotes: 5,
    hot_score: 59.8,
    verified: false,
    color: '#38bdf8',
    top_songs: [
      { id: 'bd4bf1de-22e2-470d-8439-bc1a09ffc48d', title: 'Dù Cho Tận Thế', play_count: 18, upvote_count: 2, duration: 225, image_url: 'https://cdn2.suno.ai/image_bd4bf1de-22e2-470d-8439-bc1a09ffc48d.jpeg', tags: 'Vietnamese Pop Ballad, Emotional Strings, Soaring Tenor Vocal', prompt: '[Chorus]\nDù cho tận thế vẫn nắm chặt bàn tay em không rời.' },
      { id: '386c2679-d8ee-4ae8-9b03-a41d30383605', title: 'Dù Cho Tận Thế Vẫn Yêu Anh', play_count: 14, upvote_count: 1, duration: 233, image_url: 'https://cdn2.suno.ai/image_386c2679-d8ee-4ae8-9b03-a41d30383605.jpeg', tags: 'Cinematic V-Pop Ballad, Grand Piano, Orchestral Climax', prompt: '[Verse 1]\nQua bao giông bão, lời hẹn ước vẫn vẹn nguyên.' },
      { id: '1c4e7ff1-0975-4e00-a7b4-8943249c51dc', title: 'Tình Đã Rời', play_count: 12, upvote_count: 1, duration: 225, image_url: 'https://cdn2.suno.ai/image_1c4e7ff1-0975-4e00-a7b4-8943249c51dc.jpeg', tags: 'Gentle Ballad, Tenor Voice, Acoustic Guitar & Cello', prompt: '[Outro]\nTình đã rời xa như chiếc lá cuối thu.' }
    ]
  }
];

const EXTRA_CHART_SONGS = [
  { id: 'ce8ee57a-d5b4-401e-8cd9-be904ddcc168', title: 'Niềm tin, đến bên tôi 2', handle: 'futurescall', display_name: 'TienNg', play_count: 338, upvote_count: 11, duration: 235, image_url: 'https://cdn2.suno.ai/image_ce8ee57a-d5b4-401e-8cd9-be904ddcc168.jpeg', tags: 'Synthwave, Emotional Vibrato Vocals, Pop Ballad, Vietnamese', prompt: '[Chorus]\nNiềm tin đến bên tôi giữa màn đêm rực ánh đèn synthwave.' },
  { id: '0da7ac67-94eb-43c5-9e42-3177b99c8664', title: 'Đêm Trắng Sàn Giao Dịch', handle: 'futurescall', display_name: 'TienNg', play_count: 312, upvote_count: 9, duration: 198, image_url: 'https://cdn2.suno.ai/image_0da7ac67-94eb-43c5-9e42-3177b99c8664.jpeg', tags: 'Vietnamese Trap Ballad, 808 Bass, Melancholic Piano, Male Rap', prompt: '[Verse 1]\nNhìn những ngọn nến xanh đỏ nhảy múa suốt đêm dài.' },
  { id: '190f8d25-5d96-4268-bc9e-d8d7148afc3c', title: 'Khúc Nhạc Chiều Mưa', handle: 'futurescall', display_name: 'TienNg', play_count: 284, upvote_count: 8, duration: 210, image_url: 'https://cdn2.suno.ai/image_190f8d25-5d96-4268-bc9e-d8d7148afc3c.jpeg', tags: 'V-Pop Acoustic Ballad, Warm Guitar, Lo-Fi Rain Ambience', prompt: '[Verse 1]\nTiếng mưa rơi bên hiên gợi nhớ bóng hình xưa.' },
  { id: '35ecc80a-bd70-447e-83f8-329d0c308547', title: 'Sóng Gió Không Ngừng', handle: 'futurescall', display_name: 'TienNg', play_count: 265, upvote_count: 7, duration: 192, image_url: 'https://cdn2.suno.ai/image_35ecc80a-bd70-447e-83f8-329d0c308547.jpeg', tags: 'Folk Trap, Đàn Tranh, Hard 808 Sub Bass, Cinematic Rap', prompt: '[Chorus]\nVượt qua muôn trùng sóng gió, giữ vững chí trai.' },
  { id: '4180d477-9a9e-43a3-b8ae-ffe9628cfdd4', title: 'Lời Hứa Dưới Ánh Trăng', handle: 'futurescall', display_name: 'TienNg', play_count: 240, upvote_count: 8, duration: 215, image_url: 'https://cdn2.suno.ai/image_4180d477-9a9e-43a3-b8ae-ffe9628cfdd4.jpeg', tags: 'V-Pop Ballad, Emotional Female Vocal, Strings & Piano', prompt: '[Chorus]\nLời hứa dưới ánh trăng năm ấy vẫn vẹn nguyên trong tim.' },
  { id: '4bb7bed2-c03f-41eb-9eab-50e322319204', title: 'Hành Trình Tự Do', handle: 'futurescall', display_name: 'TienNg', play_count: 218, upvote_count: 6, duration: 204, image_url: 'https://cdn2.suno.ai/image_4bb7bed2-c03f-41eb-9eab-50e322319204.jpeg', tags: 'Indie Pop Rock, Acoustic & Electric Guitar, Uplifting V-Pop', prompt: '[Verse 1]\nBước chân tự do đi khắp những cung đường nắng mới.' }
];

// ============================================================================
// 2. Module State & Direct DOM References
// ============================================================================
const state = {
  creators: [],
  creatorSort: 'hot',       // 'hot' | 'plays' | 'tracks'
  creatorStyle: 'all',      // 'all' | 'V-Pop' | 'Bolero & Trữ Tình' | 'Folk Trap' | 'Lo-Fi & Chill' | 'EDM & Remix' | 'Indie & Acoustic'
  creatorSearch: '',
  loadingHandles: new Set(),

  songs: [],
  songSort: 'hot',          // 'hot' | 'plays' | 'upvotes'
  songCreator: 'all',       // 'all' or handle
  songGenre: 'all',         // 'all' | 'V-Pop' | 'Ballad' | 'Folk Trap' | 'Lo-Fi' | 'Remix / EDM' | 'Cinematic'
  songSearch: '',
  songViewMode: 'table',    // 'table' | 'grid'

  creatorsViewEl: null,
  songsViewEl: null,
  mounted: false,
  audioHooked: false
};

// ============================================================================
// 3. Complementary Scoped Styles (Prepended to <head> so enhancements.css wins)
// ============================================================================
function ensureScopedZenTabsStyles() {
  if (document.getElementById('kv-zen-tabs-inline-style')) return;
  const styleEl = document.createElement('style');
  styleEl.id = 'kv-zen-tabs-inline-style';
  styleEl.textContent = `
    .kv-podium-medal {
      font-size: 10px;
      font-weight: 800;
      padding: 2px 8px;
      border-radius: 9999px;
      letter-spacing: 0.05em;
      text-transform: uppercase;
      align-self: flex-start;
    }
    .kv-podium-card.podium-1 .kv-podium-medal { background: rgba(245, 158, 11, 0.22); color: #fbbf24; }
    .kv-podium-card.podium-2 .kv-podium-medal { background: rgba(203, 213, 225, 0.18); color: #e2e8f0; }
    .kv-podium-card.podium-3 .kv-podium-medal { background: rgba(217, 119, 6, 0.22); color: #fb923c; }
    .kv-podium-top-row {
      display: flex;
      align-items: center;
      gap: 10px;
      min-width: 0;
    }
    .kv-podium-avatar {
      width: 44px;
      height: 44px;
      border-radius: 50%;
      overflow: hidden;
      background: #242424;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 800;
      font-size: 16px;
      color: #fff;
      flex-shrink: 0;
    }
    .kv-podium-avatar img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transform: scale(1.08);
    }
    .kv-podium-play-btn {
      margin-top: auto;
      width: 100%;
      height: 28px;
      border-radius: 9999px;
      border: none;
      background: rgba(30, 215, 96, 0.16);
      color: #1ed760;
      font-size: 11px;
      font-weight: 700;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 5px;
      transition: background-color 0.15s ease, color 0.15s ease;
    }
    .kv-podium-play-btn:hover {
      background: #1ed760;
      color: #000000;
    }
    .kv-creator-rank-card.highlight-pulse {
      box-shadow: 0 0 0 2px #1ed760;
    }
    .kv-mini-chart-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 10px;
      font-weight: 800;
      text-transform: uppercase;
      letter-spacing: 0.07em;
      color: #8e8e8e;
      padding: 0 2px 4px;
    }
    .kv-mini-load-trigger {
      color: #1ed760;
      font-size: 10.5px;
      font-weight: 700;
      cursor: pointer;
    }
    .kv-mini-load-trigger:hover {
      text-decoration: underline;
    }
    .kv-creator-mini-row.kv-sp-active-card {
      background-color: rgba(30, 215, 96, 0.14);
    }
    .kv-creator-mini-row.kv-sp-active-card .kv-mini-song-title {
      color: #1ed760 !important;
    }
    .kv-mini-song-play {
      width: 26px;
      height: 26px;
      border-radius: 50%;
      border: none;
      background: rgba(30, 215, 96, 0.16);
      color: #1ed760;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      flex-shrink: 0;
      transition: background-color 0.15s ease, color 0.15s ease;
    }
    .kv-creator-mini-row:hover .kv-mini-song-play,
    .kv-creator-mini-row.kv-sp-active-card .kv-mini-song-play {
      background: #1ed760;
      color: #000000;
    }
    .kv-zen-select {
      height: 32px;
      padding: 0 12px;
      border-radius: 9999px;
      background: #1f1f1f;
      color: #ffffff;
      border: none;
      font-size: 12px;
      font-weight: 700;
      cursor: pointer;
      outline: none;
    }
    .kv-songs-cover-grid {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 16px;
    }
    @media (max-width: 1280px) {
      .kv-songs-cover-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
    }
    @media (max-width: 900px) {
      .kv-songs-cover-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    }
    .kv-song-grid-card {
      background: #181818;
      border-radius: 10px;
      padding: 12px;
      display: flex;
      flex-direction: column;
      gap: 8px;
      cursor: pointer;
      transition: background-color 0.18s ease, transform 0.18s ease;
    }
    .kv-song-grid-card:hover {
      background: #242424;
      transform: translateY(-2px);
    }
    .kv-song-grid-art {
      position: relative;
      width: 100%;
      aspect-ratio: 1 / 1;
      border-radius: 8px;
      overflow: hidden;
      background: #121212;
    }
    .kv-song-grid-art img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      transform: scale(1.11);
      transition: transform 0.3s ease;
    }
    .kv-song-grid-card:hover .kv-song-grid-art img {
      transform: scale(1.16);
    }
    .kv-grid-rank-badge {
      position: absolute;
      top: 8px;
      left: 8px;
      padding: 3px 9px;
      border-radius: 9999px;
      font-size: 11px;
      font-weight: 800;
      background: rgba(0, 0, 0, 0.78);
      color: #ffffff;
      font-variant-numeric: tabular-nums;
      z-index: 2;
    }
    .kv-grid-rank-badge.rank-1 { background: #f59e0b; color: #000; }
    .kv-grid-rank-badge.rank-2 { background: #cbd5e1; color: #000; }
    .kv-grid-rank-badge.rank-3 { background: #d97706; color: #000; }
    .kv-grid-dur-badge {
      position: absolute;
      bottom: 8px;
      left: 8px;
      padding: 2px 7px;
      border-radius: 4px;
      font-size: 10.5px;
      font-weight: 700;
      background: rgba(0, 0, 0, 0.75);
      color: #e5e5e5;
      font-variant-numeric: tabular-nums;
      z-index: 2;
    }
  `;
  if (document.head.firstChild) {
    document.head.insertBefore(styleEl, document.head.firstChild);
  } else {
    document.head.appendChild(styleEl);
  }
}

// ============================================================================
// 4. Utility Helpers (Formatting, Lyrics, Prompt Studio Bridge, Toast)
// ============================================================================
function escapeHtml(str = '') {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function formatCompactCount(num = 0) {
  const n = Number(num) || 0;
  if (n >= 1000000) return `${(n / 1000000).toFixed(1)}M`;
  if (n >= 10000) return `${(n / 1000).toFixed(1)}K`;
  return n.toLocaleString('en-US');
}

function formatDuration(secs = 180) {
  const s = Math.max(1, Math.round(Number(secs) || 180));
  const mins = Math.floor(s / 60);
  const rem = s % 60;
  return `${mins}:${rem < 10 ? '0' : ''}${rem}`;
}

function showZenToast(message) {
  let container = document.getElementById('studio-toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'studio-toast-container';
    document.body.appendChild(container);
  }
  const toast = document.createElement('div');
  toast.className = 'studio-toast';
  toast.innerHTML = `${ZEN_ICONS.check}<span>${escapeHtml(message)}</span>`;
  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(8px)';
    toast.style.transition = 'opacity 0.2s ease, transform 0.2s ease';
    setTimeout(() => toast.remove(), 200);
  }, 2200);
}

function appendTagsToPromptStudio(tagString) {
  const cleanTag = String(tagString || '').trim();
  if (!cleanTag) return;

  try {
    localStorage.setItem('suno-studio-hidden', 'false');
  } catch (e) {}

  const studioDrawer = document.querySelector('#root > div > div.fixed.bottom-0');
  const ta = studioDrawer?.querySelector('textarea') || document.querySelector('textarea');
  if (ta) {
    const currentVal = (ta.value || '').trim();
    if (currentVal.toLowerCase().includes(cleanTag.toLowerCase())) {
      showZenToast(`"${cleanTag}" is already in Prompt Studio`);
      return;
    }
    const nextVal = currentVal ? `${currentVal}, ${cleanTag}` : cleanTag;
    const nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value')?.set;
    if (nativeSetter) {
      nativeSetter.call(ta, nextVal);
    } else {
      ta.value = nextVal;
    }
    ta.dispatchEvent(new Event('input', { bubbles: true }));
    ta.dispatchEvent(new Event('change', { bubbles: true }));
    showZenToast(`Added +${cleanTag} to Prompt Studio`);
  } else {
    navigator.clipboard?.writeText(cleanTag);
    showZenToast(`Copied "${cleanTag}" to clipboard`);
  }
}

function registerTrackInGlobalRegistry(song) {
  if (!song || !song.id) return;
  window.kvTrackRegistry = window.kvTrackRegistry || {};
  const id = String(song.id).toLowerCase();
  const prev = window.kvTrackRegistry[id] || {};
  const tags = song.metadata?.tags || song.tags || prev.metadata?.tags || 'Vietnamese V-Pop, Studio Master';
  const prompt = song.metadata?.prompt || song.prompt || prev.metadata?.prompt || '';
  const duration = song.metadata?.duration || song.duration || prev.metadata?.duration || 185;

  window.kvTrackRegistry[id] = {
    ...prev,
    ...song,
    id,
    title: (song.title || prev.title || 'Untitled Track').trim(),
    handle: (song.handle || prev.handle || 'futurescall').replace(/^@/, ''),
    display_name: song.display_name || prev.display_name || 'Suno Creator',
    image_url: song.image_url || prev.image_url || '/favicon.png',
    play_count: Number(song.play_count ?? prev.play_count ?? 0),
    upvote_count: Number(song.upvote_count ?? prev.upvote_count ?? 0),
    duration,
    metadata: {
      ...(prev.metadata || {}),
      ...(song.metadata || {}),
      tags,
      prompt,
      duration
    }
  };
}

function getLyricsSnippetForSong(song) {
  const rawPrompt = song?.metadata?.prompt || song?.prompt || '';
  if (rawPrompt && typeof rawPrompt === 'string') {
    const cleanLines = rawPrompt
      .split(/\r?\n/)
      .map(l => l.trim())
      .filter(
        l =>
          l &&
          !/^\[.*\]$/.test(l) &&
          !/^[=*\-_~⸻—]{3,}$/.test(l) &&
          !/^[\{\}\[\],]+$/.test(l) &&
          !/^"[a-zA-Z0-9_-]+"\s*:/.test(l) &&
          !/^(youtube|muzik\s*vn|soundcloud|tiktok|facebook|http)/i.test(l)
      );
    if (cleanLines.length > 0) {
      return `🎤 ${cleanLines[0]}`;
    }
  }
  if (typeof window.kvGetShortLyricsPreview === 'function') {
    const preview = window.kvGetShortLyricsPreview(song);
    if (preview && !preview.includes('"title":') && !preview.includes('🎤 {')) {
      return preview;
    }
  }
  return `🎤 Giai điệu "${song?.title || 'Suno AI'}" — @${(song?.handle || 'futurescall').replace(/^@/, '')}`;
}

function parseStyleTags(song) {
  const raw = song?.metadata?.tags || song?.tags || 'V-Pop, Studio Master';
  const parts = raw
    .split(/[,;|/]+/)
    .map(s =>
      s
        .replace(/^(style|genre|tempo|key)\s*:\s*/i, '')
        .replace(/\b(track|medium tempo|slow tempo|fast tempo|with|and)\b/gi, '')
        .replace(/\s{2,}/g, ' ')
        .trim()
    )
    .filter(s => s.length >= 2 && !s.includes('http') && !s.startsWith('{'))
    .map(s => (s.length > 20 ? `${s.slice(0, 19).trim()}…` : s));
  if (parts.length === 0) return ['V-Pop', 'Suno v4'];
  if (parts.length === 1) return [parts[0], 'Suno Studio'];
  return parts.slice(0, 2);
}

// ============================================================================
// 5. Data Normalization, Seeding & Live API Fetching
// ============================================================================
function normalizeSongRecord(raw, defaultHandle = 'futurescall', defaultDisplayName = 'Suno Creator') {
  if (!raw || !raw.id) return null;
  const id = String(raw.id).toLowerCase();
  const reg = (window.kvTrackRegistry && window.kvTrackRegistry[id]) || {};
  const handle = String(raw.handle || raw.username || reg.handle || defaultHandle).replace(/^@/, '').trim();
  const display_name = raw.display_name || raw.creator_name || reg.display_name || defaultDisplayName || handle;
  const play_count = Number(raw.play_count ?? raw.plays ?? reg.play_count ?? 0);
  const upvote_count = Number(raw.upvote_count ?? raw.upvotes ?? raw.likes ?? reg.upvote_count ?? Math.max(1, Math.round(play_count * 0.025)));
  const duration = Number(raw.duration ?? raw.metadata?.duration ?? reg.metadata?.duration ?? 185);
  const tags = raw.metadata?.tags || raw.tags || reg.metadata?.tags || 'Vietnamese V-Pop, Studio Master';
  const prompt = raw.metadata?.prompt || raw.prompt || reg.metadata?.prompt || '';
  const hot_score = Number(raw.hot_score ?? (play_count + upvote_count * 18));

  const normalized = {
    id,
    title: String(raw.title || reg.title || 'Untitled Track').trim(),
    handle,
    display_name,
    image_url: raw.image_url || raw.image_large_url || reg.image_url || `https://cdn2.suno.ai/image_${id}.jpeg`,
    audio_url: `/api/suno-audio-proxy?input=${encodeURIComponent(id)}`,
    play_count,
    upvote_count,
    duration,
    hot_score,
    tags,
    metadata: {
      tags,
      prompt,
      duration
    }
  };

  registerTrackInGlobalRegistry(normalized);
  return normalized;
}

function normalizeCreatorRecord(raw, index = 0) {
  const username = String(raw.username || raw.handle || `creator_${index}`).replace(/^@/, '').trim();
  const seedMatch = SEED_CREATORS_DATA.find(c => c.username.toLowerCase() === username.toLowerCase());

  const candidateTopSongs = Array.isArray(raw.top_songs)
    ? raw.top_songs
    : Array.isArray(raw.clips)
      ? raw.clips.slice(0, 3)
      : [];
  const hasOnlySamplePlaceholders =
    candidateTopSongs.length > 0 && candidateTopSongs.every(s => s && s._isSample);

  const rawTopSongs =
    candidateTopSongs.length > 0 && !(hasOnlySamplePlaceholders && seedMatch?.top_songs?.length)
      ? candidateTopSongs
      : (seedMatch?.top_songs || candidateTopSongs);

  const display_name = raw.display_name || raw.name || seedMatch?.display_name || username;
  const top_songs = rawTopSongs
    .map(s => normalizeSongRecord(s, username, display_name))
    .filter(Boolean)
    .slice(0, 3);

  const computedPlaysFromSongs = top_songs.reduce((acc, s) => acc + (s.play_count || 0), 0);
  const total_plays = Math.max(
    Number(raw.total_plays ?? raw.plays ?? raw.play_count ?? seedMatch?.total_plays ?? 0),
    computedPlaysFromSongs
  );
  const tracks_count = Math.max(
    Number(raw.tracks_count ?? raw.num_total_clips ?? raw.count ?? seedMatch?.tracks_count ?? top_songs.length),
    top_songs.length
  );
  const total_upvotes = Number(raw.total_upvotes ?? raw.upvotes ?? seedMatch?.total_upvotes ?? Math.round(total_plays * 0.03));

  const styles = Array.isArray(raw.styles) && raw.styles.length > 0
    ? raw.styles
    : Array.isArray(raw.style_tags) && raw.style_tags.length > 0
      ? raw.style_tags
      : typeof raw.style === 'string' && raw.style
        ? raw.style.split(/[,•/]+/).map(s => s.trim()).filter(Boolean)
        : (seedMatch?.styles || ['V-Pop', 'Suno Studio']);

  const style_category = raw.style_category || seedMatch?.style_category || styles[0] || 'V-Pop';
  const topHitPlays = top_songs[0]?.play_count || 0;
  const hot_score = Number(
    raw.hot_score ??
    (Math.log10(total_plays + 10) * 18 + Math.log10(topHitPlays + 10) * 12 + Math.min(12, tracks_count / 80))
  );

  return {
    username,
    display_name,
    avatar_url: raw.avatar_url || raw.avatar_image_url || raw.image_url || seedMatch?.avatar_url || '',
    style_category,
    styles: styles.slice(0, 3),
    total_plays,
    tracks_count,
    total_upvotes,
    hot_score,
    verified: Boolean(raw.verified ?? seedMatch?.verified ?? (total_plays >= 500 || tracks_count >= 50)),
    color: raw.color || seedMatch?.color || '#1ed760',
    top_songs,
    liveLoaded: Boolean(raw.liveLoaded || raw.is_live)
  };
}

function initializeSeedData() {
  if (state.creators.length === 0) {
    state.creators = SEED_CREATORS_DATA.map((c, idx) => normalizeCreatorRecord(c, idx));
  }
  rebuildSongsCatalogFromCreators();
}

function rebuildSongsCatalogFromCreators(extraSongs = []) {
  const map = new Map();

  state.creators.forEach(creator => {
    (creator.top_songs || []).forEach(song => {
      const norm = normalizeSongRecord(song, creator.username, creator.display_name);
      if (norm && !map.has(norm.id)) {
        map.set(norm.id, norm);
      }
    });
  });

  EXTRA_CHART_SONGS.forEach(song => {
    const norm = normalizeSongRecord(song, song.handle, song.display_name);
    if (norm && !map.has(norm.id)) {
      map.set(norm.id, norm);
    }
  });

  extraSongs.forEach(song => {
    const norm = normalizeSongRecord(song, song.handle, song.display_name);
    if (norm && !map.has(norm.id)) {
      map.set(norm.id, norm);
    }
  });

  if (window.kvTrackRegistry) {
    Object.values(window.kvTrackRegistry).forEach(regClip => {
      if (regClip && regClip.id && !map.has(String(regClip.id).toLowerCase())) {
        const norm = normalizeSongRecord(regClip, regClip.handle, regClip.display_name);
        if (norm) map.set(norm.id, norm);
      }
    });
  }

  state.songs = Array.from(map.values());
}

async function fetchJsonSafe(url, options = {}) {
  try {
    const res = await fetch(url, options);
    if (!res.ok) return null;
    const ct = res.headers.get('content-type') || '';
    if (!ct.includes('application/json')) return null;
    return await res.json();
  } catch (e) {
    return null;
  }
}

export async function refreshCreatorsRanking(forceSync = false) {
  initializeSeedData();

  const rankingData = await fetchJsonSafe(`/api/suno-creators-ranking?sort=${encodeURIComponent(state.creatorSort)}${forceSync ? '&refresh=true' : ''}`);
  const rawList = Array.isArray(rankingData)
    ? rankingData
    : (rankingData?.creators || rankingData?.rankings || rankingData?.items || null);

  if (Array.isArray(rawList) && rawList.length > 0) {
    const mergedByHandle = new Map();
    state.creators.forEach(c => mergedByHandle.set(c.username.toLowerCase(), c));
    rawList.forEach((item, idx) => {
      const norm = normalizeCreatorRecord(item, idx);
      mergedByHandle.set(norm.username.toLowerCase(), norm);
    });
    state.creators = Array.from(mergedByHandle.values());
    rebuildSongsCatalogFromCreators();
    renderCreatorsView();
    renderSongsView();
    return;
  }

  const showcaseData = await fetchJsonSafe(`/api/suno-showcase?handle=futurescall${forceSync ? '&refresh=true' : ''}`);
  if (showcaseData && Array.isArray(showcaseData.clips) && showcaseData.clips.length > 0) {
    const fcIdx = state.creators.findIndex(c => c.username.toLowerCase() === 'futurescall');
    const topClips = [...showcaseData.clips]
      .sort((a, b) => (b.play_count || 0) - (a.play_count || 0))
      .slice(0, 3);
    if (fcIdx !== -1) {
      state.creators[fcIdx] = normalizeCreatorRecord({
        ...state.creators[fcIdx],
        display_name: showcaseData.display_name || state.creators[fcIdx].display_name,
        avatar_url: showcaseData.avatar_image_url || state.creators[fcIdx].avatar_url,
        tracks_count: Math.max(state.creators[fcIdx].tracks_count, showcaseData.num_total_clips || showcaseData.clips.length),
        top_songs: topClips,
        liveLoaded: true
      });
    }
    rebuildSongsCatalogFromCreators(showcaseData.clips);
    renderCreatorsView();
    renderSongsView();
  }
}

export async function refreshSongsChart() {
  initializeSeedData();

  const creatorParam = state.songCreator === 'all' ? '' : `&creator=${encodeURIComponent(state.songCreator)}`;
  const chartData = await fetchJsonSafe(`/api/suno-songs-chart?sort=${encodeURIComponent(state.songSort)}${creatorParam}`);
  const rawSongs = Array.isArray(chartData)
    ? chartData
    : (chartData?.songs || chartData?.clips || chartData?.tracks || chartData?.chart || null);

  if (Array.isArray(rawSongs) && rawSongs.length > 0) {
    rebuildSongsCatalogFromCreators(rawSongs);
    renderSongsView();
  }
}

async function fetchCreatorLiveTopSongs(handle, playImmediately = false) {
  const cleanHandle = String(handle || '').replace(/^@/, '').trim();
  if (!cleanHandle || state.loadingHandles.has(cleanHandle.toLowerCase())) return;

  state.loadingHandles.add(cleanHandle.toLowerCase());
  renderCreatorsView();

  try {
    const profileData = await fetchJsonSafe(`/api/suno-profile?handle=${encodeURIComponent(cleanHandle)}&page=1`);
    if (profileData && Array.isArray(profileData.clips) && profileData.clips.length > 0) {
      const sortedClips = [...profileData.clips].sort((a, b) => (b.play_count || 0) - (a.play_count || 0));
      const idx = state.creators.findIndex(c => c.username.toLowerCase() === cleanHandle.toLowerCase());
      if (idx !== -1) {
        const existing = state.creators[idx];
        const totalProfilePlays = sortedClips.reduce((s, c) => s + (c.play_count || 0), 0);
        state.creators[idx] = normalizeCreatorRecord({
          ...existing,
          display_name: profileData.display_name || existing.display_name,
          avatar_url: profileData.avatar_image_url || existing.avatar_url,
          tracks_count: Math.max(existing.tracks_count, profileData.num_total_clips || sortedClips.length),
          total_plays: Math.max(existing.total_plays, totalProfilePlays),
          top_songs: sortedClips.slice(0, 3),
          liveLoaded: true
        });

        rebuildSongsCatalogFromCreators(sortedClips);

        if (playImmediately && state.creators[idx].top_songs.length > 0) {
          playSongInKvPlayer(state.creators[idx].top_songs[0], state.creators[idx].top_songs);
        } else {
          showZenToast(`Loaded top songs for @${cleanHandle}`);
        }
      }
    }
  } finally {
    state.loadingHandles.delete(cleanHandle.toLowerCase());
    renderCreatorsView();
    renderSongsView();
  }
}

function playSongInKvPlayer(song, queueList = null) {
  if (!song) return;
  const norm = normalizeSongRecord(song, song.handle, song.display_name);
  if (!norm) return;

  const normalizedQueue = Array.isArray(queueList) && queueList.length > 0
    ? queueList.map(s => normalizeSongRecord(s, s.handle || norm.handle, s.display_name || norm.display_name)).filter(Boolean)
    : [norm];

  if (window.kvPlayer && typeof window.kvPlayer.playTrack === 'function') {
    window.kvPlayer.playTrack(norm, normalizedQueue, true);
    if (typeof window.kvPlayer.updateNowPlayingPane === 'function') {
      window.kvPlayer.updateNowPlayingPane(norm);
    }
  }
  syncActivePlayingTrackUI();
}

// ============================================================================
// 6. Filtering & Sorting Logic
// ============================================================================
function getSortedAndFilteredCreators() {
  const query = state.creatorSearch.trim().toLowerCase();
  const styleFilter = state.creatorStyle;

  const filtered = state.creators.filter(creator => {
    if (styleFilter !== 'all') {
      const styleHaystack = [creator.style_category, ...(creator.styles || [])].join(' ').toLowerCase();
      const target = styleFilter.toLowerCase();
      if (target.includes('bolero') && !styleHaystack.includes('bolero') && !styleHaystack.includes('trữ tình') && !styleHaystack.includes('quê hương')) return false;
      else if (target.includes('folk trap') && !styleHaystack.includes('folk') && !styleHaystack.includes('trap') && !styleHaystack.includes('xẩm')) return false;
      else if (target.includes('lo-fi') && !styleHaystack.includes('lo-fi') && !styleHaystack.includes('chill') && !styleHaystack.includes('orchestral')) return false;
      else if (target.includes('edm') && !styleHaystack.includes('edm') && !styleHaystack.includes('remix') && !styleHaystack.includes('house')) return false;
      else if (target.includes('indie') && !styleHaystack.includes('indie') && !styleHaystack.includes('acoustic') && !styleHaystack.includes('rock')) return false;
      else if (target === 'v-pop' && !styleHaystack.includes('v-pop') && !styleHaystack.includes('pop') && !styleHaystack.includes('ballad')) return false;
    }

    if (query) {
      const songTitles = (creator.top_songs || []).map(s => `${s.title} ${s.tags || ''}`).join(' ');
      const haystack = `${creator.username} ${creator.display_name} ${creator.styles.join(' ')} ${songTitles}`.toLowerCase();
      if (!haystack.includes(query.replace(/^@/, ''))) return false;
    }
    return true;
  });

  return filtered.sort((a, b) => {
    if (state.creatorSort === 'plays') {
      return (b.total_plays || 0) - (a.total_plays || 0);
    }
    if (state.creatorSort === 'tracks') {
      return (b.tracks_count || 0) - (a.tracks_count || 0);
    }
    return (b.hot_score || 0) - (a.hot_score || 0);
  });
}

function getSortedAndFilteredSongs() {
  const query = state.songSearch.trim().toLowerCase();
  const creatorFilter = state.songCreator;
  const genreFilter = state.songGenre;

  const filtered = state.songs.filter(song => {
    if (creatorFilter !== 'all' && song.handle.toLowerCase() !== creatorFilter.toLowerCase()) {
      return false;
    }

    if (genreFilter !== 'all') {
      const tagHaystack = `${song.title} ${song.metadata?.tags || ''} ${song.tags || ''}`.toLowerCase();
      const g = genreFilter.toLowerCase();
      if (g === 'v-pop' && !/v-pop|vietnamese|pop|nhạc việt/i.test(tagHaystack)) return false;
      if (g === 'ballad' && !/ballad|bolero|trữ tình|acoustic|piano/i.test(tagHaystack)) return false;
      if (g === 'folk trap' && !/trap|folk|đàn tranh|xẩm|rap|808/i.test(tagHaystack)) return false;
      if (g === 'lo-fi' && !/lo-fi|lofi|chill|ambient|lounge/i.test(tagHaystack)) return false;
      if (g.includes('remix') && !/remix|edm|house|vinahouse|dub-step|club|electronic|dance/i.test(tagHaystack)) return false;
      if (g === 'cinematic' && !/cinematic|orchestral|choir|anthem|strings|epic/i.test(tagHaystack)) return false;
    }

    if (query) {
      const haystack = `${song.title} ${song.handle} ${song.display_name} ${song.metadata?.tags || ''} ${song.metadata?.prompt || ''}`.toLowerCase();
      if (!haystack.includes(query.replace(/^@/, ''))) return false;
    }

    return true;
  });

  return filtered.sort((a, b) => {
    if (state.songSort === 'plays') {
      return (b.play_count || 0) - (a.play_count || 0);
    }
    if (state.songSort === 'upvotes') {
      return (b.upvote_count || 0) - (a.upvote_count || 0) || (b.play_count || 0) - (a.play_count || 0);
    }
    const scoreA = (a.play_count || 0) + (a.upvote_count || 0) * 25;
    const scoreB = (b.play_count || 0) + (b.upvote_count || 0) * 25;
    return scoreB - scoreA;
  });
}

// ============================================================================
// 7. Renderers for #kv-zen-creators-view and #kv-zen-songs-view
// ============================================================================
function renderCreatorsView(targetEl = null) {
  const container = targetEl || state.creatorsViewEl || document.getElementById('kv-zen-creators-view');
  if (!container) return;

  const rankedCreators = getSortedAndFilteredCreators();
  const top3 = (rankedCreators.length >= 3 ? rankedCreators : state.creators).slice(0, 3);
  const podiumMedals = ['👑 #1 GOLD', '🥈 #2 SILVER', '🥉 #3 BRONZE'];

  const styleChips = [
    { key: 'all', label: 'All Styles' },
    { key: 'V-Pop', label: 'V-Pop' },
    { key: 'Bolero & Trữ Tình', label: 'Bolero & Trữ Tình' },
    { key: 'Folk Trap', label: 'Folk Trap' },
    { key: 'Lo-Fi & Chill', label: 'Lo-Fi & Chill' },
    { key: 'EDM & Remix', label: 'EDM & Remix' },
    { key: 'Indie & Acoustic', label: 'Indie & Acoustic' }
  ];

  container.innerHTML = `
    <!-- A. Creators Spotlight Hero Banner -->
    <section class="kv-zen-hero-banner creators-hero">
      <div class="kv-zen-hero-left">
        <div class="kv-zen-hero-badge">VERIFIED COMMUNITY LEADERBOARD • ${state.creators.length} CREATORS</div>
        <h1 class="kv-zen-hero-title">Suno Creators &amp; Hot Song Rankings</h1>
        <p class="kv-zen-hero-sub">
          Discover top Vietnamese &amp; global Suno AI producers ranked by hottest tracks, total streams, and catalog size.
        </p>
        <div class="kv-zen-hero-actions">
          <button type="button" class="kv-sp-pill-btn primary" id="kv-creators-play-top-btn">
            ▶ Play Top Creator Hits
          </button>
          <button type="button" class="kv-sp-pill-btn" id="kv-creators-sync-sheet-btn">
            + Sync Google Sheet
          </button>
        </div>
      </div>

      <div class="kv-zen-podium-grid">
        ${top3
          .map((cr, idx) => {
            const initial = (cr.display_name || cr.username || 'S').replace(/[^a-zA-Z0-9À-ỹ]/g, '').charAt(0).toUpperCase() || 'S';
            const topHit = cr.top_songs?.[0];
            return `
              <div class="kv-podium-card podium-${idx + 1}" data-podium-handle="${escapeHtml(cr.username)}">
                <span class="kv-podium-medal">${podiumMedals[idx]}</span>
                <div class="kv-podium-top-row">
                  <div class="kv-podium-avatar">
                    ${
                      cr.avatar_url
                        ? `<img src="${escapeHtml(cr.avatar_url)}" alt="${escapeHtml(cr.display_name)}" onerror="this.style.display='none';this.parentNode.textContent='${initial}';" />`
                        : initial
                    }
                  </div>
                  <div style="min-width:0;flex:1;text-align:left;">
                    <div style="font-size:13px;font-weight:800;color:#fff;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;" title="${escapeHtml(cr.display_name)}">${escapeHtml(cr.display_name)}</div>
                    <div style="font-size:11px;color:#b3b3b3;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">@${escapeHtml(cr.username)}</div>
                  </div>
                </div>
                <div style="font-size:11.5px;font-weight:700;color:#1ed760;font-variant-numeric:tabular-nums;">
                  🎧 ${formatCompactCount(cr.total_plays)} • ${cr.tracks_count} tracks
                </div>
                <button type="button" class="kv-podium-play-btn" data-podium-play="${escapeHtml(cr.username)}" title="Play #1 Hit: ${escapeHtml(topHit?.title || '')}">
                  ▶ Play #1 Hit
                </button>
              </div>
            `;
          })
          .join('')}
      </div>
    </section>

    <!-- B. Structured 2-Row Ranking & Filter Toolbar -->
    <section class="kv-zen-control-bar">
      <div class="kv-zen-toolbar-top">
        <div class="kv-zen-control-group" role="tablist" aria-label="Creator Ranking Mode">
          <button type="button" class="kv-zen-sort-pill ${state.creatorSort === 'hot' ? 'active' : ''}" data-creator-sort="hot">
            🔥 Hot Songs Ranking
          </button>
          <button type="button" class="kv-zen-sort-pill ${state.creatorSort === 'plays' ? 'active' : ''}" data-creator-sort="plays">
            🎧 Most Played Songs
          </button>
          <button type="button" class="kv-zen-sort-pill ${state.creatorSort === 'tracks' ? 'active' : ''}" data-creator-sort="tracks">
            💿 Most Published Tracks
          </button>
        </div>

        <div class="kv-zen-toolbar-right">
          <div class="kv-zen-control-search">
            <input
              type="search"
              id="kv-creators-search-input"
              class="kv-zen-search-input"
              placeholder="Filter @handle, creator, or song..."
              value="${escapeHtml(state.creatorSearch)}"
              autocomplete="off"
            />
          </div>
          <span class="kv-zen-toolbar-count">${rankedCreators.length} Creators</span>
        </div>
      </div>

      <div class="kv-zen-toolbar-bottom">
        <div class="kv-zen-filter-group">
          ${styleChips
            .map(
              chip => `
              <button
                type="button"
                class="kv-zen-filter-pill ${state.creatorStyle === chip.key ? 'active' : ''}"
                data-creator-style="${escapeHtml(chip.key)}"
              >
                ${escapeHtml(chip.label)}
              </button>
            `
            )
            .join('')}
        </div>
      </div>
    </section>

    <!-- C. Creators Ranking Grid -->
    <section id="kv-zen-creators-grid">
      ${
        rankedCreators.length === 0
          ? `<div style="grid-column: 1 / -1; padding: 40px 24px; text-align: center; background: #181818; border-radius: 12px; color: #b3b3b3;">
               <p style="font-size: 15px; font-weight: 700; color: #fff; margin: 0 0 8px;">No matching creators found</p>
               <p style="font-size: 13px; margin: 0 0 14px;">Try clearing your search filter or switching to All Styles.</p>
               <button type="button" class="kv-sp-pill-btn primary" id="kv-creators-reset-filters">Reset Filters</button>
             </div>`
          : rankedCreators
              .map((cr, idx) => {
                const rank = idx + 1;
                const rankClass = rank === 1 ? 'rank-1' : rank === 2 ? 'rank-2' : rank === 3 ? 'rank-3' : 'rank-n';
                const initial = (cr.display_name || cr.username || 'S').replace(/[^a-zA-Z0-9À-ỹ]/g, '').charAt(0).toUpperCase() || 'S';
                const isLoadingLive = state.loadingHandles.has(cr.username.toLowerCase());
                const activeTrackId = window.kvPlayer?.currentTrack?.id;
                const isPlaying = Boolean(window.kvPlayer?.isPlaying);

                return `
                  <article class="kv-creator-rank-card" data-rank="${rank}" data-creator-card="${escapeHtml(cr.username)}">
                    <!-- Card Top Row -->
                    <div class="kv-creator-card-top">
                      <span class="kv-creator-rank-badge ${rankClass}">#${rank}</span>
                      <div class="kv-creator-avatar-wrap" style="width:52px;height:52px;">
                        ${
                          cr.avatar_url
                            ? `<img src="${escapeHtml(cr.avatar_url)}" alt="${escapeHtml(cr.display_name)}" loading="lazy" onerror="this.style.display='none';this.parentNode.textContent='${initial}';" />`
                            : initial
                        }
                      </div>
                      <div class="kv-creator-identity">
                        <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">
                          <div style="display:flex;align-items:center;gap:5px;min-width:0;">
                            <h3 class="kv-creator-display-name" title="${escapeHtml(cr.display_name)}">${escapeHtml(cr.display_name)}</h3>
                            ${cr.verified ? `<span title="Verified Suno Creator" style="flex-shrink:0;display:inline-flex;">${ZEN_ICONS.verified}</span>` : ''}
                          </div>
                          <span style="font-size:12px;font-weight:800;color:#1ed760;font-variant-numeric:tabular-nums;white-space:nowrap;flex-shrink:0;">
                            🎧 ${formatCompactCount(cr.total_plays)}
                          </span>
                        </div>
                        <div style="display:flex;align-items:center;justify-content:space-between;gap:8px;">
                          <span class="kv-creator-handle-sub">@${escapeHtml(cr.username)}</span>
                          <span style="font-size:11px;font-weight:600;color:#8e8e8e;font-variant-numeric:tabular-nums;white-space:nowrap;">
                            ${(cr.tracks_count || 0).toLocaleString('en-US')} tracks
                          </span>
                        </div>
                        <div class="kv-creator-style-pills" style="margin-top:5px;">
                          ${(cr.styles || [])
                            .map(st => `<span class="kv-creator-style-pill">${escapeHtml(st)}</span>`)
                            .join('')}
                        </div>
                      </div>
                    </div>

                    <!-- Card Middle: Hot & Most Played Songs Mini-Chart -->
                    <div class="kv-creator-mini-chart">
                      <div class="kv-mini-chart-header">
                        <span>🔥 Hot &amp; Most Played Songs</span>
                        <span role="button" tabindex="0" class="kv-mini-load-trigger" data-load-live-handle="${escapeHtml(cr.username)}">
                          ${isLoadingLive ? 'Loading...' : cr.liveLoaded ? '✓ Live Synced' : 'Load Top Songs'}
                        </span>
                      </div>
                      ${(cr.top_songs || [])
                        .slice(0, 3)
                        .map((song, sIdx) => {
                          const isCurrent = activeTrackId && song.id === activeTrackId;
                          const stateAttr = isCurrent && isPlaying ? 'pause' : 'play';
                          return `
                            <div
                              class="kv-creator-mini-row ${isCurrent ? 'kv-sp-active-card' : ''}"
                              data-kv-track-id="${escapeHtml(song.id)}"
                              data-mini-creator="${escapeHtml(cr.username)}"
                              data-mini-song-idx="${sIdx}"
                            >
                              <span style="width:20px;font-size:11px;font-weight:800;color:#8e8e8e;font-variant-numeric:tabular-nums;">#${sIdx + 1}</span>
                              <div class="kv-creator-mini-cover" style="width:36px;height:36px;">
                                <img src="${escapeHtml(song.image_url)}" alt="${escapeHtml(song.title)}" loading="lazy" onerror="this.src='/favicon.png'" />
                              </div>
                              <div style="min-width:0;flex:1;">
                                <div class="kv-mini-song-title" style="font-size:12.5px;font-weight:700;color:#fff;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;" title="${escapeHtml(song.title)}">${escapeHtml(song.title)}</div>
                              </div>
                              <span style="font-size:11px;font-weight:600;color:#b3b3b3;font-variant-numeric:tabular-nums;flex-shrink:0;">🎧 ${(song.play_count || 0).toLocaleString('en-US')}</span>
                              <button
                                type="button"
                                class="kv-mini-song-play kv-sp-card-play-fab"
                                data-state="${stateAttr}"
                                title="Play ${escapeHtml(song.title)}"
                              >
                                ${stateAttr === 'pause' ? ZEN_ICONS.pause : ZEN_ICONS.play}
                              </button>
                            </div>
                          `;
                        })
                        .join('')}
                    </div>

                    <!-- Card Footer Row -->
                    <div class="kv-creator-card-footer">
                      <button type="button" class="kv-creator-action-btn primary" data-creator-radio="${escapeHtml(cr.username)}">
                        ▶ Play Creator Radio
                      </button>
                      <button type="button" class="kv-creator-action-btn" data-creator-explore="${escapeHtml(cr.username)}">
                        Explore All ${cr.tracks_count} Songs →
                      </button>
                    </div>
                  </article>
                `;
              })
              .join('')
      }
    </section>
  `;

  bindCreatorsViewEvents(container, rankedCreators);
  window.kvUpdateZenHistoryUI?.();
}

function bindCreatorsViewEvents(container, rankedCreators) {
  container.querySelectorAll('[data-zen-go-back]').forEach(btn => {
    btn.addEventListener('click', () => {
      if (typeof window.kvGoBack === 'function') {
        window.kvGoBack();
      } else if (typeof window.kvSwitchZenMode === 'function') {
        window.kvSwitchZenMode('discover');
      }
    });
  });

  container.querySelector('#kv-creators-play-top-btn')?.addEventListener('click', () => {
    const topCreator = rankedCreators[0] || state.creators[0];
    if (topCreator && topCreator.top_songs?.length > 0) {
      playSongInKvPlayer(topCreator.top_songs[0], topCreator.top_songs);
      showZenToast(`Playing #1 Creator Hits from ${topCreator.display_name}`);
    }
  });

  container.querySelector('#kv-creators-sync-sheet-btn')?.addEventListener('click', async e => {
    const btn = e.currentTarget;
    btn.disabled = true;
    showZenToast('Crawling Vietnamese Hot Hits & Syncing Leaderboard...');
    try {
      await fetchJsonSafe('/api/fetch-drive-csv', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({})
      });
      const crawlRes = await fetchJsonSafe('/api/suno-crawl-vn-hits', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ waitForCompletion: true })
      });
      await Promise.all([refreshCreatorsRanking(true), refreshSongsChart()]);
      const totalCr = crawlRes?.creators_count || state.creators.length;
      const vnHits = crawlRes?.vietnamese_hot_hits_count || state.songs.length;
      showZenToast(`Synced ${totalCr} creators & ${vnHits} Vietnamese hot hits!`);
    } finally {
      btn.disabled = false;
    }
  });

  container.querySelectorAll('[data-podium-play]').forEach(btn => {
    btn.addEventListener('click', e => {
      e.stopPropagation();
      const handle = btn.getAttribute('data-podium-play');
      const creator = state.creators.find(c => c.username === handle);
      if (creator && creator.top_songs?.length > 0) {
        playSongInKvPlayer(creator.top_songs[0], creator.top_songs);
      }
    });
  });

  container.querySelectorAll('[data-podium-handle]').forEach(card => {
    card.addEventListener('click', e => {
      if (e.target.closest('button')) return;
      const handle = card.getAttribute('data-podium-handle');
      if (handle) filterCreatorsByHandle(handle);
    });
  });

  container.querySelectorAll('[data-creator-sort]').forEach(btn => {
    btn.addEventListener('click', () => {
      const sort = btn.getAttribute('data-creator-sort');
      if (sort && sort !== state.creatorSort) {
        state.creatorSort = sort;
        renderCreatorsView();
        refreshCreatorsRanking(false);
      }
    });
  });

  container.querySelectorAll('[data-creator-style]').forEach(btn => {
    btn.addEventListener('click', () => {
      const style = btn.getAttribute('data-creator-style');
      if (style) {
        state.creatorStyle = style;
        renderCreatorsView();
      }
    });
  });

  const searchInput = container.querySelector('#kv-creators-search-input');
  if (searchInput) {
    searchInput.addEventListener('input', e => {
      const pos = e.target.selectionStart;
      state.creatorSearch = e.target.value;
      renderCreatorsView();
      const nextInput = document.getElementById('kv-creators-search-input');
      if (nextInput) {
        nextInput.focus();
        try { nextInput.setSelectionRange(pos, pos); } catch (err) {}
      }
    });
  }

  container.querySelector('#kv-creators-reset-filters')?.addEventListener('click', () => {
    state.creatorSearch = '';
    state.creatorStyle = 'all';
    renderCreatorsView();
  });

  container.querySelectorAll('.kv-creator-mini-row').forEach(row => {
    row.addEventListener('click', e => {
      e.stopPropagation();
      const handle = row.getAttribute('data-mini-creator');
      const sIdx = parseInt(row.getAttribute('data-mini-song-idx') || '0', 10);
      const creator = state.creators.find(c => c.username === handle);
      const song = creator?.top_songs?.[sIdx];
      if (song) {
        playSongInKvPlayer(song, creator.top_songs);
        window.kvPlayer?.toggleLyrics?.(true);
      }
    });
  });

  container.querySelectorAll('[data-load-live-handle]').forEach(btn => {
    btn.addEventListener('click', e => {
      e.stopPropagation();
      const handle = btn.getAttribute('data-load-live-handle');
      if (handle) fetchCreatorLiveTopSongs(handle, false);
    });
  });

  container.querySelectorAll('[data-creator-radio]').forEach(btn => {
    btn.addEventListener('click', e => {
      e.stopPropagation();
      const handle = btn.getAttribute('data-creator-radio');
      const creator = state.creators.find(c => c.username === handle);
      if (creator) {
        if (creator.top_songs?.length > 0) {
          playSongInKvPlayer(creator.top_songs[0], creator.top_songs);
          showZenToast(`Playing @${creator.username} Radio`);
        } else {
          fetchCreatorLiveTopSongs(handle, true);
        }
      }
    });
  });

  container.querySelectorAll('[data-creator-explore]').forEach(btn => {
    btn.addEventListener('click', e => {
      e.stopPropagation();
      const handle = btn.getAttribute('data-creator-explore');
      if (!handle) return;
      navigateToCreatorInDiscover(handle);
    });
  });
}

function navigateToCreatorInDiscover(handle) {
  const cleanHandle = String(handle || 'futurescall').replace(/^@/, '').trim();
  if (typeof window.kvSelectCreatorChannel === 'function') {
    window.kvSelectCreatorChannel(cleanHandle);
    return;
  }

  const discoverNavBtn = document.querySelector('.kv-sp-lib-item[data-zen-mode="discover"]');
  if (discoverNavBtn) {
    discoverNavBtn.click();
  } else {
    document.body.setAttribute('data-zen-view', 'discover');
  }

  const creatorsSubPill = document.querySelector('#kv-zen-discover-bar [data-discover-sub="creators"]');
  if (creatorsSubPill) {
    creatorsSubPill.click();
  }

  setTimeout(() => {
    if (typeof window.kvSelectCreator === 'function') {
      window.kvSelectCreator(cleanHandle);
      showZenToast(`Exploring @${cleanHandle} full catalog`);
    }
  }, 80);
}

// ============================================================================
// 8. Renderer for #kv-zen-songs-view (Songs Chart Hero, Filter Bar, Table & Grid)
// ============================================================================
function renderSongsView(targetEl = null) {
  const container = targetEl || state.songsViewEl || document.getElementById('kv-zen-songs-view');
  if (!container) return;

  const chartSongs = getSortedAndFilteredSongs();
  const top4Mosaic = (chartSongs.length >= 4 ? chartSongs : state.songs).slice(0, 4);
  const topSong = chartSongs[0] || state.songs[0];
  const totalChartPlays = state.songs.reduce((acc, s) => acc + (s.play_count || 0), 0);

  const sortLabels = {
    hot: 'Hot & Trending',
    plays: 'Most Played',
    upvotes: 'Most Upvoted'
  };

  const genrePills = [
    { key: 'all', label: 'All' },
    { key: 'V-Pop', label: 'V-Pop' },
    { key: 'Ballad', label: 'Ballad' },
    { key: 'Folk Trap', label: 'Folk Trap' },
    { key: 'Lo-Fi', label: 'Lo-Fi' },
    { key: 'Remix / EDM', label: 'Remix / EDM' },
    { key: 'Cinematic', label: 'Cinematic' }
  ];

  const activeTrackId = window.kvPlayer?.currentTrack?.id;
  const isPlaying = Boolean(window.kvPlayer?.isPlaying);

  container.innerHTML = `
    <!-- A. Songs Chart Hero Banner (Left: Title & CTAs, Right: Top 4 Mosaic) -->
    <section class="kv-zen-hero-banner songs-hero">
      <div class="kv-zen-hero-left">
        <div class="kv-zen-hero-badge">OFFICIAL SUNO CHART • LIVE STREAMING</div>
        <h1 class="kv-zen-hero-title">Hot &amp; Most Played Songs</h1>
        <p class="kv-zen-hero-sub">
          <strong style="color:#1ed760;">🎧 ${totalChartPlays.toLocaleString('en-US')} Total Chart Streams</strong>
          • Mode: <strong style="color:#fff;">${escapeHtml(sortLabels[state.songSort] || 'Hot & Trending')}</strong>
          ${topSong ? `• #1 Song: <strong style="color:#fbbf24;">"${escapeHtml(topSong.title)}"</strong> by <span style="color:#fff;">@${escapeHtml(topSong.handle)}</span>` : ''}
        </p>
        <div class="kv-zen-hero-actions">
          <button type="button" class="kv-sp-pill-btn primary" id="kv-songs-play-all-btn">
            ▶ Play Top 50 Chart
          </button>
          <button type="button" class="kv-sp-pill-btn" id="kv-songs-shuffle-btn">
            🔀 Shuffle Chart
          </button>
        </div>
      </div>

      <div class="kv-songs-hero-mosaic">
        ${top4Mosaic
          .map(
            (s, idx) => `
            <div class="kv-zen-mosaic-card" data-mosaic-song-id="${escapeHtml(s.id)}" title="Play #${idx + 1}: ${escapeHtml(s.title)}">
              <div class="kv-mosaic-cover-wrap">
                <img src="${escapeHtml(s.image_url)}" alt="${escapeHtml(s.title)}" onerror="this.src='/favicon.png'" />
              </div>
              <div style="min-width:0;flex:1;">
                <div style="font-size:10px;font-weight:800;color:${idx === 0 ? '#f59e0b' : '#1ed760'};letter-spacing:0.05em;">
                  ${idx === 0 ? '👑 #1 HOT SONG' : `#${idx + 1} CHART HIT`}
                </div>
                <div style="font-size:12.5px;font-weight:700;color:#fff;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">${escapeHtml(s.title)}</div>
                <div style="font-size:11px;color:#b3b3b3;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">@${escapeHtml(s.handle)} • 🎧 ${formatCompactCount(s.play_count)}</div>
              </div>
            </div>
          `
          )
          .join('')}
      </div>
    </section>

    <!-- B. Structured 2-Row Songs Toolbar -->
    <section class="kv-zen-control-bar">
      <div class="kv-zen-toolbar-top">
        <div class="kv-zen-control-group" role="tablist" aria-label="Song Chart Ranking">
          <button type="button" class="kv-zen-sort-pill ${state.songSort === 'hot' ? 'active' : ''}" data-song-sort="hot">
            🔥 Hot &amp; Trending
          </button>
          <button type="button" class="kv-zen-sort-pill ${state.songSort === 'plays' ? 'active' : ''}" data-song-sort="plays">
            🎧 Most Played
          </button>
          <button type="button" class="kv-zen-sort-pill ${state.songSort === 'upvotes' ? 'active' : ''}" data-song-sort="upvotes">
            ❤️ Most Upvoted
          </button>
        </div>

        <div class="kv-zen-toolbar-right">
          <select id="kv-songs-creator-select" class="kv-zen-select" aria-label="Filter by Creator">
            <option value="all" ${state.songCreator === 'all' ? 'selected' : ''}>All Creators ▾</option>
            ${state.creators
              .map(
                cr => `
                <option value="${escapeHtml(cr.username)}" ${state.songCreator.toLowerCase() === cr.username.toLowerCase() ? 'selected' : ''}>
                  @${escapeHtml(cr.username)}
                </option>
              `
              )
              .join('')}
          </select>

          <div class="kv-zen-control-search">
            <input
              type="search"
              id="kv-songs-search-input"
              class="kv-zen-search-input"
              placeholder="Search song, @creator, or lyrics..."
              value="${escapeHtml(state.songSearch)}"
              autocomplete="off"
            />
          </div>

          <div class="kv-zen-view-toggle" role="group" aria-label="View Mode">
            <button type="button" class="kv-zen-view-btn ${state.songViewMode === 'table' ? 'active' : ''}" data-song-view="table" title="Chart Table View">
              ☰ Table
            </button>
            <button type="button" class="kv-zen-view-btn ${state.songViewMode === 'grid' ? 'active' : ''}" data-song-view="grid" title="Cover Grid View">
              ⊞ Grid
            </button>
          </div>
        </div>
      </div>

      <div class="kv-zen-toolbar-bottom">
        <div class="kv-zen-filter-group">
          ${genrePills
            .map(
              g => `
              <button
                type="button"
                class="kv-zen-filter-pill ${state.songGenre === g.key ? 'active' : ''}"
                data-song-genre="${escapeHtml(g.key)}"
              >
                ${escapeHtml(g.label)}
              </button>
            `
            )
            .join('')}
        </div>
        <span class="kv-zen-toolbar-count">${chartSongs.length} Songs</span>
      </div>
    </section>

    <!-- C. Songs Display Area (#kv-zen-songs-content) -->
    <section id="kv-zen-songs-content">
      ${
        chartSongs.length === 0
          ? `<div style="padding: 40px 24px; text-align: center; background: #181818; border-radius: 12px; color: #b3b3b3;">
               <p style="font-size: 15px; font-weight: 700; color: #fff; margin: 0 0 8px;">No songs match your current filters</p>
               <p style="font-size: 13px; margin: 0 0 14px;">Reset creator, genre, or search filters to view the full chart.</p>
               <button type="button" class="kv-sp-pill-btn primary" id="kv-songs-reset-filters">Show All Chart Songs</button>
             </div>`
          : state.songViewMode === 'table'
            ? renderSongsTableMarkup(chartSongs, activeTrackId, isPlaying)
            : renderSongsGridMarkup(chartSongs, activeTrackId, isPlaying)
      }
    </section>
  `;

  bindSongsViewEvents(container, chartSongs);
  window.kvUpdateZenHistoryUI?.();
}

function renderSongsTableMarkup(chartSongs, activeTrackId, isPlaying) {
  return `
    <div class="kv-songs-chart-wrap">
      <table class="kv-songs-chart-table">
        <colgroup>
          <col class="kv-col-rank" />
          <col class="kv-col-track" />
          <col class="kv-col-dna" />
          <col class="kv-col-lyrics" />
          <col class="kv-col-streams" />
          <col class="kv-col-action" />
        </colgroup>
        <thead>
          <tr>
            <th style="text-align: center;">#</th>
            <th>Track</th>
            <th>Style DNA</th>
            <th>Lyrics Preview</th>
            <th style="text-align: right;">Streams</th>
            <th style="text-align: right;"><span class="kv-th-desktop">Time &amp; Studio</span><span class="kv-th-mobile">Studio</span></th>
          </tr>
        </thead>
        <tbody>
          ${chartSongs
            .map((song, idx) => {
              const rank = idx + 1;
              const rankClass = rank === 1 ? 'rank-1' : rank === 2 ? 'rank-2' : rank === 3 ? 'rank-3' : 'rank-n';
              const isCurrent = activeTrackId && song.id === activeTrackId;
              const stateAttr = isCurrent && isPlaying ? 'pause' : 'play';
              const [tag1, tag2] = parseStyleTags(song);
              const lyricsSnippet = getLyricsSnippetForSong(song);

              return `
                <tr
                  class="kv-songs-chart-row ${isCurrent ? 'kv-sp-active-card' : ''}"
                  data-kv-track-id="${escapeHtml(song.id)}"
                  data-chart-song-idx="${idx}"
                >
                  <td class="kv-song-rank-cell">
                    <span class="kv-song-rank-num ${rankClass}">${rank}</span>
                    <button
                      type="button"
                      class="kv-song-row-play kv-sp-card-play-fab"
                      data-state="${stateAttr}"
                      title="Play ${escapeHtml(song.title)}"
                    >
                      ${stateAttr === 'pause' ? ZEN_ICONS.pause : ZEN_ICONS.play}
                    </button>
                  </td>
                  <td>
                    <div class="kv-song-track-cell">
                      <div class="kv-song-cover-wrap">
                        <img src="${escapeHtml(song.image_url)}" alt="${escapeHtml(song.title)}" loading="lazy" onerror="this.src='/favicon.png'" />
                      </div>
                      <div class="kv-song-meta-col">
                        <div class="kv-song-title-text" title="${escapeHtml(song.title)}">${escapeHtml(song.title)}</div>
                        <div class="kv-song-subline">
                          <span role="button" tabindex="0" class="kv-song-artist-link" data-song-creator-jump="${escapeHtml(song.handle)}">
                            @${escapeHtml(song.handle)}
                          </span>
                          <span class="kv-mobile-song-plays">• 🎧 ${formatCompactCount(song.play_count)}</span>
                          <span class="kv-mobile-song-tag">• ${escapeHtml(tag1)}</span>
                        </div>
                      </div>
                    </div>
                  </td>
                  <td>
                    <div class="kv-song-dna-cell">
                      <button type="button" class="kv-creator-style-pill" data-append-dna="${escapeHtml(tag1)}" title="Append '${escapeHtml(tag1)}' to Prompt Studio">${escapeHtml(tag1)}</button>
                      <button type="button" class="kv-creator-style-pill" data-append-dna="${escapeHtml(tag2)}" title="Append '${escapeHtml(tag2)}' to Prompt Studio">${escapeHtml(tag2)}</button>
                    </div>
                  </td>
                  <td>
                    <div class="kv-song-lyrics-cell" title="${escapeHtml(lyricsSnippet)}">
                      ${escapeHtml(lyricsSnippet)}
                    </div>
                  </td>
                  <td class="kv-song-plays-cell">
                    <div class="kv-song-streams-stack">
                      <span class="kv-song-plays-main">🎧 ${formatCompactCount(song.play_count)}</span>
                      <span class="kv-song-likes-sub">♥ ${formatCompactCount(song.upvote_count)}</span>
                    </div>
                  </td>
                  <td class="kv-song-actions-cell">
                    <span class="kv-song-dur-label kv-tabular">${formatDuration(song.duration)}</span>
                    <button
                      type="button"
                      class="kv-song-studio-pill"
                      data-append-dna="${escapeHtml(song.metadata?.tags || tag1)}"
                      title="Append track style tags to Prompt Studio"
                    >
                      + Studio
                    </button>
                  </td>
                </tr>
              `;
            })
            .join('')}
        </tbody>
      </table>
    </div>
  `;
}

function renderSongsGridMarkup(chartSongs, activeTrackId, isPlaying) {
  return `
    <div class="kv-songs-cover-grid">
      ${chartSongs
        .map((song, idx) => {
          const rank = idx + 1;
          const rankClass = rank === 1 ? 'rank-1' : rank === 2 ? 'rank-2' : rank === 3 ? 'rank-3' : 'rank-n';
          const isCurrent = activeTrackId && song.id === activeTrackId;
          const stateAttr = isCurrent && isPlaying ? 'pause' : 'play';
          const [tag1] = parseStyleTags(song);

          return `
            <article
              class="kv-song-grid-card ${isCurrent ? 'kv-sp-active-card' : ''}"
              data-kv-track-id="${escapeHtml(song.id)}"
              data-chart-song-idx="${idx}"
            >
              <div class="kv-song-grid-art">
                <span class="kv-grid-rank-badge ${rankClass}">#${rank}</span>
                <img src="${escapeHtml(song.image_url)}" alt="${escapeHtml(song.title)}" loading="lazy" onerror="this.src='/favicon.png'" />
                <span class="kv-grid-dur-badge">${formatDuration(song.duration)}</span>
                <button
                  type="button"
                  class="kv-sp-card-play-fab kv-grid-play-fab"
                  data-state="${stateAttr}"
                  title="Play ${escapeHtml(song.title)}"
                >
                  ${stateAttr === 'pause' ? ZEN_ICONS.pause : ZEN_ICONS.play}
                </button>
              </div>
              <div class="kv-song-title-text" style="max-width: 100%;" title="${escapeHtml(song.title)}">${escapeHtml(song.title)}</div>
              <div style="display: flex; align-items: center; justify-content: space-between; font-size: 11.5px; color: #b3b3b3;">
                <span role="button" tabindex="0" class="kv-song-artist-link" data-song-creator-jump="${escapeHtml(song.handle)}">@${escapeHtml(song.handle)}</span>
                <span style="color: #1ed760; font-weight: 700; font-variant-numeric: tabular-nums;">🎧 ${(song.play_count || 0).toLocaleString('en-US')}</span>
              </div>
              <div style="display: flex; align-items: center; justify-content: space-between; gap: 6px; margin-top: auto; padding-top: 6px;">
                <span class="kv-creator-style-pill" style="max-width: 60%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${escapeHtml(tag1)}</span>
                <button
                  type="button"
                  class="kv-song-studio-pill"
                  data-append-dna="${escapeHtml(song.metadata?.tags || tag1)}"
                >
                  + Studio
                </button>
              </div>
            </article>
          `;
        })
        .join('')}
    </div>
  `;
}

function bindSongsViewEvents(container, chartSongs) {
  container.querySelectorAll('[data-zen-go-back]').forEach(btn => {
    btn.addEventListener('click', () => {
      if (typeof window.kvGoBack === 'function') {
        window.kvGoBack();
      } else if (typeof window.kvSwitchZenMode === 'function') {
        window.kvSwitchZenMode('discover');
      }
    });
  });

  container.querySelector('#kv-songs-play-all-btn')?.addEventListener('click', () => {
    if (chartSongs.length > 0) {
      playSongInKvPlayer(chartSongs[0], chartSongs);
      showZenToast(`Playing Top ${chartSongs.length} Suno Chart from #1`);
    }
  });

  container.querySelector('#kv-songs-shuffle-btn')?.addEventListener('click', () => {
    if (chartSongs.length > 0) {
      const shuffled = [...chartSongs].sort(() => Math.random() - 0.5);
      playSongInKvPlayer(shuffled[0], shuffled);
      showZenToast(`Shuffling ${shuffled.length} Charting Songs`);
    }
  });

  container.querySelectorAll('[data-mosaic-song-id]').forEach(tile => {
    tile.addEventListener('click', () => {
      const id = tile.getAttribute('data-mosaic-song-id');
      const found = chartSongs.find(s => s.id === id) || state.songs.find(s => s.id === id);
      if (found) playSongInKvPlayer(found, chartSongs);
    });
  });

  container.querySelectorAll('[data-song-sort]').forEach(btn => {
    btn.addEventListener('click', () => {
      const sort = btn.getAttribute('data-song-sort');
      if (sort && sort !== state.songSort) {
        state.songSort = sort;
        renderSongsView();
        refreshSongsChart();
      }
    });
  });

  container.querySelector('#kv-songs-creator-select')?.addEventListener('change', e => {
    state.songCreator = e.target.value || 'all';
    renderSongsView();
    refreshSongsChart();
  });

  container.querySelectorAll('[data-song-genre]').forEach(btn => {
    btn.addEventListener('click', () => {
      const g = btn.getAttribute('data-song-genre');
      if (g) {
        state.songGenre = g;
        renderSongsView();
      }
    });
  });

  container.querySelectorAll('[data-song-view]').forEach(btn => {
    btn.addEventListener('click', () => {
      const v = btn.getAttribute('data-song-view');
      if (v && v !== state.songViewMode) {
        state.songViewMode = v;
        renderSongsView();
      }
    });
  });

  const searchInput = container.querySelector('#kv-songs-search-input');
  if (searchInput) {
    searchInput.addEventListener('input', e => {
      const pos = e.target.selectionStart;
      state.songSearch = e.target.value;
      renderSongsView();
      const nextInput = document.getElementById('kv-songs-search-input');
      if (nextInput) {
        nextInput.focus();
        try { nextInput.setSelectionRange(pos, pos); } catch (err) {}
      }
    });
  }

  container.querySelector('#kv-songs-reset-filters')?.addEventListener('click', () => {
    state.songCreator = 'all';
    state.songGenre = 'all';
    state.songSearch = '';
    renderSongsView();
  });

  container.querySelectorAll('[data-append-dna]').forEach(btn => {
    btn.addEventListener('click', e => {
      e.stopPropagation();
      const tag = btn.getAttribute('data-append-dna');
      if (tag) appendTagsToPromptStudio(tag);
    });
  });

  container.querySelectorAll('[data-song-creator-jump]').forEach(btn => {
    btn.addEventListener('click', e => {
      e.stopPropagation();
      const handle = btn.getAttribute('data-song-creator-jump');
      if (handle) {
        filterCreatorsByHandle(handle);
      }
    });
  });

  container.querySelectorAll('[data-chart-song-idx]').forEach(el => {
    el.addEventListener('click', e => {
      if (e.target.closest('[data-append-dna]') || e.target.closest('[data-song-creator-jump]')) return;
      const idx = parseInt(el.getAttribute('data-chart-song-idx') || '0', 10);
      const song = chartSongs[idx];
      if (song) {
        playSongInKvPlayer(song, chartSongs);
      }
    });
  });
}

// ============================================================================
// 9. Active Track Play/Pause State Synchronization
// ============================================================================
function syncActivePlayingTrackUI() {
  const activeId = window.kvPlayer?.currentTrack?.id;
  const isPlaying = Boolean(window.kvPlayer?.isPlaying);
  if (!activeId) return;

  document.querySelectorAll('#kv-zen-creators-view [data-kv-track-id], #kv-zen-songs-view [data-kv-track-id]').forEach(el => {
    const isCurrent = el.getAttribute('data-kv-track-id') === activeId;
    el.classList.toggle('kv-sp-active-card', isCurrent);

    const fab = el.querySelector('.kv-sp-card-play-fab');
    if (fab) {
      const desired = isCurrent && isPlaying ? 'pause' : 'play';
      if (fab.getAttribute('data-state') !== desired) {
        fab.setAttribute('data-state', desired);
        fab.innerHTML = desired === 'pause' ? ZEN_ICONS.pause : ZEN_ICONS.play;
      }
    }
  });
}

function hookKvPlayerAudioEvents() {
  if (state.audioHooked || !window.kvPlayer?.audio) return;
  state.audioHooked = true;
  window.kvPlayer.audio.addEventListener('play', syncActivePlayingTrackUI);
  window.kvPlayer.audio.addEventListener('playing', syncActivePlayingTrackUI);
  window.kvPlayer.audio.addEventListener('pause', syncActivePlayingTrackUI);
  window.kvPlayer.audio.addEventListener('ended', syncActivePlayingTrackUI);
}

// ============================================================================
// 10. Public Exported API & Lifecycle Mounting
// ============================================================================
export function ensureZenCustomTabsMounted(mainView = null) {
  ensureScopedZenTabsStyles();
  initializeSeedData();

  const targetMain = mainView || document.getElementById('kv-sp-main-view');
  if (!targetMain) return;

  let creatorsView = targetMain.querySelector('#kv-zen-creators-view') || document.getElementById('kv-zen-creators-view');
  if (!creatorsView) {
    creatorsView = document.createElement('div');
    creatorsView.id = 'kv-zen-creators-view';
    creatorsView.className = 'kv-zen-custom-view';
    targetMain.appendChild(creatorsView);
  }
  state.creatorsViewEl = creatorsView;

  let songsView = targetMain.querySelector('#kv-zen-songs-view') || document.getElementById('kv-zen-songs-view');
  if (!songsView) {
    songsView = document.createElement('div');
    songsView.id = 'kv-zen-songs-view';
    songsView.className = 'kv-zen-custom-view';
    targetMain.appendChild(songsView);
  }
  state.songsViewEl = songsView;

  if (!state.mounted) {
    state.mounted = true;
    renderCreatorsView(creatorsView);
    renderSongsView(songsView);
    hookKvPlayerAudioEvents();

    const bodyObserver = new MutationObserver(() => {
      const currentMode = document.body.getAttribute('data-zen-view');
      if (state.creatorsViewEl) {
        const isCreators = currentMode === 'creators';
        state.creatorsViewEl.classList.toggle('active', isCreators);
        state.creatorsViewEl.style.display = isCreators ? 'flex' : 'none';
      }
      if (state.songsViewEl) {
        const isSongs = currentMode === 'songs';
        state.songsViewEl.classList.toggle('active', isSongs);
        state.songsViewEl.style.display = isSongs ? 'flex' : 'none';
      }
    });
    bodyObserver.observe(document.body, { attributes: true, attributeFilter: ['data-zen-view'] });

    setTimeout(() => {
      refreshCreatorsRanking(false);
      refreshSongsChart();
    }, 120);
  }
}

export function activateZenCreatorsTab(options = {}) {
  ensureZenCustomTabsMounted();
  hookKvPlayerAudioEvents();

  if (options.sort && ['hot', 'plays', 'tracks'].includes(options.sort)) {
    state.creatorSort = options.sort;
  }
  if (options.style) {
    state.creatorStyle = options.style;
  }
  if (typeof options.search === 'string') {
    state.creatorSearch = options.search;
  }

  if (document.body.getAttribute('data-zen-view') !== 'creators') {
    document.body.setAttribute('data-zen-view', 'creators');
  }

  const creatorsView = state.creatorsViewEl || document.getElementById('kv-zen-creators-view');
  const songsView = state.songsViewEl || document.getElementById('kv-zen-songs-view');
  const likedView = document.getElementById('kv-sp-liked-playlist-view');
  const rootEl = document.getElementById('root');

  if (creatorsView) {
    creatorsView.style.display = 'flex';
    creatorsView.classList.add('active');
  }
  if (songsView) {
    songsView.style.display = 'none';
    songsView.classList.remove('active');
  }
  if (likedView) likedView.classList.remove('active');
  if (rootEl) rootEl.style.display = 'none';

  document.querySelectorAll('.kv-sp-lib-item[data-zen-mode]').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-zen-mode') === 'creators');
  });

  renderCreatorsView(creatorsView);
  document.getElementById('kv-sp-main-view')?.scrollTo({ top: 0, behavior: 'smooth' });
}

export function activateZenSongsTab(options = {}) {
  ensureZenCustomTabsMounted();
  hookKvPlayerAudioEvents();

  if (options.sort && ['hot', 'plays', 'upvotes'].includes(options.sort)) {
    state.songSort = options.sort;
  }
  if (options.creator) {
    state.songCreator = options.creator;
  }
  if (options.genre) {
    state.songGenre = options.genre;
  }
  if (typeof options.search === 'string') {
    state.songSearch = options.search;
  }

  if (document.body.getAttribute('data-zen-view') !== 'songs') {
    document.body.setAttribute('data-zen-view', 'songs');
  }

  const creatorsView = state.creatorsViewEl || document.getElementById('kv-zen-creators-view');
  const songsView = state.songsViewEl || document.getElementById('kv-zen-songs-view');
  const likedView = document.getElementById('kv-sp-liked-playlist-view');
  const rootEl = document.getElementById('root');

  if (songsView) {
    songsView.style.display = 'flex';
    songsView.classList.add('active');
  }
  if (creatorsView) {
    creatorsView.style.display = 'none';
    creatorsView.classList.remove('active');
  }
  if (likedView) likedView.classList.remove('active');
  if (rootEl) rootEl.style.display = 'none';

  document.querySelectorAll('.kv-sp-lib-item[data-zen-mode]').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-zen-mode') === 'songs');
  });

  renderSongsView(songsView);
  document.getElementById('kv-sp-main-view')?.scrollTo({ top: 0, behavior: 'smooth' });
}

export function filterCreatorsByHandle(handle = '') {
  const cleanHandle = String(handle || '').replace(/^@/, '').trim();
  state.creatorStyle = 'all';
  state.creatorSearch = '';
  activateZenCreatorsTab();

  if (!cleanHandle) return;

  setTimeout(() => {
    const card = document.querySelector(`.kv-creator-rank-card[data-creator-card="${cleanHandle}"]`);
    if (card) {
      document.querySelectorAll('.kv-creator-rank-card.highlight-pulse').forEach(el => el.classList.remove('highlight-pulse'));
      card.classList.add('highlight-pulse');
      card.scrollIntoView({ behavior: 'smooth', block: 'center' });
      setTimeout(() => card.classList.remove('highlight-pulse'), 2800);
    } else {
      state.creatorSearch = cleanHandle;
      renderCreatorsView();
    }
  }, 60);
}

if (typeof window !== 'undefined') {
  window.kvZenTabs = {
    ensureZenCustomTabsMounted,
    activateZenCreatorsTab,
    activateZenSongsTab,
    filterCreatorsByHandle,
    refreshCreatorsRanking,
    refreshSongsChart
  };

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
      setTimeout(() => ensureZenCustomTabsMounted(), 50);
    });
  } else {
    setTimeout(() => ensureZenCustomTabsMounted(), 50);
  }

  // Automatically sync latest crawled creator & song rankings every 5 minutes
  setInterval(() => {
    if (document.hidden) return;
    const activeInput = document.activeElement?.tagName;
    if (activeInput === 'INPUT' || activeInput === 'TEXTAREA') return;
    refreshCreatorsRanking(false);
    refreshSongsChart();
  }, 5 * 60 * 1000);
}
