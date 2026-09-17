package com.futuretech.poweruser.textbook

import com.google.gson.Gson
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates every V5 part which has actually been authored so a sidecar file cannot merely exist
 * while bearing no relationship to the visible book structure. Full 11-track scale is enforced by
 * V5BookScaleCorpusGateTest; this test is intentionally useful while the corpus is still RED.
 */
class V5ExistingPartEvidenceContractTest {
    private val gson = Gson()

    private fun assetsRoot(): File {
        val candidates = listOf(
            File("src/main/assets/textbook/v5"),
            File("app/src/main/assets/textbook/v5")
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Cannot find V5 asset root; cwd=${File(".").absolutePath}")
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
        }
        return base.copy(parts = fragments.flatMap { it.parts }.sortedBy { it.order })
    }

    @Test
    fun everyAuthoredPartHasOrderedEvidenceForEveryChapter() {
        val root = assetsRoot()
        val trackDirs = root.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.matches(Regex("track_\\d{2}")) }
            .sortedBy { it.name }
        assertTrue("At least one V5 track must exist", trackDirs.isNotEmpty())

        trackDirs.forEach { trackDir ->
            val manifest = mergedManifest(trackDir)
            assertTrue(manifest.parts.size >= 2)
            assertEquals((1..manifest.parts.size).toList(), manifest.parts.map { it.order })
            assertEquals(manifest.parts.size, manifest.parts.map { it.id }.distinct().size)

            manifest.parts.forEach { part ->
                val markdownFile = File(root.parentFile!!.parentFile!!, part.assetPath)
                val sourceMapFile = File(root.parentFile!!.parentFile!!, part.sourceMapPath)
                assertTrue("Missing authored part ${part.assetPath}", markdownFile.isFile)
                assertTrue("Missing source map ${part.sourceMapPath}", sourceMapFile.isFile)

                val markdown = markdownFile.readText(Charsets.UTF_8)
                val chapterHeadings = Regex("(?m)^## CHAPTER \\d+ · .+$")
                    .findAll(markdown)
                    .map { it.value.removePrefix("## ").trim() }
                    .toList()
                assertTrue("Part ${part.id} needs real chapters", chapterHeadings.size >= 2)

                val evidence = gson.fromJson(sourceMapFile.readText(Charsets.UTF_8), V5PartSourceMap::class.java)
                assertEquals(part.id, evidence.partId)
                assertEquals(
                    "Evidence count must equal visible chapter count for ${part.id}",
                    chapterHeadings.size,
                    evidence.sections.size
                )

                evidence.sections.forEachIndexed { index, section ->
                    val expectedPrefix = "${part.id}-S${(index + 1).toString().padStart(2, '0')}-"
                    assertTrue(
                        "Evidence rows must follow chapter order: ${section.sectionId} expected prefix $expectedPrefix",
                        section.sectionId.startsWith(expectedPrefix)
                    )
                    assertTrue("No chapter may be source-less: ${section.sectionId}", section.sourceIds.isNotEmpty())
                    val unknown = section.sourceIds.filterNot(V5BookAllSources.byId::containsKey)
                    assertTrue("Unknown sources in ${section.sectionId}: $unknown", unknown.isEmpty())
                }
            }
        }
    }

    @Test
    fun learnerTextDoesNotContainInternalSourceIdsOrRawCitationPlumbing() {
        val root = assetsRoot()
        val badTokens = listOf("CSAPP3", "OSTEP", "TLPI", "sourceId", "sourceIds", "V5BookSourceRegistry")
        val offenders = mutableListOf<String>()
        root.walkTopDown()
            .filter { it.isFile && it.extension.equals("md", ignoreCase = true) }
            .forEach { file ->
                val text = file.readText(Charsets.UTF_8)
                badTokens.forEach { token ->
                    if (token in text) offenders += "${file.name}:$token"
                }
            }
        assertTrue("Internal citation plumbing leaked into learner prose: $offenders", offenders.isEmpty())
    }
}
