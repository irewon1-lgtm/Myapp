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
    fun problemTypeVocabularyRemainsAvailableForFullPracticeSurfaces() {
        assertEquals(
            listOf(
                "개념 선택",
                "실행결과 예상",
                "순서배치",
                "빈칸코드",
                "한 줄 수정",
                "직접 설명/작성",
                "디버깅",
                "AI가 만든 답 검증"
            ),
            LearningProblemType.entries.map { it.displayName }
        )
        assertEquals(8, LearningProblemType.entries.size)
    }

    @Test
    fun syntheticLessonStaysOneLearningPageAndPreservesAllContent() {
        val track = V1TextbookCatalog.chapters.first()
        val practice = requireNotNull(CurriculumDataRepository.lessonById(track.practiceLessonId))
        val blocks = buildList {
            add(TextbookBlock.Heading(3, "개념 A"))
            repeat(7) { add(TextbookBlock.Paragraph("A-$it")) }
            add(TextbookBlock.Heading(4, "개념 B"))
            repeat(3) { add(TextbookBlock.Paragraph("B-$it")) }
        }
        val section = TextbookSection(
            id = "${track.id}-S01",
            index = 0,
            title = "테스트 LESSON",
            estimatedMinutes = 5,
            blocks = blocks,
            weightedLength = blocks.sumOf(TextbookSectioner::weightOf)
        )

        val concepts = TextbookLearningFlow.buildConcepts(track.id, section, practice)

        assertEquals(1, concepts.size)
        assertEquals(blocks, concepts.single().blocks)
        assertEquals(section.title, concepts.single().title)
        assertEquals("${section.id}-C01", concepts.single().id)
        assertEquals(LearningProblemType.DIRECT_WRITE, concepts.single().problem.type)
        assertTrue(concepts.single().problem.prompt.contains(section.title))
        assertTrue(concepts.single().problem.prompt.contains("점수를 깎지"))
    }

    @Test
    fun inlineRecallDoesNotCycleUnrelatedLegacyQuestions() {
        val track = V1TextbookCatalog.chapters.first()
        val practice = requireNotNull(CurriculumDataRepository.lessonById(track.practiceLessonId))

        val pages = (0 until 8).map { index ->
            val blocks = listOf(
                TextbookBlock.Heading(3, "LESSON ${index + 1}"),
                TextbookBlock.Paragraph("현재 LESSON의 실제 학습 내용 ${index + 1}")
            )
            val section = TextbookSection(
                id = "${track.id}-S${(index + 1).toString().padStart(2, '0')}",
                index = index,
                title = "LESSON ${index + 1}",
                estimatedMinutes = 5,
                blocks = blocks,
                weightedLength = blocks.sumOf(TextbookSectioner::weightOf)
            )
            TextbookLearningFlow.buildConcepts(track.id, section, practice).single()
        }

        assertTrue(pages.all { it.problem.type == LearningProblemType.DIRECT_WRITE })
        assertTrue(pages.all { page -> page.problem.prompt.contains(page.title) })
        assertFalse(pages.any { it.problem.prompt == practice.aiHallucinationQuestion })
    }

    @Test
    fun allRealV2LessonsPreserveContentAndUseOneGroundedRecall() {
        V1TextbookCatalog.chapters.forEach { track ->
            val practice = requireNotNull(CurriculumDataRepository.lessonById(track.practiceLessonId))
            val markdown = assetFile(track.assetPath).readText(Charsets.UTF_8)
            val blocks = TextbookMarkdownParser.parse(markdown)
            val sections = TextbookSectioner.split(track.id, blocks)

            sections.forEach { section ->
                val concepts = TextbookLearningFlow.buildConcepts(track.id, section, practice)
                assertEquals("${section.id}: exactly one learning page", 1, concepts.size)
                val page = concepts.single()
                assertEquals("${section.id}: all source blocks preserved", section.blocks, page.blocks)
                assertTrue("${section.id}: page titled", page.title.isNotBlank())
                assertEquals(LearningProblemType.DIRECT_WRITE, page.problem.type)
                assertTrue("${section.id}: recall grounded in current title", page.problem.prompt.contains(section.title))
            }
        }
    }

    @Test
    fun answerNormalizationIsStableForLowStakesChecks() {
        assertEquals(
            TextbookLearningFlow.normalizeAnswer("  SHA-256   "),
            TextbookLearningFlow.normalizeAnswer("sha-256")
        )
    }
}
