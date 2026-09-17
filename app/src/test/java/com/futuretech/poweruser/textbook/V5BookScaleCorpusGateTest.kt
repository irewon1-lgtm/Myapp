package com.futuretech.poweruser.textbook

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Literal book-scale contract from the user.
 *
 * The frozen baseline is the complete learner-facing V3/V4 corpus which existed when the 5x rule
 * was fixed: authored V3 blocks + beginner guidance injected around every learner section + active
 * expert depth. Every V5 TRACK must individually contain at least five times that semantic corpus.
 * Average size, sum across tracks, or unreferenced padding files cannot satisfy this gate.
 */
class V5BookScaleCorpusGateTest {
    private fun repoFile(relative: String): File {
        val candidates = listOf(File(relative), File("app/$relative"))
        return candidates.firstOrNull { it.exists() }
            ?: error("Cannot locate repository path: $relative; cwd=${File(".").absolutePath}")
    }

    private fun semanticChars(blocks: List<TextbookBlock>): Long = blocks.sumOf { block ->
        val text = when (block) {
            is TextbookBlock.Heading -> block.text
            is TextbookBlock.Paragraph -> block.text
            is TextbookBlock.BulletList -> block.items.joinToString(" ")
            is TextbookBlock.Code -> block.text
            is TextbookBlock.Table -> block.headers.joinToString(" ") + " " + block.rows.flatten().joinToString(" ")
            TextbookBlock.Divider -> ""
        }
        text.replace(Regex("\\s+"), "").length.toLong()
    }

    private fun frozenLearnerFacingBaselineSemanticChars(): Long =
        V1TextbookCatalog.chapters.sumOf { track ->
            val markdown = repoFile("src/main/assets/${track.assetPath}").readText(Charsets.UTF_8)
            val authored = TextbookMarkdownParser.parse(markdown)
            val sections = TextbookSectioner.split(track.id, authored)
            sections.sumOf { section ->
                val guided = V3BeginnerGuidanceResolver.decorateBlocks(section.id, section.blocks)
                val expert = V4BookDepthLibrary.blocksFor(section.id)
                semanticChars(guided) + semanticChars(expert)
            }
        }

    private fun manifestFiles(trackDir: File): List<File> = trackDir.listFiles().orEmpty()
        .filter { it.isFile && (it.name == "manifest.json" || it.name.matches(Regex("manifest_\\d{2}\\.json"))) }
        .sortedWith(compareBy<File> { if (it.name == "manifest.json") 0 else 1 }.thenBy { it.name })

    private fun manifestReferencedTrackFiles(trackNumber: Int): List<File> {
        val root = repoFile("src/main/assets")
        val trackDir = repoFile("src/main/assets/textbook/v5/track_${trackNumber.toString().padStart(2, '0')}")
        assertTrue("V5 track directory must be a directory: $trackDir", trackDir.isDirectory)
        val manifests = manifestFiles(trackDir)
        assertTrue("TRACK $trackNumber must have manifest.json", manifests.firstOrNull()?.name == "manifest.json")
        val paths = manifests.flatMap { manifest ->
            Regex("\\\"assetPath\\\"\\s*:\\s*\\\"([^\\\"]+\\.md)\\\"")
                .findAll(manifest.readText(Charsets.UTF_8))
                .map { it.groupValues[1] }
                .toList()
        }
        assertTrue("TRACK $trackNumber must contain multiple manifest-referenced book parts", paths.size >= 2)
        assertEquals("TRACK $trackNumber manifests must not reference a part twice", paths.size, paths.distinct().size)
        return paths.map { path ->
            val file = File(root, path)
            assertTrue("Manifest references missing part: $path", file.isFile)
            file
        }
    }

    private fun parsedTrackBlocks(trackNumber: Int): List<TextbookBlock> =
        manifestReferencedTrackFiles(trackNumber)
            .flatMap { TextbookMarkdownParser.parse(it.readText(Charsets.UTF_8)) }

    @Test
    fun everyTrackIndividuallyExceedsFiveTimesTheFrozenWholeBookBaseline() {
        val baseline = frozenLearnerFacingBaselineSemanticChars()
        assertTrue("frozen learner-facing baseline must be substantial: $baseline", baseline > 100_000L)
        val minimumPerTrack = baseline * 5L

        (1..11).forEach { trackNumber ->
            val actual = semanticChars(parsedTrackBlocks(trackNumber))
            assertTrue(
                "TRACK ${trackNumber.toString().padStart(2, '0')} violates literal 5x rule: actual=$actual minimum=$minimumPerTrack baseline=$baseline",
                actual >= minimumPerTrack
            )
        }
    }

    @Test
    fun noLongParagraphIsCopiedToPadTheCorpus() {
        val owners = linkedMapOf<String, MutableList<String>>()
        (1..11).forEach { trackNumber ->
            manifestReferencedTrackFiles(trackNumber).forEach { file ->
                val location = "T${trackNumber.toString().padStart(2, '0')}/${file.name}"
                TextbookMarkdownParser.parse(file.readText(Charsets.UTF_8))
                    .filterIsInstance<TextbookBlock.Paragraph>()
                    .forEach { paragraph ->
                        val normalized = paragraph.text.replace(Regex("\\s+"), " ").trim()
                        if (normalized.length >= 80) owners.getOrPut(normalized) { mutableListOf() }.add(location)
                    }
            }
        }
        val duplicates = owners.filterValues { it.size > 1 }
        assertTrue(
            "Repeated long paragraphs are padding, not depth. groups=${duplicates.size} examples=${duplicates.values.take(5)}",
            duplicates.isEmpty()
        )
    }

    @Test
    fun repeatedTwelveTokenShinglesStayBelowEightPercent() {
        val counts = hashMapOf<String, Int>()
        var total = 0L
        (1..11).forEach { trackNumber ->
            val text = manifestReferencedTrackFiles(trackNumber)
                .joinToString("\n") { it.readText(Charsets.UTF_8) }
                .replace(Regex("```[\\s\\S]*?```"), " ")
                .replace(Regex("[#>*_`|\\-]+"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            val tokens = text.split(' ').filter { it.isNotBlank() }
            if (tokens.size >= 12) {
                for (i in 0..tokens.size - 12) {
                    val shingle = tokens.subList(i, i + 12).joinToString(" ")
                    counts[shingle] = (counts[shingle] ?: 0) + 1
                    total++
                }
            }
        }
        val duplicateOccurrences = counts.values.sumOf { (it - 1).coerceAtLeast(0).toLong() }
        val ratio = if (total == 0L) 1.0 else duplicateOccurrences.toDouble() / total.toDouble()
        assertTrue("12-token repeated-shingle ratio too high: $ratio", ratio <= 0.08)
    }

    @Test
    fun trackDirectoriesAreExactlyElevenAndStable() {
        val root = repoFile("src/main/assets/textbook/v5")
        val dirs = root.listFiles()?.filter { it.isDirectory && it.name.matches(Regex("track_\\d{2}")) }?.sortedBy { it.name }.orEmpty()
        assertEquals((1..11).map { "track_${it.toString().padStart(2, '0')}" }, dirs.map { it.name })
    }
}
