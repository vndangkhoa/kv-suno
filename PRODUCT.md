# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

AI music creators, songwriters, producers, and Suno AI enthusiasts who browse real Suno tracks, reverse-engineer acoustic style prompts, study production masterclasses, and build structured multi-verse lyrics and prompts for music generation.

## Product Purpose

**kv-suno** is an all-in-one AI music streaming, discovery, and prompt engineering studio built around the Spotify Web Player ("Encore") 3-pane desktop architecture. It enables users to stream Suno tracks seamlessly (powered by an autonomous local FFmpeg AAC extraction and disk-caching engine), inspect track acoustic DNA and lyrics in real time, and craft studio-grade Suno prompts using a 6-pillar acoustic taxonomy, 2D emotional Vibe Matrix, 3D Binaural SFX lab, and 55 production masterclasses.

## Positioning

Unlike static prompt wikis or generic music players, **kv-suno** fuses a full-fidelity Spotify Desktop 3-pane streaming experience (`Your Library` + `Main Canvas` + `Now Playing & Prompt DNA` + `Bottom Player Bar`) directly with live Suno API track streaming, zero-key local AI prompt/lyrics tools, and one-click prompt reverse-engineering.

## Operating Context

Used primarily in desktop and tablet web browsers during active music production and songwriting sessions alongside Suno.com. Users listen to reference tracks in the persistent player, inspect style tags and `[Intro]/[Verse]/[Chorus]` structures in the Right Now Playing pane, click tags to inject them into the Prompt Studio Builder, and export/copy finished prompts directly into Suno.

## Capabilities and Constraints

- **Autonomous Audio Streaming Engine**: Extracts pristine 192kbps/48kHz AAC `.m4a` audio directly from Suno's official CDN via local FFmpeg, caches to `.cache/audio/`, and serves with RFC 7233 HTTP 206 byte-range seeking.
- **No External AI API Key Required**: Built-in local heuristic and template AI engines power Prompt Optimization, AI Lyrics Generation, Style Explanation, and Audio Reverse-Engineering with zero setup.
- **Spotify Zen ("Pure Stream & Inspector") 3-Pane Shell**:
  - Top Global Bar (`kv-suno` branding, unified search omnibox, primary `Prompt Studio` pill with live tag counter badge, consolidated Studio Tools menu, VI/EN language switch).
  - Left 4-Mode Navigation Rail (`Discover`, `Prompt Studio`, `Sound Wiki`, `Liked Songs`, plus an active Studio Prompt summary card; collapsible to compact icon rail).
  - Center Mode-Scoped Main Canvas (Shows one focused workspace at a time: clean 3-signal Track Showcase & Creator Dropdown in `Discover`, dedicated full-canvas Composer + 6-Pillar Matrix in `Prompt Studio`, unified filterable catalog in `Sound Wiki`, and saved tracks in `Liked Songs`).
  - Right Unified Track DNA & Lyrics Inspector (Compact track header, interactive Style DNA tag pills that inject directly into Prompt Studio, and integrated scrollable Lyrics & Structure card).
  - Bottom Persistent Transport Bar (Track meta, Like heart toggle, Shuffle/Prev/Play/Next/Repeat with active green dot indicators, hover-green scrubber with thumb, Inspector toggle, and hover-green volume bar).

## Brand Commitments

- **Brand Name**: `kv-suno`
- **Visual System**: Authentic Spotify "Encore" Dark Web Player (`#000000` outer frame, `#121212` floating island panes with `8px` radius and `8px` gaps, `#181818` cards, `#282828` hover states, `#1ed760` Spotify Green primary action & playback indicators, dynamic album-art ambient header wash, geometric sans-serif UI typography paired with monospace strictly for prompt code/metatags).

## Evidence on Hand

- Live Suno API integration (`/api/suno-showcase`, `/api/suno-profile`, `/api/suno-similar`, `/api/suno-clip-info`, `/api/suno-audio-proxy`, `/api/download-suno-m4a`).
- Curated catalog of 55 production masterclasses and 200+ acoustic prompt tags across Genres, SFX, Instruments, and Metatags (`public/assets/data-CRajibOq.js`).
- Local Web Audio API synthesizer for instant acoustic tag previewing (`StudioWebAudio`).

## Product Principles

1. **Zero-Friction Playback**: Every track card, table row, and playlist item plays immediately in the persistent Spotify transport bar with zero decode errors and instant range seeking.
2. **Listen-to-Create Bridge**: Every song being played exposes its acoustic style tags and lyric structure in the Right Now Playing pane so listeners can turn inspiration into a new prompt in one click.
3. **True Desktop App Ergonomics**: Locked `100dvh` 3-pane spatial architecture with independent scrolling panes, keyboard shortcuts (`Space`, `M`, `L`, `ArrowLeft/Right`), and responsive collapse on smaller viewports.
