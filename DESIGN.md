# Design System — kv-suno (Spotify Zen: "Pure Stream & Inspector" v5.0)

## 1. Visual World & Spatial Topology

**kv-suno** adopts the **Spotify Zen ("Pure Stream & Inspector")** 3-pane detached-island architecture on a locked `100dvh` pitch-black (`#000000`) frame, eliminating multi-block page clutter and duplicate floating bars in favor of **one focused mode at a time**:

- **Outer Frame (`body`)**: `100vw × 100dvh`, `overflow: hidden`, `background: #000000`, `padding: 0`, driven by `data-zen-view="discover" | "studio" | "wiki" | "liked"`.
- **Top Global Bar (`#kv-sp-topbar`)**: `58px` height on `#000000`.
  - **Left**: `kv-suno` logo mark (Spotify-inspired sonic wave emblem in `#1ed760` + bold `kv-suno` wordmark) + `<span class="kv-zen-badge">Zen Stream</span>`.
  - **Center**: Home/Discover button (`#1f1f1f`) + Unified Search Omnibox (`Search tracks, @creators, or acoustic tags...`) bi-directionally bound to the React search state.
  - **Right**:
    - Primary **`✦ Prompt Studio`** pill button (`#kv-sp-open-studio`) with a live counter badge (`#kv-sp-studio-count`) reflecting the number of comma-separated tags in the active Studio prompt.
    - Consolidated **`Studio Tools ▾`** popover menu (`#kv-sp-tools-dropdown`) housing `HD Audio DSP` (`24-bit`), `Saved Presets`, and `Language (VI/EN)` without cluttering the header.
- **3-Pane Workspace (`#kv-sp-workspace`)**: `height: calc(100dvh - 58px - 86px)`, `padding: 0 8px`, `gap: 8px`, open in a 3-pane desktop layout by default (`>= 1100px`):
  1. **Left Pane — 4-Item Navigation Rail (`#kv-sp-left-sidebar`)**: `224px` width (collapsible to `64px` icon rail), `background: #121212`, `border-radius: 8px`.
     - **4 Top-Level Modes**:
       1. **`Discover`** (`data-zen-mode="discover"`): Streams Suno tracks & 32 community creator showcases.
       2. **`Prompt Studio`** (`data-zen-mode="studio"`): Distraction-free inline Prompt & Lyrics Composer + 6-Pillar Acoustic Taxonomy & 2D Emotional Vibe Matrix.
       3. **`Sound Wiki`** (`data-zen-mode="wiki"`): Unified catalog of `200+` Genres, Instruments, SFX & Mastering, 3D Audio Lab, Metatags, 55 Masterclasses, and Saved Tags.
       4. **`Liked Songs`** (`data-zen-mode="liked"`): Saved reference tracks with live count badge (`#kv-sp-liked-count`).
     - **Bottom Sub-Panel (`#kv-sp-left-studio-summary`)**: Always-visible **"Active Studio Prompt"** summary card showing live tag count (`#kv-sp-left-studio-count`), 2-line prompt preview (`#kv-sp-left-studio-preview`), and quick **`Open Studio →`** / **`Copy`** actions.
  2. **Center Pane — Mode-Based Main Canvas (`#kv-sp-main-view`)**: `flex: 1`, `background: #121212`, `border-radius: 8px`, independent vertical scroll (`overflow-y: auto`), featuring a dynamic **Ambient Cover-Art Header Gradient** (`#kv-sp-ambient-wash`).
     - **`discover` mode**: Renders sticky `#kv-zen-discover-bar` with segmented pills (`Featured Showcase` and `All 32 Community Creators`) + simplified 3-signal track cards (Cover+Title/@handle + 1 Genre Pill + Hover Play FAB).
     - **`studio` mode**: Renders React's Prompt & Lyrics Composer inline at the top of the canvas (`position: static`), followed immediately by `#block-prompt-studio` (6-Pillar Taxonomy & 2D Matrix).
     - **`wiki` mode**: Renders sticky `#kv-zen-wiki-bar` with category filter pills (`Genres`, `Instruments`, `SFX & Mastering`, `3D Audio Lab`, `Metatags`, `55 Masterclasses`, `Saved Tags`).
     - **`liked` mode**: Renders `#kv-sp-liked-playlist-view` table.
  3. **Right Pane — Unified "Now Playing DNA & Lyrics" Inspector (`#kv-sp-right-sidebar`)**: `316px` width (open by default on desktop `>= 1100px`, toggleable via `L` or bottom bar icons), `background: #121212`, `border-radius: 8px`.
     - **Compact Track Header Card**: `56×56px` cover art + Title + `@handle` + Like heart + Download `.m4a` button.
     - **Style DNA Tags Card**: Interactive `+ tag` pills (click to append to Prompt Studio and increment `#kv-sp-studio-count`) + **`+ Load Full Style into Studio`** CTA.
     - **Lyrics & Structure Card**: Scrollable container (`max-height: 280px`) with highlighted `[Intro]`, `[Verse]`, `[Chorus]` badges and **`Copy Lyrics`** button.
- **Bottom Pane — Persistent Transport Bar (`#kv-music-player`)**: `86px` height on `#000000`, 3-wing layout (`30% / 40% / 30%`) with `#ffffff` 34px circular Play/Pause button, `#1ed760` active indicators, and hover-green scrubber/volume sliders.

## 2. Color Tokens

| Token | Value | Role |
|---|---|---|
| `--sp-black` | `#000000` | App frame background, 8px panel gutters, bottom transport bar |
| `--sp-base` | `#121212` | Primary panel surface (Left Rail, Center Canvas, Right DNA Inspector) |
| `--sp-elevated` | `#181818` | Media cards at rest, dropdown popovers |
| `--sp-tinted` | `#1f1f1f` | Search omnibox, Inspector sub-cards, pill buttons |
| `--sp-highlight` | `#282828` | Card hover surface, active navigation row |
| `--sp-press` | `#333333` | Active/pressed surface |
| `--sp-green` | `#1ed760` | Primary brand accent, hover Play FAB, active track title, scrubber hover fill |
| `--sp-green-hover` | `#1fdf64` | Hover state for primary green buttons |
| `--sp-text-primary` | `#ffffff` | Headings, active navigation, track titles |
| `--sp-text-subdued` | `#b3b3b3` | Secondary text, artist names, inactive nav links, timestamps |
| `--sp-text-muted` | `#8e8e8e` | Section labels, placeholders, subtle metadata |
| `--kv-ambient-rgb` | Dynamic `r, g, b` | Ambient color wash derived from active track |

## 3. Typography

- **UI & Display Font**: `'Plus Jakarta Sans', 'Inter', -apple-system, BlinkMacSystemFont, sans-serif`
- **Code & Prompt Metatag Font**: `'JetBrains Mono', monospace` — scoped to `<code>`, prompt textareas, and `[Metatag]` badges (`tabular-nums` on all timers, durations, and tag counters).
