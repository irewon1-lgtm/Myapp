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
            ?: error("Cannot locate V2 TRACK asset: $assetPath")
    }

    @Test
    fun v2TracksHaveBookScaleHierarchyPracticeAndNoPadding() {
        V1TextbookCatalog.chapters.forEach { track ->
            val text = assetFile(track.assetPath).readText(Charsets.UTF_8)
            val lines = text.lines()
            val blockRows = lines.mapIndexedNotNull { index, line ->
                if (line.startsWith("## BLOCK ")) index to line else null
            }
            val lessonIndices = lines.mapIndexedNotNull { index, line ->
                index.takeIf { line.startsWith("### LESSON ") }
            }
            val supportLabels = listOf("핵심 용어 사전", "완료 기준")
            val instructionalBlocks = blockRows.filter { (_, heading) ->
                supportLabels.none { heading.contains(it) }
            }

            assertTrue("${track.id}: must start as TRACK", text.startsWith("# TRACK ${track.number.toString().padStart(2, '0')}"))
            assertTrue("${track.id}: book-scale BLOCK count=${blockRows.size}", blockRows.size >= 25)
            assertTrue("${track.id}: lesson count=${lessonIndices.size}", lessonIndices.size >= instructionalBlocks.size)
            assertTrue("${track.id}: accidental-truncation floor chars=${text.length}", text.length >= 8_000)
            assertTrue("${track.id}: code/data examples required", text.contains("```"))
            assertTrue("${track.id}: glossary required", text.contains("핵심 용어 사전"))
            assertTrue("${track.id}: completion criteria required", text.contains("완료 기준"))
            assertTrue(
                "${track.id}: project/practice required",
                text.contains("TRACK 프로젝트") || text.contains("최종 프로젝트") || text.contains("프로젝트 ·")
            )

            instructionalBlocks.forEach { (blockStart, heading) ->
                val nextBlockStart = blockRows.firstOrNull { it.first > blockStart }?.first ?: lines.size
                assertTrue(
                    "${track.id}: instructional BLOCK needs LESSON, heading=$heading",
                    lessonIndices.any { it in (blockStart + 1) until nextBlockStart }
                )
            }

            val substantiveParagraphs = text
                .split(Regex("\\n\\s*\\n"))
                .map { it.replace(Regex("\\s+"), " ").trim() }
                .filter { paragraph ->
                    paragraph.length >= 120 &&
                        !paragraph.startsWith("```") &&
                        !paragraph.startsWith("|")
                }
            val duplicates = substantiveParagraphs.groupingBy { it }.eachCount().filterValues { it > 1 }
            assertTrue(
                "${track.id}: repeated explanatory padding found: ${duplicates.keys.take(3)}",
                duplicates.isEmpty()
            )

            val placeholders = listOf("TODO", "TBD", "LOREM", "나중에 작성", "준비중")
            assertTrue(
                "${track.id}: placeholder text found",
                placeholders.none { marker -> text.contains(marker, ignoreCase = true) }
            )
        }
    }

    @Test
    fun prerequisiteHeavyTopicsAppearInTeachingOrder() {
        fun text(trackNumber: Int): String = assetFile(
            requireNotNull(V1TextbookCatalog.chapters.find { it.number == trackNumber }).assetPath
        ).readText(Charsets.UTF_8)

        fun ordered(source: String, headings: List<String>): Boolean {
            var cursor = -1
            for (heading in headings) {
                val next = source.indexOf(heading, startIndex = cursor + 1)
                if (next < 0 || next <= cursor) return false
                cursor = next
            }
            return true
        }

        assertTrue(
            "TRACK 05 async prerequisites must be incremental",
            ordered(
                text(5),
                listOf(
                    "## BLOCK 24 · 비동기를 배우기 전에 기다림",
                    "## BLOCK 28 · callback",
                    "## BLOCK 29 · Promise",
                    "## BLOCK 38 · event loop",
                    "## BLOCK 39 · microtask",
                    "## BLOCK 44 · race condition"
                )
            )
        )

        assertTrue(
            "TRACK 06 MIME/upload vocabulary must be introduced in order",
            ordered(
                text(6),
                listOf(
                    "## BLOCK 31 · 데이터 종류를 왜 알려줘야 하나",
                    "## BLOCK 32 · MIME type",
                    "## BLOCK 34 · Content-Type",
                    "## BLOCK 35 · Accept",
                    "## BLOCK 38 · 파일 업로드 문제",
                    "## BLOCK 39 · multipart/form-data",
                    "## BLOCK 40 · boundary"
                )
            )
        )

        assertTrue(
            "TRACK 08 database integrity/ACID vocabulary must be incremental",
            ordered(
                text(8),
                listOf(
                    "## BLOCK 20 · PRIMARY KEY",
                    "## BLOCK 26 · FOREIGN KEY",
                    "## BLOCK 28 · 데이터 무결성",
                    "## BLOCK 44 · transaction을 배우기 전에",
                    "## BLOCK 45 · transaction",
                    "## BLOCK 47 · Atomicity",
                    "## BLOCK 48 · Consistency",
                    "## BLOCK 49 · Isolation",
                    "## BLOCK 50 · Durability"
                )
            )
        )
    }
}
