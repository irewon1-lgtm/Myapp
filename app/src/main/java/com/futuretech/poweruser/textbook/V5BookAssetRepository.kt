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
 * Large books never become one track-sized String. A source map is validated independently for the
 * current part and is bound one-to-one, in order, to every learner-facing H2 CHAPTER. This prevents
 * an author from dropping a token source-map file beside prose while leaving whole chapters without
 * evidence coverage.
 */
class V5BookAssetRepository(
    private val context: Context,
    private val gson: Gson = Gson()
) {
    fun loadManifest(trackNumber: Int): V5BookManifest {
        require(trackNumber in 1..11) { "trackNumber out of range: $trackNumber" }
        val path = "textbook/v5/track_${trackNumber.toString().padStart(2, '0')}/manifest.json"
        val manifest = context.assets.open(path).bufferedReader().use { reader ->
            gson.fromJson(reader, V5BookManifest::class.java)
        }
        validateManifest(manifest, trackNumber)
        return manifest
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
            .filter { it.level == 2 && it.text.startsWith("CHAPTER ") }

        require(chapterHeadings.isNotEmpty()) { "${part.id} has no learner-facing H2 CHAPTER headings" }
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
