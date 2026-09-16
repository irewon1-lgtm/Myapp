package com.futuretech.poweruser.textbook

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V5BookReaderSourceContractTest {
    private fun source(relative: String): String {
        val candidates = listOf(File("src/main/java/$relative"), File("app/src/main/java/$relative"))
        return candidates.firstOrNull { it.isFile }?.readText(Charsets.UTF_8)
            ?: error("Cannot locate source: $relative")
    }

    @Test
    fun adaptiveShelfRoutesValidatedV5ManifestsToV5ReaderAndKeepsLegacyFallback() {
        val adaptive = source("com/futuretech/poweruser/ui/AdaptiveTextbookScreen.kt")
        assertTrue(adaptive.contains("V5BookAssetRepository(context)"))
        assertTrue(adaptive.contains("loadManifest(selectedChapter.number)"))
        assertTrue(adaptive.contains("if (hasV5Book)"))
        assertTrue(adaptive.contains("V5TrackBookScreen("))
        assertTrue(adaptive.contains("V4PagedBookScreen("))
    }

    @Test
    fun V5ReaderLoadsOnlyCurrentPartAndEvidenceBeforeProse() {
        val reader = source("com/futuretech/poweruser/ui/V5TrackBookScreen.kt")
        assertTrue(reader.contains("val part = manifest.parts[partIndex]"))
        assertTrue(reader.contains("repo.loadSourceMap(part)"))
        assertTrue(reader.contains("repo.loadPart(part)"))
        assertFalse("V5 must not join every part into one track-sized String", reader.contains("manifest.parts.flatMap"))
        assertFalse("V5 must not eagerly load every part", reader.contains("manifest.parts.map { repo.loadPart"))
    }

    @Test
    fun V5ReaderUsesMeasuredPaginationAndFullPageNavigation() {
        val reader = source("com/futuretech/poweruser/ui/V5TrackBookScreen.kt")
        assertTrue(reader.contains("MeasuredTextbookPageComposer.paginate("))
        assertTrue(reader.contains("rememberTextMeasurer"))
        assertTrue(reader.contains("detectHorizontalDragGestures"))
        assertTrue(reader.contains("v5_left_tap_zone"))
        assertTrue(reader.contains("v5_right_tap_zone"))
        assertTrue(reader.contains("V5_PAGE_Y = 6.dp"))
        assertTrue(reader.contains("V5_OUTER_Y = 1.dp"))
        assertFalse(reader.contains("charsPerLine"))
        assertFalse(reader.contains("/ 27f"))
        assertFalse(reader.contains("LazyColumn"))
        assertFalse(reader.contains("verticalScroll"))
    }

    @Test
    fun V5ReaderPersistsExactPartAndPageResumePoint() {
        val reader = source("com/futuretech/poweruser/ui/V5TrackBookScreen.kt")
        assertTrue(reader.contains("selectedSectionIndex(legacyChapter.id)"))
        assertTrue(reader.contains("saveSelectedSectionIndex(legacyChapter.id"))
        assertTrue(reader.contains("selectedPageIndex(part.id)"))
        assertTrue(reader.contains("saveSelectedPageIndex(part.id, it)"))
        assertTrue(reader.contains("initialPageIndex == Int.MAX_VALUE"))
    }
}
