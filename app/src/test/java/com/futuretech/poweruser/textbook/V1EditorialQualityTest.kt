package com.futuretech.poweruser.textbook

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V1EditorialQualityTest {
    private fun assetFile(assetPath: String): File {
        val candidates = listOf(
            File("src/main/assets/$assetPath"),
            File("app/src/main/assets/$assetPath")
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("Cannot locate V3 TRACK asset: $assetPath")
    }

    @Test
    fun v3TracksUseFewLargeBeginnerBlocksInsteadOfVocabularyPages() {
        V1TextbookCatalog.chapters.forEach { track ->
            val file = assetFile(track.assetPath)
            val text = file.readText(Charsets.UTF_8)
            val lines = text.lines()
            val blockRows = lines.mapIndexedNotNull { index, line ->
                if (line.startsWith("## BLOCK ")) index to line else null
            }
            val lessonRows = lines.mapIndexedNotNull { index, line ->
                if (line.startsWith("### LESSON ")) index to line else null
            }

            assertTrue(
                "${track.id}: must start as TRACK",
                text.startsWith("# TRACK ${track.number.toString().padStart(2, '0')}")
            )
            assertTrue(
                "${track.id}: V3 intentionally uses a small number of large authored BLOCKs; count=${blockRows.size}",
                blockRows.size in 3..8
            )
            assertEquals(
                "${track.id}: each authored BLOCK owns one deep source lesson",
                blockRows.size,
                lessonRows.size
            )
            assertTrue(
                "${track.id}: accidental-truncation floor bytes=${file.length()}",
                file.length() >= 30_000L
            )
            assertTrue("${track.id}: executable/code or structured-data examples required", text.contains("```"))

            blockRows.forEachIndexed { blockIndex, (blockStart, heading) ->
                val nextBlockStart = blockRows.getOrNull(blockIndex + 1)?.first ?: lines.size
                val blockText = lines.subList(blockStart, nextBlockStart).joinToString("\n")
                val stepCount = lines.subList(blockStart, nextBlockStart).count { it.startsWith("#### ") }

                assertTrue(
                    "${track.id}: $heading is still too short (${blockText.length} chars)",
                    blockText.length >= 2_500
                )
                assertTrue(
                    "${track.id}: $heading needs step-by-step subsections; count=$stepCount",
                    stepCount >= 5
                )
                assertTrue(
                    "${track.id}: $heading needs an authored LESSON heading",
                    lines.subList(blockStart, nextBlockStart).any { it.startsWith("### LESSON ") }
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
    fun prerequisiteHeavyTopicsStillProgressFromEasyProblemToAdvancedMechanism() {
        fun text(trackNumber: Int): String = assetFile(
            requireNotNull(V1TextbookCatalog.chapters.find { it.number == trackNumber }).assetPath
        ).readText(Charsets.UTF_8)

        fun ordered(source: String, markers: List<String>): Boolean {
            var cursor = -1
            for (marker in markers) {
                val next = source.indexOf(marker, startIndex = cursor + 1)
                if (next < 0 || next <= cursor) return false
                cursor = next
            }
            return true
        }

        assertTrue(
            "TRACK 05 async teaching order must stay incremental",
            ordered(
                text(5),
                listOf(
                    "## BLOCK 04 · 시간이 걸리는 일을 기다리는 방법부터 비동기를 시작한다",
                    "## BLOCK 05 · Promise와 async/await는 미래의 성공과 실패를 다루는 방법이다",
                    "## BLOCK 06 · \"나중\"은 정확히 언제 실행되는가",
                    "## BLOCK 07 · 여러 비동기 작업이 동시에 존재할 때 생기는 문제를 다룬다"
                )
            )
        )

        assertTrue(
            "TRACK 06 HTTP/data/API/browser-policy order must stay incremental",
            ordered(
                text(6),
                listOf(
                    "## BLOCK 04 · HTTP 요청과 응답을 실제 메시지처럼 읽는다",
                    "## BLOCK 05 · 데이터가 무엇인지 알려 주고 여러 종류를 한 요청에 담는다",
                    "## BLOCK 06 · API는 프로그램이 다른 프로그램의 기능을 쓰는 약속이다",
                    "## BLOCK 07 · 브라우저 보안 정책과 cache, 실시간 통신, 실제 네트워크 디버깅을 연결한다"
                )
            )
        )

        assertTrue(
            "TRACK 08 integrity must precede relations/performance/transaction",
            ordered(
                text(8),
                listOf(
                    "## BLOCK 04 · 데이터가 말이 안 되는 상태가 되지 않도록 DB 자체에 규칙을 둔다",
                    "## BLOCK 05 · 현실의 관계를 table로 설계한다",
                    "## BLOCK 07 · 원하는 row를 빨리 찾도록 index와 query plan을 이해한다",
                    "## BLOCK 08 · 여러 변경을 한 작업처럼 안전하게 처리한다"
                )
            )
        )
    }
}
