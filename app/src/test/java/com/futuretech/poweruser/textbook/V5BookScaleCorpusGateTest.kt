package com.futuretech.poweruser.textbook

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * User contract gate for the V5 rewrite.
 *
 * IMPORTANT: this test intentionally derives the baseline from the repository's current learner-facing
 * corpus. No hand-entered byte/character target is allowed. Repeated text does not make a book deep;
 * a second gate therefore checks exact paragraphs and token shingles for padding.
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

    private fun currentAllTracksBaselineSemanticChars(): Long {
        val authored = V1TextbookCatalog.chapters.sumOf { track ->
            val markdown = repoFile("src/main/assets/${track.assetPath}").readText(Charsets.UTF_8)
            semanticChars(TextbookMarkdownParser.parse(markdown))
        }
        val injectedDepth = V4BookDepthLibrary.sectionIds().sumOf { sectionId ->
            semanticChars(V4BookDepthLibrary.blocksFor(sectionId))
        }
        return authored + injectedDepth
    }

    private fun v5TrackFiles(trackNumber: Int): List<File> {
        val dir = repoFile("src/main/assets/textbook/v5/track_${trackNumber.toString().padStart(2, '0')}")
        assertTrue("V5 track directory must be a directory: $dir", dir.isDirectory)
        val files = dir.walkTopDown()
            .filter { it.isFile && it.extension.equals("md", ignoreCase = true) }
            .sortedBy { it.relativeTo(dir).invariantSeparatorsPath }
            .toList()
        assertTrue("TRACK $trackNumber must be split into multiple authored book parts", files.size >= 2)
        return files
    }

    private fun parsedTrackBlocks(trackNumber: Int): List<TextbookBlock> =
        v5TrackFiles(trackNumber).flatMap { TextbookMarkdownParser.parse(it.readText(Charsets.UTF_8)) }

    @Test
    fun everyTrackIsAtLeastFiveTimesTheEntireCurrentElevenTrackCorpus() {
        val baseline = currentAllTracksBaselineSemanticChars()
        assertTrue("baseline must be non-trivial: $baseline", baseline > 100_000L)
        val minimumPerTrack = baseline * 5L

        (1..11).forEach { trackNumber ->
            val actual = semanticChars(parsedTrackBlocks(trackNumber))
            assertTrue(
                "TRACK ${trackNumber.toString().padStart(2, '0')} is below literal 5x contract: actual=$actual minimum=$minimumPerTrack baseline=$baseline",
                actual >= minimumPerTrack
            )
        }
    }

    @Test
    fun noLongParagraphIsCopiedToPadMultipleLocations() {
        val owners = linkedMapOf<String, MutableList<String>>()
        (1..11).forEach { trackNumber ->
            v5TrackFiles(trackNumber).forEach { file ->
                val location = "T${trackNumber.toString().padStart(2, '0')}/${file.name}"
                TextbookMarkdownParser.parse(file.readText(Charsets.UTF_8))
                    .filterIsInstance<TextbookBlock.Paragraph>()
                    .forEach { paragraph ->
                        val normalized = paragraph.text.replace(Regex("\\s+"), " ").trim()
                        if (normalized.length >= 100) {
                            owners.getOrPut(normalized) { mutableListOf() }.add(location)
                        }
                    }
            }
        }

        val duplicates = owners.filterValues { it.size > 1 }
        assertTrue(
            "Long paragraph duplication is padding, not depth. duplicateGroups=${duplicates.size} examples=${duplicates.values.take(5)}",
            duplicates.isEmpty()
        )
    }

    @Test
    fun repeatedTwelveTokenShinglesStayBelowEightPercent() {
        val counts = hashMapOf<String, Int>()
        var total = 0L

        (1..11).forEach { trackNumber ->
            val text = v5TrackFiles(trackNumber)
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
        assertTrue("12-token repeated-shingle ratio too high: $ratio (duplicates=$duplicateOccurrences total=$total)", ratio <= 0.08)
    }

    @Test
    fun trackDirectoriesAreExactlyElevenAndUseStableNumbering() {
        val root = repoFile("src/main/assets/textbook/v5")
        val dirs = root.listFiles()?.filter { it.isDirectory && it.name.matches(Regex("track_\\d{2}")) }?.sortedBy { it.name }.orEmpty()
        assertEquals((1..11).map { "track_${it.toString().padStart(2, '0')}" }, dirs.map { it.name })
    }
}
