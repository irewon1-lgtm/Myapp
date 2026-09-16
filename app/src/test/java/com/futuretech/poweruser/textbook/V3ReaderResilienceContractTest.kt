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
    fun activeReaderUsesFixedPagesAndPersistsExactPageInsteadOfVerticalScroll() {
        val adaptive = sourceFile("app/src/main/java/com/futuretech/poweruser/ui/AdaptiveTextbookScreen.kt")
            .readText(Charsets.UTF_8)
        val screen = sourceFile("app/src/main/java/com/futuretech/poweruser/ui/EbookTextbookScreen.kt")
            .readText(Charsets.UTF_8)
        val store = sourceFile("app/src/main/java/com/futuretech/poweruser/textbook/TextbookProgressStore.kt")
            .readText(Charsets.UTF_8)

        assertTrue(adaptive.contains("EbookTextbookScreen("))
        assertFalse(adaptive.contains("V1TextbookScreen("))
        assertTrue(screen.contains("TextbookPaginator.paginate"))
        assertTrue(screen.contains("initialPageIndex = store.pageIndex(currentConcept.id)"))
        assertTrue(screen.contains("store.savePageIndex(currentConcept.id, index)"))
        assertTrue(store.contains("page_index_"))
        assertFalse("active ebook reader must not use a vertical LazyColumn", screen.contains("LazyColumn"))
        assertFalse("active ebook reader must not restore pixel scroll offsets", screen.contains("rememberLazyListState"))
        assertFalse(screen.contains("snapshotFlow"))
    }

    @Test
    fun pageTurnsSupportSwipeAndBothScreenEdges() {
        val screen = sourceFile("app/src/main/java/com/futuretech/poweruser/ui/EbookTextbookScreen.kt")
            .readText(Charsets.UTF_8)

        assertTrue(screen.contains("detectHorizontalDragGestures"))
        assertTrue(screen.contains("56.dp.toPx()"))
        assertTrue(screen.contains("ebook_left_edge"))
        assertTrue(screen.contains("ebook_right_edge"))
        assertTrue(screen.contains("ebook_page_counter"))
        assertTrue("beginner code must wrap inside a book page", screen.contains("softWrap = true"))
    }

    @Test
    fun readerHasBookStructureInsteadOfOneContinuousLessonSurface() {
        val screen = sourceFile("app/src/main/java/com/futuretech/poweruser/ui/EbookTextbookScreen.kt")
            .readText(Charsets.UTF_8)

        assertTrue(screen.contains("EbookPage.TrackCover"))
        assertTrue(screen.contains("EbookPage.LessonOpening"))
        assertTrue(screen.contains("EbookPage.Content"))
        assertTrue(screen.contains("EbookPage.Recall"))
        assertTrue(screen.contains("EbookPage.TrackEnd"))
    }
}
