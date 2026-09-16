package com.futuretech.poweruser.textbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextbookPaginatorTest {
    @Test
    fun longMixedLessonBecomesFixedPagesWithoutLosingAuthoredContent() {
        val paragraph = (1..180).joinToString(" ") { "단어$it" }
        val bullets = (1..16).map { "항목 $it " + "설명 ".repeat(6) }
        val codeLines = (1..32).map { "val value$it = values.map { item -> item + $it }" }
        val tableRows = (1..12).map { listOf("행$it", "설명 ".repeat(5)) }
        val blocks = listOf(
            TextbookBlock.Heading(2, "자료구조를 손으로 추적하기"),
            TextbookBlock.Heading(3, "왜 직접 그려봐야 하나"),
            TextbookBlock.Paragraph(paragraph),
            TextbookBlock.BulletList(bullets, ordered = false),
            TextbookBlock.Code("kotlin", codeLines.joinToString("\n")),
            TextbookBlock.Table(listOf("이름", "설명"), tableRows)
        )
        val metrics = TextbookPageMetrics(widthDp = 360, heightDp = 500)
        val pages = TextbookPaginator.paginate(blocks, metrics)

        assertTrue(pages.size >= 8)
        assertTrue(pages.all { it.estimatedLines <= metrics.lineBudget })
        assertTrue(pages.dropLast(1).none { it.blocks.lastOrNull() is TextbookBlock.Heading })

        val rebuiltParagraph = pages.flatMap { it.blocks }
            .filterIsInstance<TextbookBlock.Paragraph>()
            .joinToString(" ") { it.text }
        assertEquals(paragraph, rebuiltParagraph)

        val rebuiltBullets = pages.flatMap { it.blocks }
            .filterIsInstance<TextbookBlock.BulletList>()
            .flatMap { it.items }
        assertEquals(bullets, rebuiltBullets)

        val rebuiltCode = pages.flatMap { it.blocks }
            .filterIsInstance<TextbookBlock.Code>()
            .flatMap { it.text.lines() }
        assertEquals(codeLines, rebuiltCode)

        val rebuiltRows = pages.flatMap { it.blocks }
            .filterIsInstance<TextbookBlock.Table>()
            .flatMap { it.rows }
        assertEquals(tableRows, rebuiltRows)
    }

    @Test
    fun paginationAdaptsToPhoneAndTabletInsteadOfUsingOneHardCodedPageSize() {
        val blocks = listOf(
            TextbookBlock.Heading(3, "그래프를 직접 따라가기"),
            TextbookBlock.Paragraph((1..140).joinToString(" ") { "설명$it" })
        )
        val phone = TextbookPaginator.paginate(blocks, TextbookPageMetrics(320, 420))
        val tablet = TextbookPaginator.paginate(blocks, TextbookPageMetrics(760, 820))

        assertTrue(phone.size > tablet.size)
        assertTrue(phone.size > 1)
        assertTrue(tablet.isNotEmpty())
    }
}
