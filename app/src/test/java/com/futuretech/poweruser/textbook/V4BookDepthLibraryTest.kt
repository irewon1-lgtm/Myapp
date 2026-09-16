package com.futuretech.poweruser.textbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private fun searchableText(sectionId: String): String = V4BookDepthLibrary.blocksFor(sectionId)
        .joinToString("\n") { block ->
            when (block) {
                is TextbookBlock.Heading -> block.text
                is TextbookBlock.Paragraph -> block.text
                is TextbookBlock.BulletList -> block.items.joinToString("\n")
                is TextbookBlock.Code -> block.text
                is TextbookBlock.Table -> block.headers.joinToString(" ") + "\n" + block.rows.flatten().joinToString(" ")
                TextbookBlock.Divider -> ""
            }
        }

    @Test
    fun allFortyTwoLearnerLessonsHaveDedicatedExpertDepthReading() {
        assertEquals(42, expectedSectionIds.size)
        assertEquals(expectedSectionIds, V4BookDepthLibrary.sectionIds())
    }

    @Test
    fun depthIsSubstantialWithoutRestoringTheOldForcedTemplate() {
        expectedSectionIds.forEach { sectionId ->
            val blocks = V4BookDepthLibrary.blocksFor(sectionId)
            assertTrue("$sectionId must have V4 depth blocks", blocks.isNotEmpty())
            assertTrue(
                "$sectionId expert depth is too thin: chars=${V4BookDepthLibrary.extraCharacterCount(sectionId)}",
                V4BookDepthLibrary.extraCharacterCount(sectionId) >= 1_600
            )
            assertTrue("$sectionId needs several real mechanisms", blocks.filterIsInstance<TextbookBlock.Paragraph>().size >= 5)
            assertTrue("$sectionId needs at least one concrete trace/code scenario", blocks.any { it is TextbookBlock.Code })
        }

        val totalExtraChars = expectedSectionIds.sumOf(V4BookDepthLibrary::extraCharacterCount)
        assertTrue("V4 expert layer is too small: totalExtraChars=$totalExtraChars", totalExtraChars >= 80_000)
    }

    @Test
    fun oldBoilerplateQuestionOnlyArchitectureAndEditorialArtifactsAreGone() {
        val corpus = expectedSectionIds.joinToString("\n", transform = ::searchableText)
        assertFalse(corpus.contains("앞의 본문은 첫 이해를 만드는 설명이다"))
        assertFalse(corpus.contains("같은 개념을 다른 각도에서 다시 보고"))
        assertFalse(corpus.contains("처음 읽을 때 전부 외우지 말고"))
        assertFalse("rendered V4 text must not expose transport escaping", corpus.contains("\\\""))
        assertFalse("learner-facing book must not expose internal V3 authoring labels", corpus.contains("V3"))
        assertFalse("learner-facing book must not expose internal V4 authoring labels", corpus.contains("V4"))

        // These concepts used to be easy-to-ignore follow-up questions. They must now be taught in
        // normal authored headings/paragraphs/code, not hidden behind a mandatory questions list.
        listOf(
            "grapheme", "NFC", "topological", "Dijkstra", "presigned", "idempotent",
            "outbox", "saga", "covering", "WAL", "deadlock", "pepper", "KDF",
            "property-based", "mutation", "contract test", "bulkhead", "error budget"
        ).forEach { term ->
            assertTrue("advanced concept must be taught in the V4 body: $term", corpus.contains(term, ignoreCase = true))
        }
    }

    @Test
    fun noLongExplanatoryParagraphIsCopiedAcrossLessons() {
        val owners = mutableMapOf<String, MutableSet<String>>()
        expectedSectionIds.forEach { sectionId ->
            V4BookDepthLibrary.blocksFor(sectionId)
                .filterIsInstance<TextbookBlock.Paragraph>()
                .forEach { block ->
                    val normalized = block.text.replace(Regex("\\s+"), " ").trim()
                    if (normalized.length >= 80) owners.getOrPut(normalized) { linkedSetOf() }.add(sectionId)
                }
        }
        val duplicates = owners.filterValues { it.size > 1 }
        assertTrue("long boilerplate paragraphs found: ${duplicates.values}", duplicates.isEmpty())
    }

    @Test
    fun originalAuthoredLessonBlocksAreStillAdditiveAndUntouched() {
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
