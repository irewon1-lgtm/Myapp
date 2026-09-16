package com.futuretech.poweruser.textbook

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V3BeginnerGuidanceTest {
    private fun assetFile(assetPath: String): File {
        val candidates = listOf(
            File("src/main/assets/$assetPath"),
            File("app/src/main/assets/$assetPath")
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("Cannot locate V3 TRACK asset: $assetPath")
    }

    private fun allSections(): List<TextbookSection> = V1TextbookCatalog.chapters.flatMap { track ->
        val markdown = assetFile(track.assetPath).readText(Charsets.UTF_8)
        val blocks = TextbookMarkdownParser.parse(markdown)
        TextbookSectioner.split(track.id, blocks)
    }

    @Test
    fun everyCurrentLearnerLessonHasExactlyOneBeginnerGuide() {
        val sectionIds = allSections().map { it.id }
        val guideIds = V3BeginnerGuidance.guides.map { it.sectionId }

        assertEquals("Current V3 learner lesson count changed", 26, sectionIds.size)
        assertEquals("Duplicate beginner guide ids", guideIds.size, guideIds.toSet().size)
        assertEquals("Guide coverage must exactly match learner lessons", sectionIds.toSet(), guideIds.toSet())
        assertTrue(V3BeginnerGuidance.guides.all { it.mustUnderstand.size >= 3 })
        assertTrue(V3BeginnerGuidance.guides.all { it.answerPoints.size >= 4 })
    }

    @Test
    fun decorationNeverDeletesOrReordersAuthoredBlocks() {
        allSections().forEach { section ->
            val decorated = V3BeginnerGuidance.decorateBlocks(section.id, section.blocks)
            val start = decorated.indexOf(section.blocks.first())

            assertTrue("${section.id}: guide must be prepended before authored content", start > 0)
            assertEquals(
                "${section.id}: authored content changed while applying beginner layer",
                section.blocks,
                decorated.subList(start, start + section.blocks.size)
            )
            assertTrue(
                "${section.id}: answer/explanation summary missing",
                decorated.drop(start + section.blocks.size).any {
                    it is TextbookBlock.Heading && it.text.contains("정답·해설")
                }
            )
        }
    }

    @Test
    fun previouslyAbruptAdvancedTermsNowHavePlainLanguagePreviews() {
        val required = mapOf(
            "V1-C03-S01" to listOf("amortized cost", "BST", "heap", "trie"),
            "V1-C05-S04" to listOf("value is User", "Record<string, unknown>", "race condition"),
            "V1-C06-S02" to listOf("MIME type", "boundary", "preflight"),
            "V1-C07-S02" to listOf("IDOR/BOLA", "single flight", "TTL jitter", "DLQ", "outbox", "saga/compensation"),
            "V1-C08-S02" to listOf("selectivity", "optimizer", "WAL"),
            "V1-C11-S02" to listOf("circuit breaker", "bulkhead", "SLI/SLO")
        )

        required.forEach { (sectionId, terms) ->
            val hints = V3BeginnerGuidance.forSection(sectionId).termHints
            terms.forEach { term ->
                val hint = hints.firstOrNull { it.term == term }
                assertTrue("$sectionId: missing plain-language preview for $term", hint != null)
                assertTrue("$sectionId: preview for $term is too thin", requireNotNull(hint).plainMeaning.length >= 10)
            }
        }
    }

    @Test
    fun deepDiveLessonsTellBeginnerWhatCanWait() {
        val deep = V3BeginnerGuidance.guides.filter { it.focus == BeginnerLessonFocus.CORE_WITH_DEEP_DIVE }
        assertTrue("Expected several intentionally dense lessons", deep.size >= 8)
        assertTrue("Every deep-dive lesson must explicitly mark deferrable detail", deep.all { it.canDefer.isNotEmpty() })
    }
}
