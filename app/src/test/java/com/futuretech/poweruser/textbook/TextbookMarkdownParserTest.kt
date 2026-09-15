package com.futuretech.poweruser.textbook

import org.junit.Assert.*
import org.junit.Test

class TextbookMarkdownParserTest {
    @Test fun parsesHeadingParagraphListCodeTableAndDivider() {
        val markdown = """
            ## 제목
            설명 문단입니다.

            - 하나
            - 둘

            1. 첫째
            2. 둘째

            ```python
            print('ok')
            ```

            | 항목 | 값 |
            | --- | --- |
            | A | B |

            ---
        """.trimIndent()
        val blocks = TextbookMarkdownParser.parse(markdown)
        assertTrue(blocks.any { it is TextbookBlock.Heading })
        assertTrue(blocks.any { it is TextbookBlock.Paragraph })
        assertEquals(2, blocks.count { it is TextbookBlock.BulletList })
        assertTrue(blocks.any { it is TextbookBlock.Code })
        assertTrue(blocks.any { it is TextbookBlock.Table })
        assertTrue(blocks.any { it is TextbookBlock.Divider })
    }

    @Test fun stripsSimpleInlineMarkdownForReadableNativeText() {
        val blocks = TextbookMarkdownParser.parse("**중요** 문장과 `코드` 용어")
        val paragraph = blocks.single() as TextbookBlock.Paragraph
        assertEquals("중요 문장과 코드 용어", paragraph.text)
    }

    @Test fun incompleteCodeFenceStillReturnsCodeBlockWithoutCrash() {
        val blocks = TextbookMarkdownParser.parse("```text\nhello")
        val code = blocks.single() as TextbookBlock.Code
        assertEquals("hello", code.text)
    }
}
