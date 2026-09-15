package com.futuretech.poweruser.textbook

import com.futuretech.poweruser.data.CurriculumDataRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TextbookClean25Test(private val gate: Int) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "textbook-clean-{0}")
        fun gates(): Collection<Array<Any>> = (1..25).map { arrayOf<Any>(it) }
    }

    @Test fun cleanGatePasses() {
        val chapters = V1TextbookCatalog.chapters
        val practices = CurriculumDataRepository.v1TextbookPracticeLessons
        when (gate) {
            1 -> assertEquals(11, chapters.size)
            2 -> assertEquals(11, chapters.map { it.id }.toSet().size)
            3 -> assertEquals(11, chapters.map { it.title }.toSet().size)
            4 -> assertEquals((1..11).toList(), chapters.map { it.number })
            5 -> assertEquals(28, V1TextbookCatalog.allSourceLessonIds.size)
            6 -> assertEquals(28, V1TextbookCatalog.allSourceLessonIds.toSet().size)
            7 -> assertEquals(11, V1TextbookCatalog.practiceLessonIds.size)
            8 -> assertEquals(11, practices.size)
            9 -> assertTrue(chapters.all { it.keyConcepts.size >= 5 })
            10 -> assertTrue(chapters.all { it.summary.length >= 30 })
            11 -> assertTrue(chapters.all { it.humanMustKnow.length >= 30 })
            12 -> assertTrue(chapters.all { it.aiCanHelp.length >= 30 })
            13 -> assertTrue(chapters.all { it.estimatedReadMinutes >= 40 })
            14 -> assertTrue(practices.all { it.practiceLanguage == "PYTHON" })
            15 -> assertTrue(practices.all { it.stepTotal == 11 })
            16 -> assertTrue(practices.all { it.fillInBlankPrompt.contains("[") && it.fillInBlankPrompt.contains("]") })
            17 -> assertTrue(practices.all { it.aiHallucinationOptions.size >= 2 })
            18 -> assertTrue(practices.all { it.correctOptionIndex in it.aiHallucinationOptions.indices })
            19 -> assertTrue(practices.all { it.explainKeywords.size >= 5 })
            20 -> assertEquals(TextbookLayoutMode.COMPACT, TextbookLayoutMode.fromWidthDp(719))
            21 -> assertEquals(TextbookLayoutMode.MEDIUM, TextbookLayoutMode.fromWidthDp(720))
            22 -> assertEquals(TextbookLayoutMode.MEDIUM, TextbookLayoutMode.fromWidthDp(1079))
            23 -> assertEquals(TextbookLayoutMode.EXPANDED, TextbookLayoutMode.fromWidthDp(1080))
            24 -> assertTrue(chapters.none { it.title.contains("TODO", true) || it.summary.contains("TODO", true) })
            25 -> assertTrue(practices.none { it.codeSample.contains("TODO", true) || it.brokenCodeFix.contains("TODO", true) })
        }
    }
}
