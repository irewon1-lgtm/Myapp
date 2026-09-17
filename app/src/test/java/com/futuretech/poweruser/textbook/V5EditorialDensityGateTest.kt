package com.futuretech.poweruser.textbook

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rejects learner-facing filler, repeated teaching scaffolding, and shallow example padding. */
class V5EditorialDensityGateTest {
    private fun repoFile(path: String): File {
        val candidates = listOf(File(path), File("app/$path"))
        return candidates.firstOrNull { it.exists() } ?: error("Missing path: $path")
    }

    private fun manifestFiles(track: File): List<File> = track.listFiles().orEmpty()
        .filter { it.isFile && (it.name == "manifest.json" || it.name.matches(Regex("manifest_\\d{2}\\.json"))) }
        .sortedWith(compareBy<File> { if (it.name == "manifest.json") 0 else 1 }.thenBy { it.name })

    private fun activeParts(): List<File> {
        val root = repoFile("src/main/assets/textbook/v5")
        return root.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.matches(Regex("track_\\d{2}")) }
            .sortedBy { it.name }
            .flatMap { track ->
                manifestFiles(track).flatMap { manifest ->
                    Regex("\\\"assetPath\\\"\\s*:\\s*\\\"([^\\\"]+\\.md)\\\"")
                        .findAll(manifest.readText(Charsets.UTF_8))
                        .map { File(root.parentFile.parentFile, it.groupValues[1]) }
                        .toList()
                }
            }
    }

    private val forbidden = listOf(
        "이 PART의 목표",
        "이 PART에서는",
        "이 PART에서 반드시",
        "다음 PART에서는",
        "다음 PART에서",
        "지금은 외우지",
        "외울 필요",
        "외울 단어",
        "초보자",
        "쉽게 생각",
        "간단히 말",
        "책을 덮고",
        "자가점검",
        "비유로",
        "비유하면",
        "여기서 중요한 것은",
        "기억해야 할 것은"
    )

    @Test
    fun activeV5BooksContainNoTeachingScaffoldingFiller() {
        val hits = mutableListOf<String>()
        activeParts().forEach { file ->
            val text = file.readText(Charsets.UTF_8)
            forbidden.forEach { phrase ->
                if (text.contains(phrase, ignoreCase = true)) hits += "${file.name}: $phrase"
            }
        }
        assertTrue("Filler/meta-teaching phrases remain: ${hits.take(40)}", hits.isEmpty())
    }

    @Test
    fun examplesAndCodeCannotDominateSemanticContent() {
        val offenders = mutableListOf<String>()
        activeParts().forEach { file ->
            val raw = file.readText(Charsets.UTF_8)
            val codeChars = Regex("```[\\s\\S]*?```").findAll(raw).sumOf { it.value.length }
            val proseChars = raw
                .replace(Regex("```[\\s\\S]*?```"), "")
                .replace(Regex("[#>*_`|\\-]+"), "")
                .count { !it.isWhitespace() }
            val ratio = codeChars.toDouble() / (codeChars + proseChars).coerceAtLeast(1)
            if (ratio > 0.28) offenders += "${file.name}: codeRatio=$ratio"
        }
        assertTrue("Code/example padding dominates prose: $offenders", offenders.isEmpty())
    }

    @Test
    fun eachChapterHasSubstantialNonCodeReasoning() {
        val offenders = mutableListOf<String>()
        activeParts().forEach { file ->
            val raw = file.readText(Charsets.UTF_8)
            val chunks = Regex("(?m)^## CHAPTER ").split(raw).drop(1)
            chunks.forEachIndexed { index, chunk ->
                val prose = chunk
                    .replace(Regex("```[\\s\\S]*?```"), "")
                    .replace(Regex("(?m)^#+.*$"), "")
                    .replace(Regex("\\s+"), "")
                if (prose.length < 420) offenders += "${file.name} CHAPTER ${index + 1}: prose=${prose.length}"
            }
        }
        assertTrue("Shallow chapters remain: ${offenders.take(40)}", offenders.isEmpty())
    }
}
