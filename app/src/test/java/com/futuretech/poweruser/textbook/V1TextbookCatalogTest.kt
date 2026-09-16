package com.futuretech.poweruser.textbook

import com.futuretech.poweruser.data.CurriculumDataRepository
import org.junit.Assert.*
import org.junit.Test

class V1TextbookCatalogTest {
    @Test fun catalogHasExactlyElevenOrderedTracks() {
        val tracks = V1TextbookCatalog.chapters
        assertEquals(11, tracks.size)
        assertEquals((1..11).toList(), tracks.map { it.number })
        assertEquals(11, tracks.map { it.id }.toSet().size)
        assertEquals(11, tracks.map { it.practiceLessonId }.toSet().size)
        assertEquals((1..11).map { "V2-T%02d".format(it) }, tracks.map { it.practiceLessonId })
    }

    @Test fun legacySourceLessonIdsRemainStableForProgressCompatibility() {
        val ids = V1TextbookCatalog.allSourceLessonIds
        assertEquals(28, ids.size)
        assertEquals(28, ids.toSet().size)
        assertEquals("V1-01", ids.first())
        assertEquals("V1-28", ids.last())
    }

    @Test fun everyTrackHasBookScaleLearningMetadataAndV2Asset() {
        V1TextbookCatalog.chapters.forEach { track ->
            assertTrue(track.summary.length >= 30)
            assertTrue(track.keyConcepts.size >= 5)
            assertTrue(track.humanMustKnow.length >= 30)
            assertTrue(track.aiCanHelp.length >= 30)
            assertTrue(track.estimatedReadMinutes >= 1200)
            assertTrue(track.assetPath.startsWith("textbook/v2/track_"))
            assertTrue(track.assetPath.endsWith(".md"))
        }
    }

    @Test fun everyTrackHasMatchingV2PracticeLesson() {
        val practices = CurriculumDataRepository.v2TrackPracticeLessons
        assertEquals(11, practices.size)
        V1TextbookCatalog.chapters.forEach { track ->
            val lesson = CurriculumDataRepository.lessonById(track.practiceLessonId)
            assertNotNull(lesson)
            assertEquals("TEXTBOOK_V2", lesson!!.curriculumType)
            assertEquals(track.number, lesson.stepNumber)
            assertEquals(11, lesson.stepTotal)
            assertTrue(lesson.explainKeywords.size >= 5)
            assertTrue(lesson.aiHallucinationOptions.size >= 2)
            assertEquals(track.title, lesson.title)
        }
    }
}
