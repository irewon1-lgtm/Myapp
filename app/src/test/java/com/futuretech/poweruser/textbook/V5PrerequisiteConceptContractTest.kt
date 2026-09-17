package com.futuretech.poweruser.textbook

import com.google.gson.Gson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fails closed on prerequisite typos without making the runtime reader eagerly load every source map.
 * All manifest shards are merged in reader order; every prerequisite must name an actual evidence
 * section from an earlier PART in the same TRACK. This keeps the book's prerequisite chain both
 * machine-checkable and compatible with current-PART-only runtime loading.
 */
class V5PrerequisiteConceptContractTest {
    private val gson = Gson()

    private fun assetsRoot(): File {
        val candidates = listOf(File("src/main/assets"), File("app/src/main/assets"))
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Cannot find assets root; cwd=${File(".").absolutePath}")
    }

    private fun shards(trackDir: File): List<File> = trackDir.listFiles().orEmpty()
        .filter { it.isFile && (it.name == "manifest.json" || it.name.matches(Regex("manifest_\\d{2}\\.json"))) }
        .sortedWith(compareBy<File> { if (it.name == "manifest.json") 0 else 1 }.thenBy { it.name })

    @Test
    fun everyPrerequisiteNamesARealEarlierSection() {
        val assets = assetsRoot()
        val v5Root = File(assets, "textbook/v5")
        val trackDirs = v5Root.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.matches(Regex("track_\\d{2}")) }
            .sortedBy { it.name }

        assertTrue("At least one V5 track must exist", trackDirs.isNotEmpty())

        trackDirs.forEach { trackDir ->
            val manifestFiles = shards(trackDir)
            assertTrue("${trackDir.name} needs manifest.json", manifestFiles.firstOrNull()?.name == "manifest.json")
            val fragments = manifestFiles.map { file ->
                gson.fromJson(file.readText(Charsets.UTF_8), V5BookManifest::class.java)
            }
            val base = fragments.first()
            fragments.forEach { fragment ->
                assertEquals(base.trackId, fragment.trackId)
                assertEquals(base.trackNumber, fragment.trackNumber)
                assertEquals(base.title, fragment.title)
            }
            val parts = fragments.flatMap { it.parts }.sortedBy { it.order }
            assertEquals((1..parts.size).toList(), parts.map { it.order })

            val sectionsByPart = linkedMapOf<String, Set<String>>()
            parts.forEach { part ->
                val sourceFile = File(assets, part.sourceMapPath)
                assertTrue("Missing source map for ${part.id}: ${part.sourceMapPath}", sourceFile.isFile)
                val sourceMap = gson.fromJson(sourceFile.readText(Charsets.UTF_8), V5PartSourceMap::class.java)
                assertEquals(part.id, sourceMap.partId)
                sectionsByPart[part.id] = sourceMap.sections.map { it.sectionId }.toSet()
            }

            parts.forEach { part ->
                assertEquals(
                    "${part.id}: duplicate prerequisite IDs do not add knowledge",
                    part.prerequisiteConceptIds.distinct().size,
                    part.prerequisiteConceptIds.size
                )
                part.prerequisiteConceptIds.forEach { prerequisite ->
                    val match = Regex("^(T\\d{2}-P(\\d{2,})-S\\d{2,}-.+)$").matchEntire(prerequisite)
                    assertTrue("${part.id}: malformed prerequisite id: $prerequisite", match != null)
                    val prerequisitePartNumber = match!!.groupValues[2].toInt()
                    assertTrue(
                        "${part.id}: prerequisite must come from an earlier PART: $prerequisite",
                        prerequisitePartNumber < part.order
                    )
                    val prerequisitePartId = prerequisite.substringBefore("-S")
                    val knownSections = sectionsByPart[prerequisitePartId]
                    assertTrue(
                        "${part.id}: prerequisite references missing PART: $prerequisitePartId",
                        knownSections != null
                    )
                    assertTrue(
                        "${part.id}: prerequisite references nonexistent section: $prerequisite",
                        knownSections!!.contains(prerequisite)
                    )
                }
            }
        }
    }
}
