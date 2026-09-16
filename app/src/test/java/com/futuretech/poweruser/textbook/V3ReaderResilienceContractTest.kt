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
    fun longLessonReaderPersistsAndRestoresVerticalPosition() {
        val screen = sourceFile("app/src/main/java/com/futuretech/poweruser/ui/V1TextbookScreen.kt")
            .readText(Charsets.UTF_8)
        val store = sourceFile("app/src/main/java/com/futuretech/poweruser/textbook/TextbookProgressStore.kt")
            .readText(Charsets.UTF_8)

        assertTrue(screen.contains("initialFirstVisibleItemIndex = initialScrollIndex"))
        assertTrue(screen.contains("initialFirstVisibleItemScrollOffset = initialScrollOffset"))
        assertTrue(screen.contains("snapshotFlow"))
        assertTrue(screen.contains("store.saveScroll(currentConcept.id, index, offset)"))
        assertTrue(store.contains("v3_deep_beginner_progress"))
        assertTrue(store.contains("scroll_index_"))
        assertTrue(store.contains("scroll_offset_"))
    }

    @Test
    fun codeBlocksDoNotStealHorizontalLessonSwipe() {
        val screen = sourceFile("app/src/main/java/com/futuretech/poweruser/ui/V1TextbookScreen.kt")
            .readText(Charsets.UTF_8)

        assertFalse("code block horizontal scrolling must stay removed", screen.contains("horizontalScroll(rememberScrollState())"))
        assertTrue("beginner code must wrap on narrow phones", screen.contains("softWrap = true"))
        assertTrue("lesson swipe remains available", screen.contains("detectHorizontalDragGestures"))
        assertTrue("swipe requires a deliberate gesture", screen.contains("110.dp.toPx()"))
    }

    @Test
    fun deepHeadingsAreVisuallyDistinctFromBodyText() {
        val screen = sourceFile("app/src/main/java/com/futuretech/poweruser/ui/V1TextbookScreen.kt")
            .readText(Charsets.UTF_8)

        assertTrue(screen.contains("3 -> 19.sp"))
        assertTrue(screen.contains("4 -> 17.sp"))
        assertTrue(screen.contains("3 -> 16.dp"))
        assertTrue(screen.contains("4 -> 10.dp"))
        assertTrue(screen.contains("4 -> FontWeight.SemiBold"))
    }
}
