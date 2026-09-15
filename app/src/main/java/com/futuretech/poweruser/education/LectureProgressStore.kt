package com.futuretech.poweruser.education

import android.content.Context

class LectureProgressStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("lecture_progress_v1", Context.MODE_PRIVATE)

    fun sectionIndex(lessonId: String): Int = prefs.getInt("$lessonId.section", 0)

    fun saveSectionIndex(lessonId: String, index: Int) {
        prefs.edit().putInt("$lessonId.section", index.coerceAtLeast(0)).apply()
    }

    fun isLectureCompleted(lessonId: String): Boolean = prefs.getBoolean("$lessonId.completed", false)

    fun markLectureCompleted(lessonId: String) {
        prefs.edit()
            .putBoolean("$lessonId.completed", true)
            .putInt("$lessonId.section", 0)
            .apply()
    }

    fun resetLecture(lessonId: String) {
        prefs.edit()
            .remove("$lessonId.completed")
            .remove("$lessonId.section")
            .apply()
    }
}
