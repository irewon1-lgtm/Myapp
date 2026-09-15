package com.futuretech.poweruser.textbook

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class V1EditorialQualityTest {
    private fun assetFile(assetPath: String): File {
        val candidates = listOf(
            File("src/main/assets/$assetPath"),
            File("app/src/main/assets/$assetPath")
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("Cannot locate V1 asset: $assetPath")
    }

    @Test
    fun v1RejectsBloatRepetitionAndExerciseFreeChapters() {
        V1TextbookCatalog.chapters.forEach { chapter ->
            val text = assetFile(chapter.assetPath).readText(Charsets.UTF_8)
            val lines = text.lines()
            // Only headings that can become learner-facing section boundaries count as fragmentation.
            // Level-4 headings are explanatory labels inside a section, not separate lessons.
            val sectionHeadings = lines.count { it.matches(Regex("^#{1,3}\\s+.+")) }
            // The capstone intentionally has multiple independent incident projects, each with evidence,
            // hypothesis, experiment and pass-criteria subsections. Those are real work stages, not padding.
            val headingLimit = if (chapter.number == 11) 60 else 34

            assertTrue("${chapter.id}: excessive section fragmentation headings=$sectionHeadings", sectionHeadings <= headingLimit)
            assertTrue("${chapter.id}: must contain executable/code examples", text.contains("```"))
            assertTrue(
                "${chapter.id}: must contain hands-on work",
                text.contains("실습") || text.contains("프로젝트")
            )
            assertTrue(
                "${chapter.id}: must contain a real end-of-chapter task",
                text.contains("장 끝 미니 프로젝트") ||
                    text.contains("V1 최종 실기시험") ||
                    text.contains("종합 훈련")
            )

            val paragraphs = text
                .split(Regex("\\n\\s*\\n"))
                .map { it.replace(Regex("\\s+"), " ").trim() }
                .filter { paragraph ->
                    paragraph.length >= 100 &&
                        !paragraph.startsWith("```") &&
                        !paragraph.startsWith("|")
                }
            val duplicateParagraphs = paragraphs
                .groupingBy { it }
                .eachCount()
                .filterValues { it > 1 }

            assertTrue(
                "${chapter.id}: duplicate explanatory paragraphs found: ${duplicateParagraphs.keys.take(3)}",
                duplicateParagraphs.isEmpty()
            )

            val placeholders = listOf("TODO", "TBD", "LOREM", "나중에 작성", "준비중")
            assertTrue(
                "${chapter.id}: placeholder text found",
                placeholders.none { marker -> text.contains(marker, ignoreCase = true) }
            )

            if (chapter.number < 11) {
                assertTrue(
                    "${chapter.id}: must end with capability-based completion criteria",
                    text.contains("이 장을 끝내고 할 수 있어야 하는 것")
                )
                assertTrue(
                    "${chapter.id}: regular chapter needs its own mini project",
                    text.contains("장 끝 미니 프로젝트")
                )
            } else {
                val projectCount = Regex("(?m)^# 프로젝트\\s+[A-Z]\\.").findAll(text).count()
                assertTrue("${chapter.id}: capstone must have explicit pass criteria", text.contains("합격 기준"))
                assertTrue("${chapter.id}: capstone must contain multiple real projects, found=$projectCount", projectCount >= 4)
            }
        }
    }
}
