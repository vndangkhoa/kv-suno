# Changelog

All notable changes to **KV Suno (`kv-suno`)** are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.0.0] - 2026-10-14

### 🚀 Major Milestone Release — Spotify Zen Streaming & PWA Media Edition
Version 2.0.0 transforms **KV Suno** from a static prompt wiki into a full-fledged **Spotify Encore AI Music Streaming & Prompt Studio** platform, featuring a live **Hot & Most Played Songs Chart**, **32-Channel Vietnamese & Global Creators Leaderboard**, **Full Lyrics & Style DNA Inspector**, **Mobile-First Responsive Navigation**, **PWA Lock-Screen Media Session Controls** with uninterrupted screen-off background audio, and **1-Click Synology DSM SPK Packaging**.

### Added
- **🔥 Hot & Most Played Songs Chart (`#kv-zen-songs-view` — Default Landing View)**:
  - Made **Hot & Most Played Songs** the default home view on launch (`DEFAULT_ZEN_MODE = 'songs'`), while preserving full instant switching to **Discover**, **Creators**, **Prompt Studio**, **Sound Wiki**, and **Liked Songs**.
  - Added a Spotify Top-50 inspired **Hero Spotlight Banner** featuring a 2×2 dynamic cover art mosaic of the top-charting tracks, live aggregate stream counter, **Play Top Chart** CTA, and **Shuffle Chart** action.
  - Added dual display modes: **Chart Table (`☰`)** with gold/silver/bronze rank badges, cover art, 1-click **Full Lyrics Modal (`🎤 Lyrics`)**, Style DNA tags, play/like counters, and `+ Studio` prompt injection; and **Cover Grid (`⊞`)** with borderless square cards and floating Spotify green Play FABs.
  - Added backend `/api/suno-songs-chart` aggregation service sorting tracks by **Hot Score** (`plays + upvotes × 25`), **Most Played**, or **Most Upvoted**, filterable by creator and genre (`V-Pop`, `Ballad`, `Folk Trap`, `Lo-Fi`, `Remix / EDM`, `Cinematic`).

- **👑 Community Creators Leaderboard & Vietnamese Hot Hits Crawler (`#kv-zen-creators-view`)**:
  - Added dedicated **Creators** tab ranking **32 verified community creators** (`@futurescall`, `@muzikvn`, `@daoxudeo`, `@aimusiccreater`, `@saigonmusic`, `@phamphungoc`, `@neomusiccafe`, `@vietmusic`, `@tina_t`, `@minhanh99`, `@nhat_nam_2301`, `@dominh2025`, and more).
  - Built a **Top 3 Podium Spotlight** (`#1 Gold`, `#2 Silver`, `#3 Bronze`) in the Creators Hero Banner with 1-click **Play #1 Hit** actions.
  - Each Creator Rank Card includes an embedded **Top 3 Hot & Most Played Songs Mini-Chart** with instant inline playback, plus **Play Creator Radio** and **Explore All Songs** actions.
  - Added live Suno Creator Discovery script & enriched `vietnamese-suno-creators.csv` with Vietnamese hot-hit creators (`Nhạc Trẻ`, `V-Pop`, `Bolero`, `Vinahouse`, `Lofi Việt`, `Rap Việt`).

- **🎤 Full Song Lyrics Viewer & Unified Right Track DNA Inspector (`#kv-sp-right-sidebar`)**:
  - Added a permanent 3rd-pane **Now Playing DNA & Lyrics Inspector** on desktop (`>=1100px`) and slide-over sheet on mobile/tablet.
  - Interactive **Style DNA Tags**: click any acoustic tag pill to inject it directly into the active Prompt Studio cart (`#kv-sp-studio-count`), or click **+ Load Full Style into Studio**.
  - **Formatted Full Lyrics & Song Structure**: automatic highlighting for structural metatags (`[Intro]`, `[Verse]`, `[Chorus]`, `[Bridge]`, `[Drop]`, `[Outro]`) with 1-click **Copy Lyrics** and **Expand Full-Screen Lyrics Modal** (`window.kvShowSongLyrics`).

- **📱 Mobile View Architecture & History Back/Forward Navigation**:
  - Added a persistent **Mobile Bottom Tab Bar (`#kv-sp-mobile-nav`)** with 5 primary destinations (`Songs`, `Creators`, `Discover`, `Studio`, `Wiki`) and safe-area inset support (`env(safe-area-inset-bottom)`).
  - Engineered responsive mobile layouts (`<=900px` and `<=600px`) for the Top Bar, Hero Banners, Control Bars, Songs Chart Table (compact 3-column mobile card rows), Creators Podium, and floating mini-player bar.
  - Added stateful **Back (`Alt+←`)** and **Forward (`Alt+→`)** navigation buttons in the top bar integrated with the HTML5 History API (`pushState` / `popstate`) so hardware mobile back gestures and browser back buttons navigate cleanly between tabs and creator profiles.

- **🎧 PWA Lock-Screen Media Controller & Screen-Off Background Audio**:
  - Upgraded `public/site.webmanifest` and `public/index.html` with full standalone PWA capabilities, dark theme metadata (`#09090b`), and iOS/Android home-screen web-app meta tags.
  - Integrated the **W3C Media Session API (`navigator.mediaSession`)** in `enhancements.js`:
    - Synchronizes live song title, `@creator` artist name, album, and multi-resolution cover artwork (`96×96` to `512×512`) to the iOS Lock Screen / Dynamic Island, Android Media Notification, macOS/Windows Media Keys, and Bluetooth car/headphone displays.
    - Registered hardware & lock-screen handlers for `play`, `pause`, `previoustrack`, `nexttrack`, `seekbackward` (`-10s`), `seekforward` (`+10s`), `seekto` (lock-screen scrubbing), and `stop`.
    - Live lock-screen progress bar synchronization via `navigator.mediaSession.setPositionState`.
  - **Uninterrupted Screen-Off Background Playback**:
    - Seamless auto-advance (`ended` handler) that advances to the next track in the queue while the phone screen is locked without waiting for UI animation frames.
    - Web Audio API `AudioContext` keep-alive resume and Screen WakeLock support during active streaming.
    - Resilient network recovery (`stalled` / `waiting` auto-retry) when switching between Wi-Fi and mobile data with the screen off.

- **📦 Synology DSM SPK Package & Multi-Registry Docker Deployment**:
  - Added production `Dockerfile` (`node:20-alpine` with `ffmpeg` for `.m4a` audio stream extraction) and `.dockerignore`.
  - Added Synology DSM 7.x package definition (`kvsuno`) for 1-click installation via `https://syno.vndns.net` and `https://syno.vndns.net/package/kvsuno`.

### Changed
- **Eliminated Cover Art White Borders**: Applied `transform: scale(1.11)` with `overflow: hidden` and dark `#181818` surface tokens across all cover art thumbnails (`SunoExplorerTab`, Songs Chart Table, Cover Grid, Right Inspector, Bottom Player) to crop out baked-in white JPEG frames from upstream Suno thumbnails.
- **Consolidated 6-Block Clutter into Focused Zen Views**: Replaced the cluttered 6-section vertical stack with mode-scoped rendering (`body[data-zen-view]`) so only the active workspace (`songs`, `creators`, `discover`, `studio`, `wiki`, or `liked`) is rendered at any time.
- **De-Terminalized Prompt Studio & Sound Wiki**: Refactored the `nj` Prompt & Lyrics Composer into a static full-width Spotify Encore studio card inside `Prompt Studio` mode, made all 6 Acoustic Pillars interactive with 1-click tag injection, and distilled Sound Wiki cards to 3 clean visual layers.
- **Zero-Stale Asset Caching**: Updated Express static middleware (`server/index.js`) and Service Worker (`public/sw.js`) to serve fresh `.html`, `.js`, and `.css` bundles (`Cache-Control: no-store, no-cache, must-revalidate`) while allowing background audio streams to flow uninterrupted.

---

## [1.0.0] - 2026-09-23

### Added
- Initial standalone release of **KV Suno** (Node.js/Express backend + React SPA frontend).
- **Autonomous Built-In AI Engine** (`/api/generate-lyrics`, `/api/optimize-prompt`, `/api/explain-style`, `/api/generate-guide`) requiring zero external API keys.
- **Suno Audio & Metadata Proxy** (`/api/suno-showcase`, `/api/suno-profile`, `/api/suno-clip-info`, `/api/suno-audio-proxy`) bypassing CORS restrictions on `cdn1.suno.ai` and `audiopipe.suno.ai`.
- **HD Audio Enhancer & `.m4a` Transcoder** (`/api/download-suno-m4a`, `/api/decode-audio-file`) powered by `ffmpeg`.
- **Sound Wiki Catalog**: 200+ genres, instruments, 3D audio SFX tags, metatags, and 55 production masterclasses.
