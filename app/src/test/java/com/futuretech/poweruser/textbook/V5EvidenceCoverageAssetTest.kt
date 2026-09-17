package com.futuretech.poweruser.textbook

import com.google.gson.Gson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V5EvidenceCoverageAssetTest {
    private val gson = Gson()

    private fun asset(relative: String): File {
        val candidates = listOf(
            File("src/main/assets/$relative"),
            File("app/src/main/assets/$relative")
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("Missing V5 asset: $relative; cwd=${File(".").absolutePath}")
    }

    private fun v5Root(): File {
        val candidates = listOf(File("src/main/assets/textbook/v5"), File("app/src/main/assets/textbook/v5"))
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Missing V5 asset root; cwd=${File(".").absolutePath}")
    }

    private fun manifestFiles(trackDir: File): List<File> = trackDir.listFiles().orEmpty()
        .filter { it.isFile && (it.name == "manifest.json" || it.name.matches(Regex("manifest_\\d{2}\\.json"))) }
        .sortedWith(compareBy<File> { if (it.name == "manifest.json") 0 else 1 }.thenBy { it.name })

    private fun mergedManifest(trackDir: File): V5BookManifest {
        val files = manifestFiles(trackDir)
        assertTrue("${trackDir.name} must contain primary manifest.json", files.firstOrNull()?.name == "manifest.json")
        val fragments = files.map { gson.fromJson(it.readText(Charsets.UTF_8), V5BookManifest::class.java) }
        val base = fragments.first()
        fragments.forEachIndexed { index, fragment ->
            assertEquals("${files[index].name}: track id mismatch", base.trackId, fragment.trackId)
            assertEquals("${files[index].name}: track number mismatch", base.trackNumber, fragment.trackNumber)
            assertEquals("${files[index].name}: title mismatch", base.title, fragment.title)
            if (index > 0) assertTrue("${files[index].name}: shard cannot be empty", fragment.parts.isNotEmpty())
        }
        return base.copy(parts = fragments.flatMap { it.parts }.sortedBy { it.order })
    }

    @Test
    fun everyAuthoredPartHasOneOrderedEvidenceEntryPerH2Chapter() {
        val trackDirs = v5Root().listFiles()
            ?.filter { it.isDirectory && it.name.matches(Regex("track_\\d{2}")) }
            ?.sortedBy { it.name }
            .orEmpty()
        assertTrue("At least one V5 track must be authored", trackDirs.isNotEmpty())

        trackDirs.forEach { trackDir ->
            val manifest = mergedManifest(trackDir)
            val expectedTrackId = "T${manifest.trackNumber.toString().padStart(2, '0')}"
            assertEquals(expectedTrackId, manifest.trackId)
            assertEquals((1..manifest.parts.size).toList(), manifest.parts.map { it.order })
            assertEquals(manifest.parts.size, manifest.parts.map { it.id }.distinct().size)
            assertEquals(manifest.parts.size, manifest.parts.map { it.assetPath }.distinct().size)
            assertEquals(manifest.parts.size, manifest.parts.map { it.sourceMapPath }.distinct().size)

            manifest.parts.forEachIndexed { partIndex, part ->
                assertEquals(
                    "$expectedTrackId-P${(partIndex + 1).toString().padStart(2, '0')}",
                    part.id
                )
                val markdown = asset(part.assetPath).readText(Charsets.UTF_8)
                val chapters = TextbookMarkdownParser.parse(markdown)
                    .filterIsInstance<TextbookBlock.Heading>()
                    .filter { it.level == 2 && it.text.startsWith("CHAPTER ") }
                assertTrue("${part.id} needs real H2 chapters", chapters.isNotEmpty())

                val sourceMap = gson.fromJson(
                    asset(part.sourceMapPath).readText(Charsets.UTF_8),
                    V5PartSourceMap::class.java
                )
                assertEquals(part.id, sourceMap.partId)
                assertEquals(
                    "${part.id}: every H2 chapter must have exactly one evidence entry",
                    chapters.size,
                    sourceMap.sections.size
                )
                sourceMap.sections.forEachIndexed { chapterIndex, evidence ->
                    val prefix = "${part.id}-S${(chapterIndex + 1).toString().padStart(2, '0')}-"
                    assertTrue(
                        "${part.id}: evidence ids must be contiguous in authored chapter order: ${evidence.sectionId}",
                        evidence.sectionId.startsWith(prefix)
                    )
                    assertTrue("${evidence.sectionId}: evidence cannot be empty", evidence.sourceIds.isNotEmpty())
                    assertEquals(
                        "${evidence.sectionId}: duplicate source ids are not additional evidence",
                        evidence.sourceIds.distinct().size,
                        evidence.sourceIds.size
                    )
                    val unknown = evidence.sourceIds.filterNot(V5BookAllSources.byId::containsKey)
                    assertTrue("${evidence.sectionId}: unknown source ids $unknown", unknown.isEmpty())
                }
            }
        }
    }

    @Test
    fun manifestsCannotMentionMissingPartOrSourceFiles() {
        v5Root().listFiles()
            ?.filter { it.isDirectory && it.name.matches(Regex("track_\\d{2}")) }
            ?.forEach { trackDir ->
                val manifest = mergedManifest(trackDir)
                manifest.parts.forEach { part ->
                    assertTrue("Missing prose asset for ${part.id}: ${part.assetPath}", asset(part.assetPath).isFile)
                    assertTrue("Missing source map for ${part.id}: ${part.sourceMapPath}", asset(part.sourceMapPath).isFile)
                }
            }
    }
}
