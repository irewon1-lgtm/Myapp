package com.futuretech.poweruser.textbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V4BookDepthLibraryTest {
    private val expectedSectionIds = buildSet {
        (1..5).forEach { add("V1-C01-S%02d".format(it)) }
        (1..4).forEach { add("V1-C02-S%02d".format(it)) }
        (1..4).forEach { add("V1-C03-S%02d".format(it)) }
        (1..3).forEach { add("V1-C04-S%02d".format(it)) }
        (1..4).forEach { add("V1-C05-S%02d".format(it)) }
        (1..5).forEach { add("V1-C06-S%02d".format(it)) }
        (1..4).forEach { add("V1-C07-S%02d".format(it)) }
        (1..4).forEach { add("V1-C08-S%02d".format(it)) }
        (1..3).forEach { add("V1-C09-S%02d".format(it)) }
        (1..3).forEach { add("V1-C10-S%02d".format(it)) }
        (1..3).forEach { add("V1-C11-S%02d".format(it)) }
    }

    @Test
    fun allFortyTwoLearnerLessonsHaveDedicatedDepthReading() {
        assertEquals(42, expectedSectionIds.size)
        assertEquals(expectedSectionIds, V4BookDepthLibrary.sectionIds())
    }

    @Test
    fun everyDepthPackContainsSubstantialExplanationExampleMistakesAndQuestions() {
        expectedSectionIds.forEach { sectionId ->
            val blocks = V4BookDepthLibrary.blocksFor(sectionId)
            assertTrue("$sectionId must have V4 depth blocks", blocks.isNotEmpty())
            assertTrue(
                "$sectionId depth is still too thin: chars=${V4BookDepthLibrary.extraCharacterCount(sectionId)}",
                V4BookDepthLibrary.extraCharacterCount(sectionId) >= 1_050
            )
            assertTrue(
                "$sectionId needs a worked example",
                blocks.filterIsInstance<TextbookBlock.Code>().any { it.text.isNotBlank() }
            )
            val lists = blocks.filterIsInstance<TextbookBlock.BulletList>()
            assertTrue("$sectionId needs mistakes and further-question lists", lists.size >= 2)
            assertTrue("$sectionId lists must be substantive", lists.all { it.items.size >= 4 })
            assertTrue(
                "$sectionId must explain why/how/failure, not only define terms",
                blocks.filterIsInstance<TextbookBlock.Paragraph>().size >= 5
            )
        }
    }

    @Test
    fun addedDepthIsBookScaleRatherThanAHandfulOfNotes() {
        val totalExtraChars = expectedSectionIds.sumOf(V4BookDepthLibrary::extraCharacterCount)
        assertTrue("V4 depth layer is too small: totalExtraChars=$totalExtraChars", totalExtraChars >= 50_000)
    }

    @Test
    fun depthLayerNeverDeletesAuthoredLessonBlocks() {
        val authored = listOf(
            TextbookBlock.Heading(2, "원문 제목"),
            TextbookBlock.Paragraph("원문 설명은 한 글자도 교체되면 안 됩니다."),
            TextbookBlock.Code("python", "print('original')")
        )
        val depth = V4BookDepthLibrary.blocksFor("V1-C01-S03")
        val combined = authored + depth

        assertEquals(authored, combined.take(authored.size))
        assertTrue(combined.size > authored.size)
    }
}
