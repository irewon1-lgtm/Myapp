package com.futuretech.poweruser.education

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LectureProgressStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("lecture_progress_v1", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun sectionProgressSurvivesStoreRecreation() {
        LectureProgressStore(context).saveSectionIndex("B01-01", 7)
        assertEquals(7, LectureProgressStore(context).sectionIndex("B01-01"))
    }

    @Test
    fun completionSurvivesStoreRecreationAndCanReset() {
        val first = LectureProgressStore(context)
        assertFalse(first.isLectureCompleted("B01-01"))
        first.markLectureCompleted("B01-01")
        assertTrue(LectureProgressStore(context).isLectureCompleted("B01-01"))
        LectureProgressStore(context).resetLecture("B01-01")
        assertFalse(LectureProgressStore(context).isLectureCompleted("B01-01"))
        assertEquals(0, LectureProgressStore(context).sectionIndex("B01-01"))
    }
}
