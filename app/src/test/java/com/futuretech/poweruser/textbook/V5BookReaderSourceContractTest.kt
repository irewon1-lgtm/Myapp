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

    private fun compactWhitespace(text: String): String = text.replace(Regex("\\s+"), " ")

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
    fun V5RepositoryMergesManifestShardsBeforeValidatingTheBook() {
        val repository = source("com/futuretech/poweruser/textbook/V5BookAssetRepository.kt")
        assertTrue(repository.contains("context.assets.list(trackDir)"))
        assertTrue(repository.contains("manifest_\\\\d{2}\\\\.json"))
        assertTrue(repository.contains("fragments.flatMap { it.parts }.sortedBy { it.order }"))
        assertTrue(repository.contains("validateManifest(merged, trackNumber)"))
        assertTrue(repository.contains("Duplicate part asset paths"))
        assertTrue(repository.contains("Duplicate source-map paths"))
    }

    @Test
    fun V5ReaderLoadsOnlyCurrentPartAndEvidenceBeforeProse() {
        val reader = source("com/futuretech/poweruser/ui/V5TrackBookScreen.kt")
        assertTrue(reader.contains("val part = manifest.parts[partIndex]"))
        assertTrue(reader.contains("repo.loadSourceMap(part)"))
        assertTrue(reader.contains("repo.loadPart(part)"))
        assertFalse(reader.contains("manifest.parts.flatMap"))
        assertFalse(reader.contains("manifest.parts.map { repo.loadPart"))
    }

    @Test
    fun V5ReaderUsesMeasuredPaginationAndUltraThinBookChrome() {
        val reader = source("com/futuretech/poweruser/ui/V5TrackBookScreen.kt")
        assertTrue(reader.contains("MeasuredTextbookPageComposer.paginate("))
        assertTrue(reader.contains("rememberTextMeasurer"))
        assertTrue(reader.contains("detectHorizontalDragGestures"))
        assertTrue(reader.contains("v5_left_tap_zone"))
        assertTrue(reader.contains("v5_right_tap_zone"))
        assertTrue(reader.contains("V5_TOP_HEIGHT = 26.dp"))
        assertTrue(reader.contains("V5_PART_HEIGHT = 18.dp"))
        assertTrue(reader.contains("V5_PAGE_Y = 3.dp"))
        assertTrue(reader.contains("V5_OUTER_Y = 0.dp"))
        assertTrue(reader.contains("V5_FOOTER_HEIGHT = 14.dp"))
        assertTrue(reader.contains("RoundedCornerShape(0.dp)"))
        assertFalse(reader.contains("charsPerLine"))
        assertFalse(reader.contains("/ 27f"))
        assertFalse(reader.contains("LazyColumn"))
        assertFalse(reader.contains("verticalScroll"))
    }

    @Test
    fun measuredComposerTypographyMatchesTheActualReader() {
        val reader = compactWhitespace(source("com/futuretech/poweruser/ui/V5TrackBookScreen.kt"))
        val measured = compactWhitespace(source("com/futuretech/poweruser/ui/MeasuredTextbookPageComposer.kt"))
        listOf(
            "fontSize = 16.5.sp, lineHeight = 27.sp",
            "fontSize = 15.sp, lineHeight = 23.sp",
            "fontSize = 13.sp, lineHeight = 20.sp"
        ).forEach { metric ->
            assertTrue("reader missing metric: $metric", reader.contains(metric))
            assertTrue("measurer missing metric: $metric", measured.contains(metric))
        }
        assertTrue(measured.contains("1 -> 23.sp"))
        assertTrue(measured.contains("2 -> 20.sp"))
        assertTrue(measured.contains("3.dp"))
        assertTrue(measured.contains("26.dp"))
        assertTrue(measured.contains("20.dp"))
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
