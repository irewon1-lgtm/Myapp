package com.futuretech.poweruser.textbook

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Source-level guardrails for the V5 reader rewrite. Runtime density/clipping needs instrumented tests. */
class V5MeasuredReaderContractTest {
    private fun source(relative: String): String {
        val candidates = listOf(File("src/main/java/$relative"), File("app/src/main/java/$relative"))
        return candidates.firstOrNull { it.isFile }?.readText(Charsets.UTF_8)
            ?: error("Cannot locate source: $relative")
    }

    @Test
    fun readerNoLongerUsesCharacterAndFixedLineHeightHeuristics() {
        val reader = source("com/futuretech/poweruser/ui/V4PagedBookScreen.kt")
        val paginator = source("com/futuretech/poweruser/ui/MeasuredTextbookPageComposer.kt")

        assertFalse(reader.contains("charsPerLine = when"))
        assertFalse(reader.contains("(heightDp - 102f) / 27f"))
        assertFalse(reader.contains("TextbookPageLayout(charsPerLine, maxLines)"))
        assertTrue("actual Compose text measurement must drive pagination", reader.contains("rememberTextMeasurer"))
        assertTrue("measured paginator must be active", reader.contains("MeasuredTextbookPageComposer"))
        assertTrue(paginator.contains("getLineBottom"))
        assertTrue(paginator.contains("getLineEnd"))
    }

    @Test
    fun compactPhoneReaderDoesNotBurnViewportOnPermanentChrome() {
        val reader = source("com/futuretech/poweruser/ui/V4PagedBookScreen.kt")

        assertFalse("48dp permanent top bar is too tall for the book contract", reader.contains(".height(48.dp)"))
        assertFalse("38dp permanent lesson strip is too tall for the book contract", reader.contains(".height(38.dp)"))
        assertFalse("30x22dp inner padding wastes the content display area", reader.contains(".padding(horizontal = 30.dp, vertical = 22.dp)"))
        assertTrue(reader.contains("BOOK_TOP_BAR_HEIGHT = 34.dp"))
        assertTrue(reader.contains("BOOK_LESSON_STRIP_HEIGHT = 24.dp"))
        assertTrue(reader.contains("BOOK_PAGE_HORIZONTAL_PADDING = 16.dp"))
        assertTrue(reader.contains("BOOK_PAGE_VERTICAL_PADDING = 8.dp"))
    }

    @Test
    fun measuredPaginationStillKeepsBookNavigationAndResume() {
        val reader = source("com/futuretech/poweruser/ui/V4PagedBookScreen.kt")

        assertTrue(reader.contains("textbook_left_tap_zone"))
        assertTrue(reader.contains("textbook_right_tap_zone"))
        assertTrue(reader.contains("detectHorizontalDragGestures"))
        assertTrue(reader.contains("selectedPageIndex(concept.id)"))
        assertTrue(reader.contains("saveSelectedPageIndex(concept.id, it)"))
        assertFalse(reader.contains("LazyColumn"))
        assertFalse(reader.contains("verticalScroll"))
    }

    @Test
    fun orderedListNumbersAndAdaptiveHeadingRuleCannotRegress() {
        val reader = source("com/futuretech/poweruser/ui/V4PagedBookScreen.kt")
        val paginator = source("com/futuretech/poweruser/ui/MeasuredTextbookPageComposer.kt")
        val models = source("com/futuretech/poweruser/textbook/TextbookModels.kt")

        assertTrue(models.contains("val startNumber: Int = 1"))
        assertTrue(reader.contains("block.startNumber + index"))
        assertTrue(paginator.contains("block.startNumber + best"))
        assertTrue(paginator.contains("headingLayout.lineCount <= 1"))
        assertFalse("old unconditional two-body-line heading reserve must not return", paginator.contains("twoBodyLines"))
    }
}
