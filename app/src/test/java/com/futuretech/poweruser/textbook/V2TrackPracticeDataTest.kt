package com.futuretech.poweruser.textbook

import com.futuretech.poweruser.data.CurriculumDataRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V2TrackPracticeDataTest {
    @Test
    fun elevenTrackPracticesAreCompleteAndExecutableBySupportedRuntime() {
        val practices = CurriculumDataRepository.v2TrackPracticeLessons
        val supported = setOf("PYTHON", "HTML_JS", "SQL", "TYPESCRIPT")

        assertEquals(11, practices.size)
        assertEquals((1..11).map { "V2-T%02d".format(it) }, practices.map { it.lessonId })
        assertEquals((1..11).toList(), practices.map { it.stepNumber })

        practices.forEach { lesson ->
            assertEquals("${lesson.lessonId}: curriculum type", "TEXTBOOK_V2", lesson.curriculumType)
            assertEquals("${lesson.lessonId}: step total", 11, lesson.stepTotal)
            assertTrue("${lesson.lessonId}: supported runtime", lesson.practiceLanguage in supported)
            assertTrue("${lesson.lessonId}: explanation", lesson.explanation.length >= 40)
            assertTrue("${lesson.lessonId}: expected outcome", lesson.expectedOutcome.length >= 20)
            assertTrue("${lesson.lessonId}: sample code", lesson.codeSample.isNotBlank())
            assertTrue("${lesson.lessonId}: editable practice", lesson.initialPracticeCode.isNotBlank())
            assertTrue("${lesson.lessonId}: broken code", lesson.brokenCode.isNotBlank())
            assertTrue("${lesson.lessonId}: fixed code", lesson.brokenCodeFix.isNotBlank())
            assertNotEquals("${lesson.lessonId}: debugging must contain a real defect", lesson.brokenCode, lesson.brokenCodeFix)
            assertTrue("${lesson.lessonId}: fill blank must expose accepted answer metadata", lesson.fillInBlankPrompt.contains("[") && lesson.fillInBlankPrompt.contains("]"))
            assertTrue("${lesson.lessonId}: concept keywords", lesson.explainKeywords.size >= 5)
            assertTrue("${lesson.lessonId}: AI audit options", lesson.aiHallucinationOptions.size >= 2)
            assertTrue("${lesson.lessonId}: AI answer index", lesson.correctOptionIndex in lesson.aiHallucinationOptions.indices)
        }
    }

    @Test
    fun trackTitlesAndPracticeTitlesStayOneToOne() {
        val tracks = V1TextbookCatalog.chapters
        val practices = CurriculumDataRepository.v2TrackPracticeLessons
        assertEquals(tracks.map { it.title }, practices.map { it.title })
        assertEquals(tracks.map { it.practiceLessonId }, practices.map { it.lessonId })
    }

    @Test
    fun practiceSetCoversAllFourSandboxModesUsedByTheCourse() {
        val languages = CurriculumDataRepository.v2TrackPracticeLessons.map { it.practiceLanguage }.toSet()
        assertTrue("Python practice required", "PYTHON" in languages)
        assertTrue("HTML/JS practice required", "HTML_JS" in languages)
        assertTrue("TypeScript practice required", "TYPESCRIPT" in languages)
        assertTrue("SQL practice required", "SQL" in languages)
    }
}
