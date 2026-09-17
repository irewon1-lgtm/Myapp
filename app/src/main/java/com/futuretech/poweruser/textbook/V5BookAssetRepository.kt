package com.futuretech.poweruser.textbook

import android.content.Context
import com.google.gson.Gson

/** Book-scale metadata. Large authored text stays in part assets, never Kotlin string constants. */
data class V5BookManifest(
    val trackId: String,
    val trackNumber: Int,
    val title: String,
    val parts: List<V5BookPartRef>
)

data class V5BookPartRef(
    val id: String,
    val order: Int,
    val title: String,
    val assetPath: String,
    val sourceMapPath: String,
    val prerequisiteConceptIds: List<String> = emptyList()
)

data class V5PartSourceMap(
    val partId: String,
    val sections: List<V5SectionEvidence>
)

data class V5SectionEvidence(
    val sectionId: String,
    val sourceIds: List<String>
)

/**
 * Loads exactly one V5 part at a time.
 *
 * Large books never become one track-sized String. Manifest metadata itself is also shardable:
 * manifest.json is the immutable spine and manifest_02.json, manifest_03.json, ... extend it.
 * Every shard must describe the same track/title; all parts are merged and then validated as one
 * contiguous P01..Pn sequence. This keeps book growth from repeatedly rewriting a giant manifest.
 *
 * A source map is validated independently for the current part and is bound one-to-one, in order,
 * to every learner-facing H2 chapter. Authored V5 content may use either the original `CHAPTER nn`
 * label or a numbered `nn.` H2 label. Both forms keep evidence coverage fail-closed while allowing
 * the newer textbook heading style to render through the same reader.
 */
class V5BookAssetRepository(
    private val context: Context,
    private val gson: Gson = Gson()
) {
    fun loadManifest(trackNumber: Int): V5BookManifest {
        require(trackNumber in 1..11) { "trackNumber out of range: $trackNumber" }
        val trackDir = "textbook/v5/track_${trackNumber.toString().padStart(2, '0')}"
        val names = context.assets.list(trackDir).orEmpty()
            .filter { it == "manifest.json" || it.matches(Regex("manifest_\\d{2}\\.json")) }
            .sortedWith(compareBy<String> { if (it == "manifest.json") 0 else 1 }.thenBy { it })

        require(names.firstOrNull() == "manifest.json") {
            "Missing primary V5 manifest for track $trackNumber: found=$names"
        }

        val fragments = names.map { name ->
            val path = "$trackDir/$name"
            context.assets.open(path).bufferedReader().use { reader ->
                gson.fromJson(reader, V5BookManifest::class.java)
            }
        }
        val base = fragments.first()
        fragments.forEachIndexed { index, fragment ->
            require(fragment.trackNumber == base.trackNumber) {
                "Manifest shard track number mismatch at ${names[index]}: base=${base.trackNumber} actual=${fragment.trackNumber}"
            }
            require(fragment.trackId == base.trackId) {
                "Manifest shard track id mismatch at ${names[index]}: base=${base.trackId} actual=${fragment.trackId}"
            }
            require(fragment.title == base.title) {
                "Manifest shard title mismatch at ${names[index]}: base=${base.title} actual=${fragment.title}"
            }
            if (index > 0) require(fragment.parts.isNotEmpty()) {
                "Manifest shard must not be empty: ${names[index]}"
            }
        }

        val merged = base.copy(parts = fragments.flatMap { it.parts }.sortedBy { it.order })
        validateManifest(merged, trackNumber)
        return merged
    }

    fun loadPart(part: V5BookPartRef): List<TextbookBlock> {
        validateRelativeAssetPath(part.assetPath)
        val markdown = context.assets.open(part.assetPath).bufferedReader().use { it.readText() }
        return TextbookMarkdownParser.parse(markdown)
    }

    fun loadSourceMap(part: V5BookPartRef): V5PartSourceMap {
        validateRelativeAssetPath(part.sourceMapPath)
        validateRelativeAssetPath(part.assetPath)
        val map = context.assets.open(part.sourceMapPath).bufferedReader().use { reader ->
            gson.fromJson(reader, V5PartSourceMap::class.java)
        }
        require(map.partId == part.id) {
            "Source map part id mismatch: expected=${part.id} actual=${map.partId}"
        }
        require(map.sections.map { it.sectionId }.distinct().size == map.sections.size) {
            "Duplicate source-map section ids in ${part.id}"
        }

        val markdown = context.assets.open(part.assetPath).bufferedReader().use { it.readText() }
        val chapterHeadings = TextbookMarkdownParser.parse(markdown)
            .filterIsInstance<TextbookBlock.Heading>()
            .filter { it.level == 2 && isLearnerChapterHeading(it.text) }

        require(chapterHeadings.isNotEmpty()) { "${part.id} has no learner-facing H2 chapter headings" }
        require(map.sections.size == chapterHeadings.size) {
            "${part.id} evidence coverage mismatch: chapters=${chapterHeadings.size} evidence=${map.sections.size}"
        }

        map.sections.forEachIndexed { index, evidence ->
            val expectedPrefix = "${part.id}-S${(index + 1).toString().padStart(2, '0')}-"
            require(evidence.sectionId.startsWith(expectedPrefix)) {
                "${part.id} evidence order/gap mismatch at chapter ${index + 1}: expectedPrefix=$expectedPrefix actual=${evidence.sectionId}"
            }
            require(evidence.sourceIds.isNotEmpty()) {
                "Section ${evidence.sectionId} has no evidence source"
            }
            require(evidence.sourceIds.distinct().size == evidence.sourceIds.size) {
                "Section ${evidence.sectionId} repeats the same source id"
            }
            val unknown = evidence.sourceIds.filterNot(V5BookAllSources.byId::containsKey)
            require(unknown.isEmpty()) {
                "Section ${evidence.sectionId} has unknown source ids: $unknown"
            }
        }
        return map
    }

    private fun isLearnerChapterHeading(text: String): Boolean =
        text.startsWith("CHAPTER ") || text.matches(Regex("^\\d+\\.\\s+.+"))

    private fun validateManifest(manifest: V5BookManifest, expectedTrackNumber: Int) {
        require(manifest.trackNumber == expectedTrackNumber) {
            "Manifest track mismatch: expected=$expectedTrackNumber actual=${manifest.trackNumber}"
        }
        val expectedTrackId = "T${expectedTrackNumber.toString().padStart(2, '0')}"
        require(manifest.trackId == expectedTrackId) {
            "Manifest id mismatch: expected=$expectedTrackId actual=${manifest.trackId}"
        }
        require(manifest.title.isNotBlank()) { "Manifest title is blank for $expectedTrackId" }
        require(manifest.parts.size >= 2) { "$expectedTrackId must be a multipart book" }
        require(manifest.parts.map { it.id }.distinct().size == manifest.parts.size) {
            "Duplicate part ids in $expectedTrackId"
        }
        require(manifest.parts.map { it.assetPath }.distinct().size == manifest.parts.size) {
            "Duplicate part asset paths in $expectedTrackId"
        }
        require(manifest.parts.map { it.sourceMapPath }.distinct().size == manifest.parts.size) {
            "Duplicate source-map paths in $expectedTrackId"
        }
        val orders = manifest.parts.map { it.order }.sorted()
        require(orders == (1..manifest.parts.size).toList()) {
            "Part orders must be contiguous in $expectedTrackId: $orders"
        }
        manifest.parts.forEachIndexed { index, part ->
            val expectedPartPrefix = "$expectedTrackId-P${(index + 1).toString().padStart(2, '0')}"
            require(part.id == expectedPartPrefix) {
                "Part ids must be contiguous in $expectedTrackId: expected=$expectedPartPrefix actual=${part.id}"
            }
            require(part.order == index + 1) {
                "Part order does not match list order in $expectedTrackId: ${part.id} order=${part.order}"
            }
            require(part.title.isNotBlank()) { "Blank part title: ${part.id}" }
            validateRelativeAssetPath(part.assetPath)
            validateRelativeAssetPath(part.sourceMapPath)
            require(part.assetPath.startsWith("textbook/v5/track_${expectedTrackNumber.toString().padStart(2, '0')}/")) {
                "Part path escapes its track: ${part.assetPath}"
            }
            require(part.sourceMapPath.startsWith("textbook/v5/track_${expectedTrackNumber.toString().padStart(2, '0')}/")) {
                "Source-map path escapes its track: ${part.sourceMapPath}"
            }
        }
    }

    private fun validateRelativeAssetPath(path: String) {
        require(path.isNotBlank()) { "Asset path is blank" }
        require(!path.startsWith('/')) { "Asset path must be relative: $path" }
        require(".." !in path.split('/')) { "Asset path traversal is forbidden: $path" }
        require('\\' !in path) { "Asset path must use forward slashes: $path" }
    }
}
