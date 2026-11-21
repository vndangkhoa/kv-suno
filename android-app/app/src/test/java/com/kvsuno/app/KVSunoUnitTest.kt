package com.kvsuno.app

import com.kvsuno.app.data.api.DirectSunoClient
import com.kvsuno.app.data.model.Track
import com.kvsuno.app.data.repository.MusicRepository
import com.kvsuno.app.data.update.UpdateChecker
import com.kvsuno.app.ui.adaptive.FoldPosture
import com.kvsuno.app.ui.adaptive.WidthClass
import com.kvsuno.app.ui.adaptive.computeWindowState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KVSunoUnitTest {

    @Test
    fun widthClass_breakpointsMatchMaterial3AdaptiveSpec() {
        assertEquals(WidthClass.Compact, WidthClass.forWidthDp(390))
        assertEquals(WidthClass.Medium, WidthClass.forWidthDp(600))
        assertEquals(WidthClass.Medium, WidthClass.forWidthDp(839))
        assertEquals(WidthClass.Expanded, WidthClass.forWidthDp(840))
        assertEquals(WidthClass.Expanded, WidthClass.forWidthDp(1200))
    }

    @Test
    fun computeWindowState_coversPhoneFoldableAndTabletModes() {
        // Standard compact phone portrait
        val phone = computeWindowState(widthDp = 390, heightDp = 844)
        assertTrue(phone.isCompact)
        assertFalse(phone.isWide)
        assertFalse(phone.useNavigationRail)
        assertEquals(FoldPosture.Flat, phone.foldPosture)

        // Unfolded foldable portrait (e.g. Z Fold inner display: Medium width, uses BottomNavBar + wide 2-col grids + docked player)
        val foldablePortrait = computeWindowState(widthDp = 690, heightDp = 840)
        assertTrue(foldablePortrait.isWide)
        assertFalse(foldablePortrait.useNavigationRail)
        assertTrue(foldablePortrait.isFoldablePortraitWatch)
        assertFalse(foldablePortrait.isTabletop)

        // Foldable half-open Tabletop flex posture
        val tabletop = computeWindowState(
            widthDp = 690,
            heightDp = 840,
            isHalfOpened = true,
            isHorizontalHinge = true,
            hingeTopDp = 410f,
            hingeHeightDp = 20f
        )
        assertEquals(FoldPosture.Tabletop, tabletop.foldPosture)
        assertTrue(tabletop.isTabletop)
        assertFalse(tabletop.isLandscapeTwoPaneWatch)

        // Foldable half-open Book posture
        val book = computeWindowState(
            widthDp = 840,
            heightDp = 690,
            isHalfOpened = true,
            isHorizontalHinge = false,
            hingeStartDp = 410f,
            hingeWidthDp = 20f
        )
        assertEquals(FoldPosture.Book, book.foldPosture)
        assertTrue(book.isBook)
        assertTrue(book.isLandscapeTwoPaneWatch)
        assertTrue(book.useNavigationRail)

        // Tablet landscape
        val tablet = computeWindowState(widthDp = 1180, heightDp = 760)
        assertEquals(WidthClass.Expanded, tablet.widthClass)
        assertTrue(tablet.isLandscape)
        assertTrue(tablet.isLandscapeTwoPaneWatch)
        assertTrue(tablet.useNavigationRail)
    }

    @Test
    fun directSunoClient_scoreAndOptimizePrompt_producesAllFivePillars() {
        val result = DirectSunoClient.scoreAndOptimizePrompt(
            promptText = "v-pop ballad, piano strings, emotional female vocals, spatial reverb",
            language = "vi"
        )
        assertTrue(result.score in 45..98)
        assertTrue(result.producerGrade in listOf("A+", "A", "B", "C"))
        assertEquals(5, result.pillars.size)
        assertTrue(result.pillars.containsKey("genre"))
        assertTrue(result.pillars.containsKey("mood"))
        assertTrue(result.pillars.containsKey("instruments"))
        assertTrue(result.pillars.containsKey("vocals"))
        assertTrue(result.pillars.containsKey("mixing"))
        assertTrue(result.optimizedPrompt.isNotBlank())
    }

    @Test
    fun directSunoClient_generateLyrics_buildsStructuredSections() {
        val vi = DirectSunoClient.generateLyrics(
            topic = "Đêm Hà Nội",
            genre = "Pop Ballad",
            mood = "Nostalgic",
            vocalType = "Female",
            structure = "Standard Pop",
            language = "vi"
        )
        assertTrue(vi.lyrics.contains("[INTRO]"))
        assertTrue(vi.lyrics.contains("[CHORUS]"))
        assertTrue(vi.lyrics.contains("Đêm Hà Nội"))

        val en = DirectSunoClient.generateLyrics(
            topic = "Midnight Neon",
            genre = "Synthwave",
            mood = "Epic",
            vocalType = "Female",
            structure = "Standard Pop",
            language = "en"
        )
        assertTrue(en.lyrics.contains("[CHORUS]"))
        assertTrue(en.lyrics.contains("Midnight Neon"))
    }

    @Test
    fun musicRepository_parseSunoLyrics_detectsHeadersAndLrcTimestamps() {
        val raw = """
            [INTRO]
            (Piano solo)
            [01:12.50] Walking through the emerald rain
            [CHORUS]
            Hold on to the music
        """.trimIndent()
        val parsed = MusicRepository.parseSunoLyrics(raw)
        assertEquals(5, parsed.size)
        assertTrue(parsed[0].isSectionHeader)
        assertEquals("[INTRO]", parsed[0].text)
        assertEquals(72.5, parsed[2].time, 0.05)
        assertEquals("Walking through the emerald rain", parsed[2].text)
        assertTrue(parsed[3].isSectionHeader)
        assertEquals("[CHORUS]", parsed[3].text)
    }

    @Test
    fun trackModel_parsesStyleTagsAndArtistHandle() {
        val track = Track(
            id = "abc-123",
            title = "Gió Đêm",
            artist = "Futures Call",
            handle = "futurescall",
            tags = "v-pop, ballad, piano | spatial reverb, v-pop"
        )
        assertEquals(listOf("v-pop", "ballad", "piano", "spatial reverb"), track.styleTagList)
        assertEquals("Futures Call • @futurescall", track.displayArtistHandle)
    }

    @Test
    fun updateChecker_comparesSemverTagsAccurately() {
        assertTrue(UpdateChecker.isNewer("v2.0.4", "2.0.3"))
        assertTrue(UpdateChecker.isNewer("2.1.0", "2.0.9"))
        assertFalse(UpdateChecker.isNewer("v2.0.3", "2.0.3"))
        assertFalse(UpdateChecker.isNewer("v2.0.2", "2.0.3"))
    }
}
