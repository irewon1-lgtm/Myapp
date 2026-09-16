package com.futuretech.poweruser.textbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextbookPageComposerTest {
    @Test
    fun longAuthoredBlocksAreSplitIntoNonScrollingPages() {
        val paragraph = ("처음 배우는 사람도 흐름을 놓치지 않도록 한 단계씩 설명합니다. ").repeat(80).trim()
        val code = (1..42).joinToString("\n") { index -> "println(\"line $index\")" }
        val blocks = listOf(
            TextbookBlock.Heading(2, "자료구조를 처음부터 이해하기"),
            TextbookBlock.Paragraph(paragraph),
            TextbookBlock.BulletList(
                items = List(10) { index -> "항목 ${index + 1} " + "설명 ".repeat(15) },
                ordered = false
            ),
            TextbookBlock.Code("kotlin", code)
        )
        val layout = TextbookPageLayout(charsPerLine = 22, maxLines = 18)

        val pages = TextbookPageComposer.paginate(blocks, layout)

        assertTrue("A long lesson must become multiple e-book pages", pages.size > 4)
        assertTrue(
            "Normal authored blocks must stay inside the configured non-scrolling line budget",
            pages.all { it.estimatedLines <= layout.maxLines }
        )
    }

    @Test
    fun paragraphPaginationPreservesAllAuthoredText() {
        val original = ("페이지를 넘겨도 원문이 빠지면 안 됩니다. ").repeat(90).trim()
        val pages = TextbookPageComposer.paginate(
            blocks = listOf(TextbookBlock.Paragraph(original)),
            layout = TextbookPageLayout(charsPerLine = 20, maxLines = 18)
        )

        val restored = pages
            .flatMap { it.blocks }
            .filterIsInstance<TextbookBlock.Paragraph>()
            .joinToString(" ") { it.text }

        assertEquals(original.replace(Regex("\\s+"), " "), restored.replace(Regex("\\s+"), " "))
    }

    @Test
    fun paginationNeverRewritesCodeOrCreatesFakeListItems() {
        val code = (1..35).joinToString("\n") { index ->
            "val original_${index} = ${index} // this exact source line must survive pagination"
        }
        val bullets = listOf(
            "첫 번째 긴 항목 " + "설명 ".repeat(40),
            "두 번째 항목",
            "세 번째 긴 항목 " + "원문 ".repeat(35),
            "네 번째 항목"
        )
        val pages = TextbookPageComposer.paginate(
            blocks = listOf(
                TextbookBlock.Code("kotlin", code),
                TextbookBlock.BulletList(bullets, ordered = false)
            ),
            layout = TextbookPageLayout(charsPerLine = 22, maxLines = 18)
        )

        val restoredCode = pages
            .flatMap { it.blocks }
            .filterIsInstance<TextbookBlock.Code>()
            .joinToString("\n") { it.text }
        val restoredBullets = pages
            .flatMap { it.blocks }
            .filterIsInstance<TextbookBlock.BulletList>()
            .flatMap { it.items }

        assertEquals("Pagination must not change executable source text", code, restoredCode)
        assertEquals("Pagination must not turn or reorder authored bullets", bullets, restoredBullets)
    }

    @Test
    fun pagePackingUsesRemainingSpaceInsteadOfLeavingLargeBlankBottoms() {
        val blocks = listOf(
            TextbookBlock.Heading(3, "책 페이지 밀도 검증"),
            TextbookBlock.Paragraph(
                ("한 페이지 안에서는 앞 문단이 끝난 뒤 남은 공간을 다음 설명이 이어서 사용해야 합니다. ").repeat(120).trim()
            ),
            TextbookBlock.Heading(4, "다음 설명"),
            TextbookBlock.Paragraph(
                ("블록 경계가 있다는 이유만으로 페이지 아래 절반을 비워 두지 않습니다. ").repeat(50).trim()
            )
        )
        val layout = TextbookPageLayout(charsPerLine = 30, maxLines = 24)

        val pages = TextbookPageComposer.paginate(blocks, layout)
        val completedPages = pages.dropLast(1)

        assertTrue("density test needs several pages", completedPages.size >= 3)
        assertTrue(
            "Completed content pages should normally use at least 75% of the line budget: ${completedPages.map { it.estimatedLines }}",
            completedPages.all { it.estimatedLines >= 18 }
        )
    }

    @Test
    fun phoneAndTabletLayoutsBothProduceBoundedPages() {
        val blocks = listOf(
            TextbookBlock.Paragraph(("한 문단이 길어져도 화면 아래로 무한 스크롤하지 않습니다. ").repeat(55)),
            TextbookBlock.Code("python", (1..30).joinToString("\n") { "value_$it = $it" })
        )
        val phone = TextbookPageComposer.paginate(blocks, TextbookPageLayout(24, 20))
        val tablet = TextbookPageComposer.paginate(blocks, TextbookPageLayout(46, 32))

        assertTrue(phone.all { it.estimatedLines <= 20 })
        assertTrue(tablet.all { it.estimatedLines <= 32 })
        assertTrue("A wider/taller tablet should not need more pages than a phone", tablet.size <= phone.size)
    }
}
