package com.futuretech.poweruser.textbook

import com.futuretech.poweruser.data.CurriculumDataRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextbookLearningFlowTest {
    private val chapter = V1TextbookCatalog.chapters.first()
    private val lesson = requireNotNull(CurriculumDataRepository.lessonById(chapter.practiceLessonId))

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
    fun sections_splitAtConceptHeadingsAndNeverReturnEmptyContent() {
        val blocks = listOf(
            TextbookBlock.Heading(2, "chapter"),
            TextbookBlock.Heading(3, "현실 문제"),
            TextbookBlock.Paragraph("a"),
            TextbookBlock.Paragraph("b"),
            TextbookBlock.Heading(4, "세부 개념"),
            TextbookBlock.Paragraph("c"),
            TextbookBlock.Heading(3, "예제"),
            TextbookBlock.Paragraph("d")
        )

        val sections = TextbookLearningFlow.buildSections(chapter, blocks, lesson)

        assertEquals(listOf("현실 문제", "세부 개념", "예제"), sections.map { it.title })
        assertTrue(sections.all { it.blocks.isNotEmpty() })
        assertTrue(sections.all { it.blocks.size <= TextbookLearningFlow.MAX_CONTENT_BLOCKS_PER_SECTION })
    }

    @Test
    fun longConcept_isChunkedInsteadOfBecomingOneLongReaderScreen() {
        val blocks = buildList {
            add(TextbookBlock.Heading(2, "chapter"))
            add(TextbookBlock.Heading(3, "긴 개념"))
            repeat(TextbookLearningFlow.MAX_CONTENT_BLOCKS_PER_SECTION + 3) {
                add(TextbookBlock.Paragraph("paragraph-$it"))
            }
        }

        val sections = TextbookLearningFlow.buildSections(chapter, blocks, lesson)

        assertEquals(2, sections.size)
        assertTrue(sections.first().blocks.size <= TextbookLearningFlow.MAX_CONTENT_BLOCKS_PER_SECTION)
        assertTrue(sections.last().blocks.size <= TextbookLearningFlow.MAX_CONTENT_BLOCKS_PER_SECTION)
        assertTrue(sections.last().title.contains("계속"))
    }

    @Test
    fun problemFactory_cyclesAcrossAllEightTypes() {
        val blocks = buildList {
            add(TextbookBlock.Heading(2, "chapter"))
            repeat(8) { index ->
                add(TextbookBlock.Heading(3, "개념 ${index + 1}"))
                add(TextbookBlock.Paragraph("내용 ${index + 1}"))
            }
        }

        val sections = TextbookLearningFlow.buildSections(chapter, blocks, lesson)

        assertEquals(8, sections.size)
        assertEquals(8, sections.map { it.problem.type }.toSet().size)
        assertEquals(LearningProblemType.entries.toSet(), sections.map { it.problem.type }.toSet())
        assertFalse(sections.any { it.problem.id.isBlank() || it.problem.prompt.isBlank() })
    }

    @Test
    fun normalization_isStableForLowStakesInlineChecks() {
        assertEquals(
            TextbookLearningFlow.normalizeAnswer("  SHA-256   "),
            TextbookLearningFlow.normalizeAnswer("sha-256")
        )
    }
}
