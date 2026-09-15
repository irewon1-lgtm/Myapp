package com.futuretech.poweruser.education

import com.futuretech.poweruser.data.CurriculumDataRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Items31To35ContractTest {
    private val lessons = CurriculumDataRepository.intermediateLessons
        .filter { it.unitNumber in 31..35 }
        .sortedBy { it.unitNumber }

    @Test
    fun items31To35KeepStableIdsAndOrder() {
        assertEquals(5, lessons.size)
        assertEquals(listOf(31, 32, 33, 34, 35), lessons.map { it.unitNumber })
        assertEquals(
            listOf("I07-01", "I07-02", "I07-03", "I07-04", "I07-05"),
            lessons.map { it.lessonId }
        )
        assertTrue(lessons.all { it.moduleNumber == 7 })
        assertTrue(lessons.all { it.moduleTitle == "오류 찾기와 테스트" })
    }

    @Test
    fun lessonsContainRealTeachingDepthInsteadOfOneLineDefinitions() {
        lessons.forEach { lesson ->
            assertTrue("${lesson.lessonId} explanation too short", lesson.explanation.length >= 450)
            assertTrue("${lesson.lessonId} outcome too short", lesson.expectedOutcome.length >= 25)
            assertTrue("${lesson.lessonId} practice missing", lesson.initialPracticeCode.lines().size >= 4)
            assertTrue("${lesson.lessonId} broken example missing", lesson.brokenCode.isNotBlank())
            assertTrue("${lesson.lessonId} fix missing", lesson.brokenCodeFix.isNotBlank())
            assertTrue("${lesson.lessonId} quiz weak", lesson.aiHallucinationOptions.size >= 3)
        }
    }

    @Test
    fun progressionTeachesReproduceIsolateHypothesisBoundaryRegression() {
        val combined = lessons.joinToString("\n") {
            listOf(it.title, it.explanation, it.expectedOutcome, it.fillInBlankPrompt).joinToString("\n")
        }
        listOf("재현", "마지막 정상 지점", "가설", "경계값", "회귀시험", "증거").forEach { keyword ->
            assertTrue("missing concept: $keyword", combined.contains(keyword))
        }
    }

    @Test
    fun practiceIsLocalFirstAndDoesNotRequirePaidNetworkCalls() {
        lessons.forEach { lesson ->
            assertEquals("PYTHON", lesson.practiceLanguage)
            val executable = listOf(
                lesson.codeSample,
                lesson.initialPracticeCode,
                lesson.brokenCode,
                lesson.brokenCodeFix
            ).joinToString("\n").lowercase()
            listOf("api.openai.com", "http://", "https://", "requests.get", "requests.post", "fetch(").forEach { token ->
                assertFalse("${lesson.lessonId} unexpectedly requires network token $token", executable.contains(token))
            }
        }
    }

    @Test
    fun item34ActuallyExercisesBoundaryValues() {
        val lesson = lessons.single { it.unitNumber == 34 }
        val text = listOf(lesson.codeSample, lesson.initialPracticeCode, lesson.brokenCodeFix).joinToString("\n")
        listOf("79", "80", "81").forEach { assertTrue(text.contains(it)) }
        assertTrue(text.contains(">= 80"))
    }

    @Test
    fun item35ForbidsFakePassAndRequiresExecutionEvidence() {
        val lesson = lessons.single { it.unitNumber == 35 }
        val text = listOf(
            lesson.explanation,
            lesson.expectedOutcome,
            lesson.initialPracticeCode,
            lesson.brokenCode,
            lesson.brokenCodeFix,
            lesson.aiHallucinationQuestion
        ).joinToString("\n")
        assertTrue(text.contains("실행"))
        assertTrue(text.contains("증거"))
        assertTrue(text.contains("PASS"))
        assertTrue(text.contains("회귀시험"))
        assertTrue(text.contains("실제로 실행하지"))
    }
}
