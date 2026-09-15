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
class TextbookClean25Test(private val gate: Int) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "curriculum-section-clean-{0}")
        fun gates(): Collection<Array<Any>> = (1..25).map { arrayOf<Any>(it) }
    }

    @Test
    fun cleanGatePasses() {
        val books = PowerUserCurriculumCatalog.books
        val all = PowerUserCurriculumCatalog.chapters
        val v1 = V1TextbookCatalog.chapters
        val practices = CurriculumDataRepository.v1TextbookPracticeLessons
        val sampleBlocks = TextbookMarkdownParser.parse(
            buildString {
                appendLine("# 샘플")
                repeat(12) { index ->
                    appendLine("## 단위 ${index + 1}")
                    appendLine(("개념을 실제 상황에 연결해서 설명한다. ".repeat(40)).trim())
                }
            }
        )
        val sections = TextbookSectioner.split("CLEAN", sampleBlocks)

        when (gate) {
            1 -> assertEquals(9, books.size)
            2 -> assertEquals(134, all.size)
            3 -> assertEquals(listOf(11, 19, 14, 14, 14, 16, 15, 15, 16), books.map { it.chapters.size })
            4 -> assertEquals(134, all.map { it.id }.toSet().size)
            5 -> assertTrue(books.all { it.chapters.map { chapter -> chapter.title }.toSet().size == it.chapters.size })
            6 -> assertEquals(11, all.count { it.contentAvailable })
            7 -> assertEquals(123, all.count { !it.contentAvailable })
            8 -> assertEquals(v1.map { it.id }, books.first().chapters.map { it.id })
            9 -> assertEquals(v1.map { it.title }, books.first().chapters.map { it.title })
            10 -> assertEquals((1..11).toList(), v1.map { it.number })
            11 -> assertEquals(28, V1TextbookCatalog.allSourceLessonIds.size)
            12 -> assertEquals(28, V1TextbookCatalog.allSourceLessonIds.toSet().size)
            13 -> assertEquals(11, V1TextbookCatalog.practiceLessonIds.size)
            14 -> assertEquals(11, practices.size)
            15 -> assertTrue(practices.all { it.stepTotal == 11 })
            16 -> assertTrue(sections.size >= 4)
            17 -> assertEquals(sampleBlocks, sections.flatMap { it.blocks })
            18 -> assertEquals(sections.size, sections.map { it.id }.toSet().size)
            19 -> assertTrue(sections.all { it.estimatedMinutes in 4..7 })
            20 -> assertTrue(sections.all { it.title.isNotBlank() })
            21 -> assertTrue(sections.all { it.weightedLength > 0 })
            22 -> assertTrue(all.none { it.title.contains("TODO", true) || it.title.contains("TBD", true) })
            23 -> assertFalse(all.any { it.id.startsWith("V1-") && !it.contentAvailable })
            24 -> assertNotNull(PowerUserCurriculumCatalog.chapterById("V9-C16"))
            25 -> assertEquals("V9", PowerUserCurriculumCatalog.bookForChapter("V9-C16")?.id)
        }
    }
}
