package com.futuretech.poweruser.textbook

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class V3CriticalCoverageTest {
    private fun assetFile(assetPath: String): File {
        val candidates = listOf(
            File("src/main/assets/$assetPath"),
            File("app/src/main/assets/$assetPath")
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("Cannot locate V3 TRACK asset: $assetPath")
    }

    private val requiredMarkers = mapOf(
        1 to listOf("입력(input)", "CPU", "RAM", "UTF-8", "프로그램"),
        2 to listOf("decomposition", "변수", "if", "for", "함수", "오류"),
        3 to listOf("array", "node", "stack", "hash", "tree", "Big-O"),
        4 to listOf("HTML", "CSS", "DOM", "event", "반응형"),
        5 to listOf("const", "closure", "Promise", "async/await", "event loop", "TypeScript"),
        6 to listOf("DNS", "TCP", "TLS", "Content-Type", "multipart/form-data", "CORS"),
        7 to listOf("route", "middleware", "validation", "authentication", "queue", "관측성"),
        8 to listOf("PRIMARY KEY", "FOREIGN KEY", "JOIN", "index", "transaction", "ACID"),
        9 to listOf("threat", "authentication", "hash", "encryption", "SQL injection", "XSS", "CSRF", "secret"),
        10 to listOf("재현", "unit", "integration", "Git", "CI", "artifact", "rollback"),
        11 to listOf("requirement", "acceptance criteria", "cohesion", "coupling", "dependency injection", "cache", "circuit breaker", "observability")
    )

    @Test
    fun everyV3TrackKeepsItsCriticalConceptSpine() {
        V1TextbookCatalog.chapters.forEach { track ->
            val text = assetFile(track.assetPath).readText(Charsets.UTF_8)
            val markers = requireNotNull(requiredMarkers[track.number])
            val missing = markers.filterNot { marker -> text.contains(marker, ignoreCase = true) }
            assertTrue(
                "${track.id}: critical concepts disappeared during V2 -> V3 consolidation: $missing",
                missing.isEmpty()
            )
        }
    }

    @Test
    fun previouslyReportedBeginnerPainPointsRemainExplainedInContext() {
        val track3 = assetFile(requireNotNull(V1TextbookCatalog.chapters.find { it.number == 3 }).assetPath)
            .readText(Charsets.UTF_8)
        val track5 = assetFile(requireNotNull(V1TextbookCatalog.chapters.find { it.number == 5 }).assetPath)
            .readText(Charsets.UTF_8)
        val track6 = assetFile(requireNotNull(V1TextbookCatalog.chapters.find { it.number == 6 }).assetPath)
            .readText(Charsets.UTF_8)
        val track8 = assetFile(requireNotNull(V1TextbookCatalog.chapters.find { it.number == 8 }).assetPath)
            .readText(Charsets.UTF_8)

        assertTrue(track3.contains("node", true) && track3.contains("next", true) && track3.contains("중간", true))
        assertTrue(track5.contains("callback", true) && track5.contains("Promise", true) && track5.contains("microtask", true))
        assertTrue(track6.contains("MIME", true) && track6.contains("Content-Type", true) && track6.contains("boundary", true))
        assertTrue(track8.contains("데이터 무결성", true) && track8.contains("CHECK", true) && track8.contains("참조 무결성", true))
    }
}
