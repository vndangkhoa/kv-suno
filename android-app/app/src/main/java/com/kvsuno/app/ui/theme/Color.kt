package com.kvsuno.app.ui.theme

import androidx.compose.ui.graphics.Color

// KV Suno Spotify Zen ("Pure Stream & Inspector") Surface & Background Tokens
val SunoPureBlack = Color(0xFF000000)         // --sp-black: outer frame, gutters, bottom transport bar
val SunoBase = Color(0xFF121212)              // --sp-base: primary island panel surface
val SunoElevated = Color(0xFF181818)          // --sp-elevated: media cards at rest, popovers
val SunoTinted = Color(0xFF1F1F1F)            // --sp-tinted: search omnibox, sub-cards, pills
val SunoHighlight = Color(0xFF282828)         // --sp-highlight: card hover surface, active nav row
val SunoPress = Color(0xFF333333)             // --sp-press: active/pressed surface

// KV Suno Emerald Brand & Semantic Accents
val SunoEmerald = Color(0xFF1ED760)           // --sp-green: primary brand accent & playback indicators
val SunoEmeraldBright = Color(0xFF24E56A)     // --sp-green-hover: hover / highlight emerald
val SunoEmeraldDeep = Color(0xFF0D8A3B)       // deep gradient emerald
val SunoEmeraldMuted = Color(0x261ED760)      // 15% alpha emerald tint for pills & active rail indicator
val SunoAmber = Color(0xFFFFB300)             // creator / warning accent
val SunoCoralRed = Color(0xFFFF5252)          // error / dislike indicator

// KV Suno Neutral Grey & Text Scale
val SunoTextPrimary = Color(0xFFFFFFFF)       // --sp-text-primary
val SunoTextSubdued = Color(0xFFB3B3B3)       // --sp-text-subdued
val SunoTextMuted = Color(0xFF8E8E8E)         // --sp-text-muted
val SunoWhite70 = Color(0xB3FFFFFF)
val SunoWhite20 = Color(0x33FFFFFF)
val SunoWhite15 = Color(0x26FFFFFF)
val SunoWhite10 = Color(0x1AFFFFFF)
val SunoWhite08 = Color(0x14FFFFFF)

// Podium & Chart Medals
val ChartGold = Color(0xFFFFD700)
val ChartSilver = Color(0xFFC0C0C0)
val ChartBronze = Color(0xFFCD7F32)

// Scrim & Overlay Tokens
val SunoDarkerTransparent = Color(0xCC000000)
val SunoBlackAt50 = Color(0x80000000)

// KV Music layout compatibility aliases mapped to KV Suno Spotify Zen Emerald Palette
val YtmBlack = SunoPureBlack
val YtmPureBlack = SunoPureBlack
val YtmGrey13 = SunoBase
val YtmSurfaceContainer = SunoElevated
val YtmGrey12 = SunoBase
val YtmBlack1 = SunoElevated
val YtmGrey11 = SunoElevated
val YtmGrey10 = SunoTinted

val YtmRedAccent = SunoEmerald
val YtmBrandRed = SunoEmerald
val YtmLightRed = SunoCoralRed
val YtmRed02 = SunoEmeraldDeep
val YtmBlue02 = SunoEmeraldBright
val YtmGreen01 = SunoEmerald

val YtmGrey14 = SunoPress
val YtmGrey09 = Color(0xFF535353)
val YtmGrey08 = SunoTextMuted
val YtmGrey06 = SunoTextSubdued
val YtmGrey05 = SunoTextSubdued
val YtmGrey04 = Color(0xFFCCCCCC)
val YtmGrey02 = Color(0xFFEBEBEB)

val YtmWhite = SunoTextPrimary
val YtmPureWhite = SunoTextPrimary
val YtmWhite70 = SunoWhite70
val YtmWhite67 = SunoTextSubdued
val YtmWhite56 = SunoTextMuted
val YtmWhite20 = SunoWhite20
val YtmWhite15 = SunoWhite15
val YtmWhite10 = SunoWhite10
val YtmWhite08 = SunoWhite08

val YtmDarkerTransparent = SunoDarkerTransparent
val YtmBlackAt50 = SunoBlackAt50
val LivePulseGreen = SunoEmerald
