package com.futuretech.poweruser.textbook

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V3ReaderResilienceContractTest {
    private fun sourceFile(path: String): File {
        val candidates = listOf(File(path), File("../$path"))
        return candidates.firstOrNull { it.isFile }
            ?: error("Cannot locate source file: $path; cwd=${File(".").absolutePath}")
    }

    @Test
    fun longLessonReaderPersistsAndRestoresExactBookPage() {
        val screen = sourceFile("app/src/main/java/com/futuretech/poweruser/ui/V1TextbookScreen.kt")
            .readText(Charsets.UTF_8)
        val store = sourceFile("app/src/main/java/com/futuretech/poweruser/textbook/TextbookProgressStore.kt")
            .readText(Charsets.UTF_8)

        assertTrue(screen.contains("initialPageIndex = store.selectedPageIndex(currentConcept.id)"))
        assertTrue(screen.contains("onPageChanged = { store.saveSelectedPageIndex(currentConcept.id, it) }"))
        assertTrue(screen.contains("textbook_page_indicator"))
        assertTrue(screen.contains("TextbookPageComposer.paginate"))
        assertFalse("book reader must not regress to a long vertical list", screen.contains("rememberLazyListState"))
        assertFalse("book reader must not restore an approximate scroll offset", screen.contains("snapshotFlow"))
        assertTrue(store.contains("v3_deep_beginner_progress"))
        assertTrue(store.contains("reader_page_"))
        assertTrue("page position must survive an immediate app close", store.contains(".commit()"))
    }

    @Test
    fun codeBlocksDoNotStealHorizontalPageSwipe() {
        val screen = sourceFile("app/src/main/java/com/futuretech/poweruser/ui/V1TextbookScreen.kt")
            .readText(Charsets.UTF_8)

        assertFalse("code block horizontal scrolling must stay removed", screen.contains("horizontalScroll(rememberScrollState())"))
        assertTrue("beginner code must wrap on narrow phones", screen.contains("softWrap = true"))
        assertTrue("page swipe remains available", screen.contains("detectHorizontalDragGestures"))
        assertTrue("swipe requires a deliberate gesture", screen.contains("72.dp.toPx()"))
        assertTrue("right-edge tap advances the page", screen.contains("textbook_right_tap_zone"))
        assertTrue("left-edge tap returns to the previous page", screen.contains("textbook_left_tap_zone"))
    }

    @Test
    fun deepHeadingsRemainVisuallyDistinctFromBodyText() {
        val screen = sourceFile("app/src/main/java/com/futuretech/poweruser/ui/V1TextbookScreen.kt")
            .readText(Charsets.UTF_8)

        assertTrue(screen.contains("3 -> 18.sp"))
        assertTrue(screen.contains("4 -> 16.sp"))
        assertTrue(screen.contains("if (block.level <= 3) FontWeight.Bold else FontWeight.SemiBold"))
        assertTrue("body text remains smaller than level-3 heading", screen.contains("fontSize = 16.sp"))
        assertTrue("body line height remains generous for long-form reading", screen.contains("lineHeight = 26.sp"))
    }
}
