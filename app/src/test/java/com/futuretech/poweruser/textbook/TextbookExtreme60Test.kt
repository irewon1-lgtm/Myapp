package com.futuretech.poweruser.textbook

import com.futuretech.poweruser.data.CurriculumDataRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TextbookExtreme60Test(private val caseId: Int) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "curriculum-section-extreme-{0}")
        fun cases(): Collection<Array<Any>> = (1..60).map { arrayOf<Any>(it) }
    }

    @Test
    fun curriculumAndSectionScenarioPasses() {
        val books = PowerUserCurriculumCatalog.books
        val allChapters = PowerUserCurriculumCatalog.chapters
        val expectedCounts = listOf(11, 19, 14, 14, 14, 16, 15, 15, 16)

        assertEquals(9, books.size)
        assertEquals(134, allChapters.size)
        assertEquals(expectedCounts, books.map { it.chapters.size })
        assertEquals(134, allChapters.map { it.id }.toSet().size)

        val book = books[(caseId - 1) % books.size]
        assertEquals("V${book.number}", book.id)
        assertTrue(book.title.isNotBlank())
        assertEquals((1..book.chapters.size).toList(), book.chapters.map { it.number })

        val chapter = allChapters[((caseId - 1) * 17) % allChapters.size]
        assertTrue(chapter.id.matches(Regex("V[1-9]-C\\d{2}")))
        assertTrue(chapter.title.length >= 5)
        if (chapter.id.startsWith("V1-")) {
            assertTrue(chapter.contentAvailable)
            val actual = V1TextbookCatalog.chapterById(chapter.id)
            assertNotNull(actual)
            assertEquals(chapter.title, actual!!.title)
            assertNotNull(CurriculumDataRepository.lessonById(actual.practiceLessonId))
        } else {
            assertFalse(chapter.contentAvailable)
        }

        val repeatCount = 7 + (caseId % 7)
        val markdown = buildString {
            appendLine("# Chapter $caseId")
            repeat(repeatCount) { index ->
                appendLine("## 개념 ${index + 1}")
                appendLine(("설명${index + 1} ".repeat(90 + (caseId % 20))).trim())
                if (index % 3 == 0) {
                    appendLine("```python")
                    appendLine("value = ${index + caseId}")
                    appendLine("print(value)")
                    appendLine("```")
                }
            }
        }
        val blocks = TextbookMarkdownParser.parse(markdown)
        val sections = TextbookSectioner.split("TEST-$caseId", blocks)

        assertTrue(sections.isNotEmpty())
        assertEquals(blocks, sections.flatMap { it.blocks })
        assertEquals(sections.size, sections.map { it.id }.toSet().size)
        assertTrue(sections.all { it.estimatedMinutes in 4..7 })
        assertTrue(sections.all { it.title.isNotBlank() })
        assertTrue(sections.all { it.weightedLength > 0 })

        if (caseId % 10 == 0) {
            assertEquals(11, V1TextbookCatalog.chapters.size)
            assertEquals(28, V1TextbookCatalog.allSourceLessonIds.toSet().size)
            assertEquals(11, V1TextbookCatalog.practiceLessonIds.size)
            assertEquals(123, allChapters.count { !it.contentAvailable })
        }
    }
}
