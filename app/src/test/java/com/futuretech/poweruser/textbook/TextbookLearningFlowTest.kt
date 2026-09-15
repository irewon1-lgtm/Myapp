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
    fun syntheticSection_staysOneLearningPageAndPreservesAllContent() {
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

        assertEquals(1, concepts.size)
        assertEquals(blocks, concepts.single().blocks)
        assertEquals(section.title, concepts.single().title)
        assertEquals("${section.id}-C01", concepts.single().id)
        assertTrue(requireNotNull(concepts.single().problem).prompt.isNotBlank())
    }

    @Test
    fun eightSyntheticSections_cycleAcrossAllEightProblemTypes() {
        val chapter = V1TextbookCatalog.chapters.first()
        val lesson = requireNotNull(CurriculumDataRepository.lessonById(chapter.practiceLessonId))

        val pages = (0 until 8).map { sectionIndex ->
            val blocks = listOf(
                TextbookBlock.Heading(3, "개념 ${sectionIndex + 1}"),
                TextbookBlock.Paragraph("내용 ${sectionIndex + 1}")
            )
            val section = TextbookSection(
                id = "${chapter.id}-S${(sectionIndex + 1).toString().padStart(2, '0')}",
                index = sectionIndex,
                title = "Section ${sectionIndex + 1}",
                estimatedMinutes = 5,
                blocks = blocks,
                weightedLength = blocks.sumOf(TextbookSectioner::weightOf)
            )
            TextbookLearningFlow.buildConcepts(chapter.id, section, lesson).single()
        }

        assertEquals(
            LearningProblemType.entries.toSet(),
            pages.map { requireNotNull(it.problem).type }.toSet()
        )
        assertFalse(pages.any {
            val problem = it.problem
            problem == null || problem.id.isBlank() || problem.prompt.isBlank()
        })
    }

    @Test
    fun allRealV1MainTextSections_preserveFullLearningContentAndUseOneProblemPerSection() {
        val usedTypes = mutableSetOf<LearningProblemType>()

        V1TextbookCatalog.chapters.forEach { chapter ->
            val lesson = requireNotNull(CurriculumDataRepository.lessonById(chapter.practiceLessonId))
            val markdown = assetFile(chapter.assetPath).readText(Charsets.UTF_8)
            val blocks = TextbookMarkdownParser.parse(markdown)
            val sections = TextbookSectioner.split(chapter.id, blocks)

            sections.forEach { section ->
                val concepts = TextbookLearningFlow.buildConcepts(chapter.id, section, lesson)
                assertEquals("${section.id}: exactly one learning page", 1, concepts.size)
                val page = concepts.single()
                assertEquals("${section.id}: all source blocks preserved", section.blocks, page.blocks)
                assertTrue("${section.id}: page titled", page.title.isNotBlank())
                val problem = requireNotNull(page.problem)
                assertTrue("${section.id}: one end-of-section problem", problem.prompt.isNotBlank())
                usedTypes += problem.type
            }
        }

        assertEquals("real textbook flow exposes all eight problem types", LearningProblemType.entries.toSet(), usedTypes)
    }

    @Test
    fun combinedWorkbookSections_areMarkedAndDoNotGetDuplicateAutoProblems() {
        V1TextbookCatalog.chapters.forEach { chapter ->
            val lesson = requireNotNull(CurriculumDataRepository.lessonById(chapter.practiceLessonId))
            val chapterText = assetFile(chapter.assetPath).readText(Charsets.UTF_8)
            val workbookPath = "textbook/v1/workbook_${chapter.number.toString().padStart(2, '0')}.md"
            val workbookText = assetFile(workbookPath).readText(Charsets.UTF_8)
            val blocks = TextbookMarkdownParser.parse("$chapterText\n\n---\n\n$workbookText")
            val sections = TextbookSectioner.split(chapter.id, blocks)
            val workbookStart = sections.indexOfFirst { it.isWorkbook }

            assertTrue("${chapter.id}: workbook start missing", workbookStart > 0)
            assertTrue("${chapter.id}: all sections after workbook start marked", sections.drop(workbookStart).all { it.isWorkbook })
            assertTrue("${chapter.id}: main text remains non-workbook", sections.take(workbookStart).none { it.isWorkbook })

            sections.forEach { section ->
                val page = TextbookLearningFlow.buildConcepts(chapter.id, section, lesson).single()
                if (section.isWorkbook) {
                    assertTrue("${section.id}: workbook must not get duplicate auto problem", page.problem == null)
                } else {
                    assertTrue("${section.id}: main text keeps low-stakes check", page.problem != null)
                }
            }
        }
    }

    @Test
    fun answerNormalization_isStableForLowStakesChecks() {
        assertEquals(
            TextbookLearningFlow.normalizeAnswer("  SHA-256   "),
            TextbookLearningFlow.normalizeAnswer("sha-256")
        )
    }
}
