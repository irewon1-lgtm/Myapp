package com.futuretech.poweruser.textbook

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V4BookReaderSourceContractTest {
    private fun source(relative: String): String {
        val candidates = listOf(File("src/main/java/$relative"), File("app/src/main/java/$relative"))
        return candidates.firstOrNull { it.isFile }?.readText(Charsets.UTF_8)
            ?: error("Cannot locate source: $relative")
    }

    @Test
    fun adaptiveReaderActuallyUsesBookSurface() {
        val adaptive = source("com/futuretech/poweruser/ui/AdaptiveTextbookScreen.kt")

        assertTrue(adaptive.contains("V4PagedBookScreen("))
        assertFalse("old reader must not be the active shell", adaptive.contains("V1TextbookScreen("))
        assertTrue(adaptive.contains("widthIn(max = 780.dp)"))
        assertTrue(adaptive.contains("reader_single_column"))
    }

    @Test
    fun contentPagesUseMeasuredViewportPaginationAndRemainNonScrolling() {
        val reader = source("com/futuretech/poweruser/ui/V4PagedBookScreen.kt")
        val paginator = source("com/futuretech/poweruser/ui/MeasuredTextbookPageComposer.kt")

        assertTrue(reader.contains("BoxWithConstraints"))
        assertTrue(reader.contains("rememberTextMeasurer"))
        assertTrue(reader.contains("MeasuredTextbookPageComposer.paginate"))
        assertTrue(paginator.contains("TextLayoutResult"))
        assertTrue(paginator.contains("getLineBottom"))
        assertTrue(paginator.contains("getLineEnd"))
        assertFalse("fixed 27dp pagination heuristic must not return", reader.contains("/ 27f"))
        assertFalse("character-count pagination must not return", reader.contains("charsPerLine"))
        assertTrue(reader.contains("textbook_left_tap_zone"))
        assertTrue(reader.contains("textbook_right_tap_zone"))
        assertTrue(reader.contains("detectHorizontalDragGestures"))
        assertTrue(reader.contains("textbook_page_indicator"))
        assertFalse(reader.contains("LazyColumn"))
        assertFalse(reader.contains("verticalScroll"))
    }

    @Test
    fun typographyUsesBookFlowAndCompactChrome() {
        val reader = source("com/futuretech/poweruser/ui/V4PagedBookScreen.kt")

        assertTrue("bullets must render as ordinary book flow", reader.contains("is TextbookBlock.BulletList -> Column("))
        assertFalse("bullets must not all be nested cards", reader.contains("is TextbookBlock.BulletList -> Surface("))
        assertTrue(reader.contains("fontSize = 16.5.sp"))
        assertTrue(reader.contains("lineHeight = 27.sp"))
        assertTrue(reader.contains("BOOK_TOP_BAR_HEIGHT = 34.dp"))
        assertTrue(reader.contains("BOOK_LESSON_STRIP_HEIGHT = 24.dp"))
        assertTrue(reader.contains("BOOK_PAGE_HORIZONTAL_PADDING = 16.dp"))
        assertTrue(reader.contains("BOOK_PAGE_VERTICAL_PADDING = 8.dp"))
        assertFalse(reader.contains(".padding(horizontal = 30.dp, vertical = 22.dp)"))
    }

    @Test
    fun currentReaderShowsAddedDepthTimeAndKeepsExactResume() {
        val reader = source("com/futuretech/poweruser/ui/V4PagedBookScreen.kt")
        val flow = source("com/futuretech/poweruser/textbook/TextbookLearningFlow.kt")
        val progress = source("com/futuretech/poweruser/textbook/TextbookProgressStore.kt")

        assertTrue(reader.contains("V4BookDepthLibrary.extraEstimatedMinutes"))
        assertTrue(flow.contains("V4BookDepthLibrary.blocksFor"))
        assertTrue(flow.contains("insertDepthBeforeSelfCheck"))
        assertTrue(reader.contains("selectedPageIndex(concept.id)"))
        assertTrue(reader.contains("saveSelectedPageIndex(concept.id, it)"))
        assertTrue(progress.contains("reader_page_"))
    }
}
