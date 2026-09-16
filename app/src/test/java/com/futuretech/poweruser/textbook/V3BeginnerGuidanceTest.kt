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
        val guideIds = V3BeginnerGuidanceResolver.guides.map { it.sectionId }

        assertTrue("V3 learner lesson count must stay inside the quality range", sectionIds.size in 20..50)
        assertEquals("Duplicate beginner guide ids", guideIds.size, guideIds.toSet().size)
        assertEquals("Guide coverage must exactly match learner lessons", sectionIds.toSet(), guideIds.toSet())
        assertTrue(V3BeginnerGuidanceResolver.guides.all { it.mustUnderstand.size >= 3 })
        assertTrue(V3BeginnerGuidanceResolver.guides.all { it.answerPoints.size >= 4 })
    }

    @Test
    fun decorationNeverDeletesOrReordersAuthoredBlocks() {
        allSections().forEach { section ->
            val decorated = V3BeginnerGuidanceResolver.decorateBlocks(section.id, section.blocks)
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
    fun renderedGuidanceReadingTimeCannotHideAnOverlongLesson() {
        allSections().forEach { section ->
            val decorated = V3BeginnerGuidanceResolver.decorateBlocks(section.id, section.blocks)
            val renderedWeight = decorated.sumOf(TextbookSectioner::weightOf)
            val renderedMinutes = TextbookSectioner.estimatedMinutesFor(renderedWeight)

            assertTrue(
                "${section.id}: guidance must not make displayed content shorter than authored content",
                renderedMinutes >= section.estimatedMinutes
            )
            assertTrue(
                "${section.id}: rendered learner lesson is over 60 minutes after beginner guidance: $renderedMinutes",
                renderedMinutes in TextbookSectioner.V3_MIN_LESSON_MINUTES..TextbookSectioner.V3_MAX_LESSON_MINUTES
            )
        }
    }

    @Test
    fun previouslyAbruptAdvancedTermsNowHavePlainLanguagePreviews() {
        val requiredByTrack = mapOf(
            "V1-C03-" to listOf("amortized cost", "BST", "heap", "trie"),
            "V1-C05-" to listOf("value is User", "Record<string, unknown>", "race condition"),
            "V1-C06-" to listOf("MIME type", "boundary", "preflight"),
            "V1-C07-" to listOf("IDOR/BOLA", "DLQ", "outbox", "observability"),
            "V1-C08-" to listOf("selectivity", "optimizer", "WAL"),
            "V1-C11-" to listOf("circuit breaker", "bulkhead", "SLI/SLO")
        )

        requiredByTrack.forEach { (prefix, terms) ->
            val hints = V3BeginnerGuidanceResolver.guides
                .filter { it.sectionId.startsWith(prefix) }
                .flatMap { it.termHints }
            terms.forEach { term ->
                val hint = hints.firstOrNull { it.term == term }
                assertTrue("$prefix: missing plain-language preview for $term", hint != null)
                assertTrue("$prefix: preview for $term is too thin", requireNotNull(hint).plainMeaning.length >= 10)
            }
        }
    }

    @Test
    fun deepDiveLessonsTellBeginnerWhatCanWait() {
        val deep = V3BeginnerGuidanceResolver.guides.filter { it.focus == BeginnerLessonFocus.CORE_WITH_DEEP_DIVE }
        assertTrue("Expected several intentionally dense lessons", deep.size >= 8)
        assertTrue("Every deep-dive lesson must explicitly mark deferrable detail", deep.all { it.canDefer.isNotEmpty() })
    }
}
