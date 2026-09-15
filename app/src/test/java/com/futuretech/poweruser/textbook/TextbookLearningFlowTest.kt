package com.futuretech.poweruser.textbook

import com.futuretech.poweruser.data.CurriculumDataRepository
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextbookLearningFlowTest {
    private fun assetFile(assetPath: String): File {
        val candidates = listOf(
            File("src/main/assets/$assetPath"),
            File("app/src/main/assets/$assetPath")
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("Cannot locate real textbook asset: $assetPath; cwd=${File(".").absolutePath}")
    }

    @Test
    fun problemTypes_areExactlyTheRequiredEight() {
        assertEquals(
            listOf(
                "개념 선택",
                "실행결과 예상",
                "순서배치",
                "빈칸코드",
                "한 줄 수정",
                "직접작성",
                "디버깅",
                "AI가 만든 답 검증"
            ),
            LearningProblemType.entries.map { it.displayName }
        )
        assertEquals(8, LearningProblemType.entries.size)
    }

    @Test
    fun syntheticSection_splitsAtConceptHeadingsAndNeverExceedsSixBlocks() {
        val chapter = V1TextbookCatalog.chapters.first()
        val lesson = requireNotNull(CurriculumDataRepository.lessonById(chapter.practiceLessonId))
        val blocks = buildList {
            add(TextbookBlock.Heading(3, "개념 A"))
            repeat(7) { add(TextbookBlock.Paragraph("A-$it")) }
            add(TextbookBlock.Heading(4, "개념 B"))
            repeat(3) { add(TextbookBlock.Paragraph("B-$it")) }
        }
        val section = TextbookSection(
            id = "${chapter.id}-S01",
            index = 0,
            title = "테스트 Section",
            estimatedMinutes = 5,
            blocks = blocks,
            weightedLength = blocks.sumOf(TextbookSectioner::weightOf)
        )

        val concepts = TextbookLearningFlow.buildConcepts(chapter.id, section, lesson)

        assertTrue(concepts.size >= 3)
        assertEquals(blocks, concepts.flatMap { it.blocks })
        assertTrue(concepts.all { it.blocks.isNotEmpty() })
        assertTrue(concepts.all { it.blocks.size <= TextbookLearningFlow.MAX_BLOCKS_PER_CONCEPT })
        assertEquals(concepts.size, concepts.map { it.id }.toSet().size)
    }

    @Test
    fun eightSyntheticConcepts_cycleAcrossAllEightProblemTypes() {
        val chapter = V1TextbookCatalog.chapters.first()
        val lesson = requireNotNull(CurriculumDataRepository.lessonById(chapter.practiceLessonId))
        val blocks = buildList {
            repeat(8) { index ->
                add(TextbookBlock.Heading(3, "개념 ${index + 1}"))
                add(TextbookBlock.Paragraph("내용 ${index + 1}"))
            }
        }
        val section = TextbookSection(
            id = "${chapter.id}-S01",
            index = 0,
            title = "8종 문제 테스트",
            estimatedMinutes = 5,
            blocks = blocks,
            weightedLength = blocks.sumOf(TextbookSectioner::weightOf)
        )

        val concepts = TextbookLearningFlow.buildConcepts(chapter.id, section, lesson)

        assertEquals(8, concepts.size)
        assertEquals(LearningProblemType.entries.toSet(), concepts.map { it.problem.type }.toSet())
        assertFalse(concepts.any { it.problem.id.isBlank() || it.problem.prompt.isBlank() })
    }

    @Test
    fun allRealV1Sections_preserveContentAndGetConceptProblems() {
        val usedTypes = mutableSetOf<LearningProblemType>()

        V1TextbookCatalog.chapters.forEach { chapter ->
            val lesson = requireNotNull(CurriculumDataRepository.lessonById(chapter.practiceLessonId))
            val markdown = assetFile(chapter.assetPath).readText(Charsets.UTF_8)
            val blocks = TextbookMarkdownParser.parse(markdown)
            val sections = TextbookSectioner.split(chapter.id, blocks)

            sections.forEach { section ->
                val concepts = TextbookLearningFlow.buildConcepts(chapter.id, section, lesson)
                assertTrue("${section.id}: at least one concept", concepts.isNotEmpty())
                assertEquals("${section.id}: source blocks preserved", section.blocks, concepts.flatMap { it.blocks })
                assertTrue(
                    "${section.id}: one concept page stays bounded",
                    concepts.all { it.blocks.size in 1..TextbookLearningFlow.MAX_BLOCKS_PER_CONCEPT }
                )
                assertTrue("${section.id}: every concept titled", concepts.all { it.title.isNotBlank() })
                assertTrue("${section.id}: every concept has a problem", concepts.all { it.problem.prompt.isNotBlank() })
                usedTypes += concepts.map { it.problem.type }
            }
        }

        assertEquals("real textbook flow exposes all eight problem types", LearningProblemType.entries.toSet(), usedTypes)
    }

    @Test
    fun answerNormalization_isStableForLowStakesChecks() {
        assertEquals(
            TextbookLearningFlow.normalizeAnswer("  SHA-256   "),
            TextbookLearningFlow.normalizeAnswer("sha-256")
        )
    }
}
