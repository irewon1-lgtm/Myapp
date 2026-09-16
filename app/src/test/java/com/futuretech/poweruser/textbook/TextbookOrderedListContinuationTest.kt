package com.futuretech.poweruser.textbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextbookOrderedListContinuationTest {
    @Test
    fun parserPreservesAuthoredOrderedListStartAndSplitsNumberingJumps() {
        val parsed = TextbookMarkdownParser.parse(
            """
            7. seven
            8. eight
            9. nine
            12. twelve
            13. thirteen
            """.trimIndent()
        )

        assertEquals(2, parsed.size)
        val first = parsed[0] as TextbookBlock.BulletList
        val second = parsed[1] as TextbookBlock.BulletList
        assertTrue(first.ordered)
        assertTrue(second.ordered)
        assertEquals(7, first.startNumber)
        assertEquals(listOf("seven", "eight", "nine"), first.items)
        assertEquals(12, second.startNumber)
        assertEquals(listOf("twelve", "thirteen"), second.items)
    }

    @Test
    fun legacyPageSplitDoesNotResetOrderedListToOne() {
        val original = TextbookBlock.BulletList(
            items = (7..46).map { n ->
                "단계 $n 에서는 페이지가 나뉘어도 번호와 원문 순서를 보존해야 한다. 이 문장은 강제로 여러 페이지를 만든다."
            },
            ordered = true,
            startNumber = 7
        )

        val pages = TextbookPageComposer.paginate(
            blocks = listOf(original),
            layout = TextbookPageLayout(charsPerLine = 24, maxLines = 12)
        )
        val fragments = pages.flatMap { it.blocks }.filterIsInstance<TextbookBlock.BulletList>()
        assertTrue("test must actually force a split", fragments.size > 1)

        var expected = 7
        val rebuilt = mutableListOf<String>()
        fragments.forEach { fragment ->
            assertEquals(expected, fragment.startNumber)
            rebuilt += fragment.items
            expected += fragment.items.size
        }
        assertEquals(47, expected)
        assertEquals(original.items, rebuilt)
    }
}
