package com.futuretech.poweruser.textbook

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

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

sealed interface V5RemoteRefreshResult {
    data object UpToDate : V5RemoteRefreshResult
    data class Updated(val revision: String, val changedFiles: Int) : V5RemoteRefreshResult
    data class RolledBack(val revision: String) : V5RemoteRefreshResult
    data class Skipped(val reason: String) : V5RemoteRefreshResult
}

/**
 * Hybrid V5 textbook repository.
 *
 * Read order is active verified overlay -> previous verified overlay -> APK assets. The APK therefore
 * remains a complete offline textbook. A lightweight public GitHub channel can move the active
 * content revision without rebuilding the APK; only changed textbook files are downloaded.
 *
 * Remote updates are transactional. Files are first applied to a staging overlay, every affected
 * track is parsed and evidence-validated against that overlay, and only then is staging promoted.
 * A failed download, malformed manifest, malformed source map, or incomplete PART leaves the current
 * active snapshot untouched. One previous verified snapshot is retained for instant rollback.
 */
class V5BookAssetRepository(
    private val context: Context,
    private val gson: Gson = Gson()
) {
    init {
        ensureCacheBaseline()
    }

    private data class Snapshot(
        val overlayRoot: File?,
        val tombstones: Set<String>
    )

    fun loadManifest(trackNumber: Int): V5BookManifest {
        require(trackNumber in 1..11) { "trackNumber out of range: $trackNumber" }
        snapshotCandidates().forEach { snapshot ->
            loadManifestFromSnapshot(trackNumber, snapshot)?.let { return it }
        }
        error("No valid V5 manifest available for track $trackNumber")
    }

    fun loadPart(part: V5BookPartRef): List<TextbookBlock> {
        validateRelativeAssetPath(part.assetPath)
        validateRelativeAssetPath(part.sourceMapPath)
        snapshotCandidates().forEach { snapshot ->
            loadValidatedPart(snapshot, part)?.let { return it.second }
        }
        error("No valid V5 part available: ${part.id}")
    }

    fun loadSourceMap(part: V5BookPartRef): V5PartSourceMap {
        validateRelativeAssetPath(part.sourceMapPath)
        validateRelativeAssetPath(part.assetPath)
        snapshotCandidates().forEach { snapshot ->
            loadValidatedPart(snapshot, part)?.let { return it.first }
        }
        error("No valid V5 source map available: ${part.id}")
    }

    /**
     * Checks the tiny channel file and, when it advances, downloads only V5 files changed since the
     * active verified revision. This function is safe to call at process start and never mutates APK
     * assets. It performs network and validation work on Dispatchers.IO.
     */
    suspend fun refreshRemoteContent(): V5RemoteRefreshResult = withContext(Dispatchers.IO) {
        synchronized(REFRESH_LOCK) {
            refreshRemoteContentBlocking()
        }
    }

    internal fun activeRevision(): String = prefs().getString(PREF_ACTIVE_REVISION, null)
        ?.takeIf(::isValidRevision)
        ?: BUNDLED_REVISION

    internal fun previousRevision(): String? = prefs().getString(PREF_PREVIOUS_REVISION, null)
        ?.takeIf(::isValidRevision)

    private fun refreshRemoteContentBlocking(): V5RemoteRefreshResult {
        val channelBody = fetchText(CHANNEL_URL, MAX_CHANNEL_BYTES)
            ?: return V5RemoteRefreshResult.Skipped("channel_unavailable")
        val targetRevision = parseChannelRevision(channelBody)
            ?: return V5RemoteRefreshResult.Skipped("channel_invalid")
        val currentRevision = activeRevision()

        if (targetRevision == currentRevision) return V5RemoteRefreshResult.UpToDate
        if (targetRevision == previousRevision()) {
            return if (swapActiveAndPrevious(targetRevision)) {
                V5RemoteRefreshResult.RolledBack(targetRevision)
            } else {
                V5RemoteRefreshResult.Skipped("rollback_swap_failed")
            }
        }

        val compareUrl = "$COMPARE_BASE/$currentRevision...$targetRevision"
        val compareBody = fetchText(compareUrl, MAX_COMPARE_BYTES)
            ?: return V5RemoteRefreshResult.Skipped("compare_unavailable")
        val comparison = parseComparison(compareBody)
            ?: return V5RemoteRefreshResult.Skipped("compare_invalid")
        if (comparison.status != "ahead" && comparison.status != "identical") {
            return V5RemoteRefreshResult.Skipped("revision_not_ahead:${comparison.status}")
        }
        if (comparison.truncated) {
            return V5RemoteRefreshResult.Skipped("too_many_changed_files")
        }

        val root = cacheRoot()
        val activeDir = File(root, ACTIVE_DIR)
        val previousDir = File(root, PREVIOUS_DIR)
        val stagingDir = File(root, STAGING_DIR)
        stagingDir.deleteRecursively()
        stagingDir.mkdirs()

        if (activeDir.exists() && !copyDirectory(activeDir, stagingDir)) {
            stagingDir.deleteRecursively()
            return V5RemoteRefreshResult.Skipped("staging_copy_failed")
        }

        val tombstones = readTombstones(stagingDir).toMutableSet()
        val touchedTracks = linkedSetOf<Int>()
        var changedV5Files = 0

        for (change in comparison.files) {
            val relativePath = change.filename.removePrefix(REPO_ASSET_PREFIX)
            if (relativePath == change.filename) continue
            if (!isSafeAssetPath(relativePath)) {
                stagingDir.deleteRecursively()
                return V5RemoteRefreshResult.Skipped("unsafe_path")
            }
            parseTrackNumber(relativePath)?.let(touchedTracks::add)

            if (change.status == "removed") {
                tombstones += relativePath
                File(stagingDir, relativePath).delete()
                changedV5Files += 1
                continue
            }

            if (change.status == "renamed") {
                change.previousFilename
                    ?.removePrefix(REPO_ASSET_PREFIX)
                    ?.takeIf { it != change.previousFilename && isSafeAssetPath(it) }
                    ?.let { previousPath ->
                        tombstones += previousPath
                        File(stagingDir, previousPath).delete()
                        parseTrackNumber(previousPath)?.let(touchedTracks::add)
                    }
            }

            if (change.status !in DOWNLOADABLE_STATUSES) continue
            val remoteText = fetchText(rawUrl(targetRevision, relativePath), MAX_CONTENT_FILE_BYTES)
                ?: run {
                    stagingDir.deleteRecursively()
                    return V5RemoteRefreshResult.Skipped("content_download_failed:$relativePath")
                }
            if (!writeOverlayFile(stagingDir, relativePath, remoteText)) {
                stagingDir.deleteRecursively()
                return V5RemoteRefreshResult.Skipped("content_write_failed:$relativePath")
            }
            tombstones.remove(relativePath)
            changedV5Files += 1
        }

        if (!writeTombstones(stagingDir, tombstones)) {
            stagingDir.deleteRecursively()
            return V5RemoteRefreshResult.Skipped("tombstone_write_failed")
        }

        val stagedSnapshot = Snapshot(stagingDir, tombstones)
        for (trackNumber in touchedTracks) {
            val manifest = loadManifestFromSnapshot(trackNumber, stagedSnapshot)
                ?: run {
                    stagingDir.deleteRecursively()
                    return V5RemoteRefreshResult.Skipped("track_manifest_invalid:$trackNumber")
                }
            for (part in manifest.parts) {
                if (loadValidatedPart(stagedSnapshot, part) == null) {
                    stagingDir.deleteRecursively()
                    return V5RemoteRefreshResult.Skipped("track_part_invalid:${part.id}")
                }
            }
        }

        if (!promoteStaging(activeDir, previousDir, stagingDir)) {
            stagingDir.deleteRecursively()
            return V5RemoteRefreshResult.Skipped("promotion_failed")
        }

        prefs().edit()
            .putString(PREF_PREVIOUS_REVISION, currentRevision)
            .putString(PREF_ACTIVE_REVISION, targetRevision)
            .apply()
        return V5RemoteRefreshResult.Updated(targetRevision, changedV5Files)
    }

    private fun loadManifestFromSnapshot(trackNumber: Int, snapshot: Snapshot): V5BookManifest? {
        return runCatching {
            val trackDir = "textbook/v5/track_${trackNumber.toString().padStart(2, '0')}"
            val names = listSnapshotFiles(snapshot, trackDir)
                .filter { it == "manifest.json" || it.matches(MANIFEST_SHARD_REGEX) }
                .sortedWith(compareBy<String> { if (it == "manifest.json") 0 else 1 }.thenBy { it })
            require(names.firstOrNull() == "manifest.json") {
                "Missing primary V5 manifest for track $trackNumber: found=$names"
            }
            val fragments = names.map { name ->
                val path = "$trackDir/$name"
                val json = requireNotNull(readSnapshotText(snapshot, path)) { "Missing $path" }
                gson.fromJson(json, V5BookManifest::class.java)
            }
            val merged = mergeManifestFragments(fragments, names)
            validateManifest(merged, trackNumber)
            merged
        }.getOrNull()
    }

    private fun mergeManifestFragments(
        fragments: List<V5BookManifest>,
        names: List<String>
    ): V5BookManifest {
        require(fragments.isNotEmpty()) { "No manifest fragments" }
        val base = fragments.first()
        fragments.forEachIndexed { index, fragment ->
            require(fragment.trackNumber == base.trackNumber) {
                "Manifest shard track number mismatch at ${names[index]}"
            }
            require(fragment.trackId == base.trackId) {
                "Manifest shard track id mismatch at ${names[index]}"
            }
            require(fragment.title == base.title) {
                "Manifest shard title mismatch at ${names[index]}"
            }
            if (index > 0) require(fragment.parts.isNotEmpty()) {
                "Manifest shard must not be empty: ${names[index]}"
            }
        }
        return base.copy(parts = fragments.flatMap { it.parts }.sortedBy { it.order })
    }

    private fun loadValidatedPart(
        snapshot: Snapshot,
        part: V5BookPartRef
    ): Pair<V5PartSourceMap, List<TextbookBlock>>? = runCatching {
        val sourceMapJson = requireNotNull(readSnapshotText(snapshot, part.sourceMapPath))
        val markdown = requireNotNull(readSnapshotText(snapshot, part.assetPath))
        validatePartContent(snapshot, part, sourceMapJson, markdown)
    }.getOrNull()

    private fun validatePartContent(
        snapshot: Snapshot,
        part: V5BookPartRef,
        sourceMapJson: String,
        markdown: String
    ): Pair<V5PartSourceMap, List<TextbookBlock>> {
        validateRelativeAssetPath(part.sourceMapPath)
        validateRelativeAssetPath(part.assetPath)
        val map = gson.fromJson(sourceMapJson, V5PartSourceMap::class.java)
        require(map.partId == part.id) {
            "Source map part id mismatch: expected=${part.id} actual=${map.partId}"
        }
        require(map.sections.map { it.sectionId }.distinct().size == map.sections.size) {
            "Duplicate source-map section ids in ${part.id}"
        }

        val blocks = TextbookMarkdownParser.parse(markdown)
        val chapterHeadings = blocks
            .filterIsInstance<TextbookBlock.Heading>()
            .filter { it.level == 2 && it.text.startsWith("CHAPTER ") }
        require(chapterHeadings.isNotEmpty()) { "${part.id} has no learner-facing H2 CHAPTER headings" }
        require(map.sections.size == chapterHeadings.size) {
            "${part.id} evidence coverage mismatch: chapters=${chapterHeadings.size} evidence=${map.sections.size}"
        }

        map.sections.forEachIndexed { index, evidence ->
            val expectedPrefix = "${part.id}-S${(index + 1).toString().padStart(2, '0')}-"
            require(evidence.sectionId.startsWith(expectedPrefix)) {
                "${part.id} evidence order/gap mismatch at chapter ${index + 1}"
            }
            require(evidence.sourceIds.isNotEmpty()) { "Section ${evidence.sectionId} has no evidence source" }
            require(evidence.sourceIds.distinct().size == evidence.sourceIds.size) {
                "Section ${evidence.sectionId} repeats the same source id"
            }
            val allowedSourceIds = allowedSourceIds(snapshot)
            val unknown = evidence.sourceIds.filterNot(allowedSourceIds::contains)
            require(unknown.isEmpty()) { "Section ${evidence.sectionId} has unknown source ids: $unknown" }
        }
        return map to blocks
    }

    private fun allowedSourceIds(snapshot: Snapshot): Set<String> {
        val ids = V5BookAllSources.byId.keys.toMutableSet()
        val extensionJson = readSnapshotText(snapshot, SOURCE_ID_EXTENSION_PATH) ?: return ids
        runCatching {
            val json = JSONObject(extensionJson)
            require(json.optInt("schemaVersion", -1) == 1)
            val array = json.optJSONArray("sourceIds") ?: return@runCatching
            for (index in 0 until array.length()) {
                val id = array.optString(index).trim()
                if (SOURCE_ID_REGEX.matches(id)) ids += id
            }
        }
        return ids
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
            val expectedPrefix = "textbook/v5/track_${expectedTrackNumber.toString().padStart(2, '0')}/"
            require(part.assetPath.startsWith(expectedPrefix)) { "Part path escapes its track: ${part.assetPath}" }
            require(part.sourceMapPath.startsWith(expectedPrefix)) { "Source-map path escapes its track: ${part.sourceMapPath}" }
        }
    }

    private fun snapshotCandidates(): List<Snapshot> {
        val root = cacheRoot()
        val activeDir = File(root, ACTIVE_DIR)
        val previousDir = File(root, PREVIOUS_DIR)
        val activeOverlay = activeDir.takeIf { activeRevision() != BUNDLED_REVISION && it.exists() }
        val previousOverlay = previousDir.takeIf { previousRevision() != BUNDLED_REVISION && it.exists() }
        val active = Snapshot(activeOverlay, activeOverlay?.let(::readTombstones).orEmpty())
        val previous = Snapshot(previousOverlay, previousOverlay?.let(::readTombstones).orEmpty())
        val bundled = Snapshot(null, emptySet())
        return listOf(active, previous, bundled).distinctBy { it.overlayRoot?.absolutePath ?: "bundled" }
    }

    private fun readSnapshotText(snapshot: Snapshot, path: String): String? {
        validateRelativeAssetPath(path)
        if (path in snapshot.tombstones) return null
        snapshot.overlayRoot?.let { root ->
            val file = File(root, path)
            if (file.isFile) return runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
        }
        return runCatching {
            context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull()
    }

    private fun listSnapshotFiles(snapshot: Snapshot, dir: String): List<String> {
        validateRelativeAssetPath(dir)
        val names = linkedSetOf<String>()
        context.assets.list(dir).orEmpty().forEach(names::add)
        snapshot.overlayRoot?.let { root ->
            File(root, dir).list()?.forEach(names::add)
        }
        snapshot.tombstones
            .filter { it.substringBeforeLast('/', "") == dir }
            .map { it.substringAfterLast('/') }
            .forEach(names::remove)
        return names.toList()
    }

    private data class RemoteFileChange(
        val filename: String,
        val status: String,
        val previousFilename: String?
    )

    private data class RemoteComparison(
        val status: String,
        val files: List<RemoteFileChange>,
        val truncated: Boolean
    )

    private fun parseChannelRevision(body: String): String? = runCatching {
        val json = JSONObject(body)
        require(json.optInt("schemaVersion", -1) == 1)
        json.optString("revision").trim().lowercase().takeIf(::isValidRevision)
    }.getOrNull()

    private fun parseComparison(body: String): RemoteComparison? = runCatching {
        val json = JSONObject(body)
        val status = json.optString("status").trim().lowercase()
        val filesJson = json.optJSONArray("files") ?: return@runCatching null
        val files = buildList {
            for (index in 0 until filesJson.length()) {
                val item = filesJson.optJSONObject(index) ?: continue
                val filename = item.optString("filename").trim()
                val fileStatus = item.optString("status").trim().lowercase()
                if (filename.isBlank() || fileStatus.isBlank()) continue
                add(
                    RemoteFileChange(
                        filename = filename,
                        status = fileStatus,
                        previousFilename = item.optString("previous_filename").trim().ifBlank { null }
                    )
                )
            }
        }
        RemoteComparison(
            status = status,
            files = files,
            truncated = filesJson.length() >= MAX_COMPARE_FILES
        )
    }.getOrNull()

    private fun rawUrl(revision: String, relativeAssetPath: String): String {
        require(isValidRevision(revision)) { "Invalid revision" }
        require(isSafeAssetPath(relativeAssetPath)) { "Invalid asset path" }
        return "$RAW_BASE/$revision/$REPO_ASSET_PREFIX$relativeAssetPath"
    }

    private fun fetchText(url: String, maxBytes: Int): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7_000
                readTimeout = 12_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json,text/plain,*/*")
                setRequestProperty("User-Agent", "Myapp-V5-Content/1")
                setRequestProperty("Cache-Control", "no-cache")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val declared = connection.contentLengthLong
            if (declared > maxBytes) return null
            val out = ByteArrayOutputStream(minOf(maxBytes, 64 * 1024))
            connection.inputStream.use { input ->
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > maxBytes) return null
                    out.write(buffer, 0, read)
                }
            }
            out.toString(Charsets.UTF_8.name())
        } catch (_: Throwable) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun writeOverlayFile(root: File, relativePath: String, content: String): Boolean = runCatching {
        require(isSafeAssetPath(relativePath))
        val file = File(root, relativePath)
        val canonicalRoot = root.canonicalFile
        val canonicalFile = file.canonicalFile
        require(canonicalFile.path.startsWith(canonicalRoot.path + File.separator))
        canonicalFile.parentFile?.mkdirs()
        val temp = File(canonicalFile.parentFile, ".${canonicalFile.name}.tmp")
        temp.writeText(content, Charsets.UTF_8)
        if (canonicalFile.exists() && !canonicalFile.delete()) error("replace_delete_failed")
        if (!temp.renameTo(canonicalFile)) error("replace_rename_failed")
        true
    }.getOrDefault(false)

    private fun copyDirectory(source: File, target: File): Boolean = runCatching {
        if (!source.exists()) return@runCatching true
        source.walkTopDown().forEach { src ->
            val relative = src.relativeTo(source)
            val dst = File(target, relative.path)
            if (src.isDirectory) dst.mkdirs() else {
                dst.parentFile?.mkdirs()
                src.copyTo(dst, overwrite = true)
            }
        }
        true
    }.getOrDefault(false)

    private fun promoteStaging(activeDir: File, previousDir: File, stagingDir: File): Boolean {
        val root = activeDir.parentFile ?: return false
        val oldActive = File(root, ".old-active")
        oldActive.deleteRecursively()
        return try {
            previousDir.deleteRecursively()
            if (activeDir.exists() && !activeDir.renameTo(oldActive)) return false
            if (!stagingDir.renameTo(activeDir)) {
                if (oldActive.exists()) oldActive.renameTo(activeDir)
                return false
            }
            if (oldActive.exists() && !oldActive.renameTo(previousDir)) {
                // Active is already valid. Losing rollback cache is preferable to reverting a verified update.
                oldActive.deleteRecursively()
            }
            true
        } catch (_: Throwable) {
            if (!activeDir.exists() && oldActive.exists()) oldActive.renameTo(activeDir)
            false
        }
    }

    private fun swapActiveAndPrevious(targetRevision: String): Boolean {
        if (previousRevision() != targetRevision) return false
        val root = cacheRoot()
        val activeDir = File(root, ACTIVE_DIR)
        val previousDir = File(root, PREVIOUS_DIR)
        val swapDir = File(root, ".swap")
        swapDir.deleteRecursively()
        return try {
            if (activeDir.exists() && !activeDir.renameTo(swapDir)) return false
            if (previousDir.exists() && !previousDir.renameTo(activeDir)) {
                if (swapDir.exists()) swapDir.renameTo(activeDir)
                return false
            }
            if (swapDir.exists() && !swapDir.renameTo(previousDir)) {
                swapDir.deleteRecursively()
            }
            val oldActive = activeRevision()
            prefs().edit()
                .putString(PREF_ACTIVE_REVISION, targetRevision)
                .putString(PREF_PREVIOUS_REVISION, oldActive)
                .apply()
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun readTombstones(root: File): Set<String> = runCatching {
        val file = File(root, TOMBSTONE_FILE)
        if (!file.isFile) return@runCatching emptySet()
        file.readLines(Charsets.UTF_8)
            .map(String::trim)
            .filter { it.isNotBlank() && isSafeAssetPath(it) }
            .toSet()
    }.getOrDefault(emptySet())

    private fun writeTombstones(root: File, tombstones: Set<String>): Boolean = runCatching {
        root.mkdirs()
        File(root, TOMBSTONE_FILE).writeText(tombstones.sorted().joinToString("\n"), Charsets.UTF_8)
        true
    }.getOrDefault(false)

    private fun parseTrackNumber(path: String): Int? = TRACK_PATH_REGEX.find(path)
        ?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..11 }

    private fun validateRelativeAssetPath(path: String) {
        require(isSafeAssetPath(path)) { "Unsafe asset path: $path" }
    }

    private fun ensureCacheBaseline() {
        val preferences = prefs()
        val storedBaseline = preferences.getString(PREF_BASELINE_REVISION, null)
        if (storedBaseline == BUNDLED_REVISION) return
        File(context.filesDir, CACHE_ROOT).deleteRecursively()
        preferences.edit()
            .clear()
            .putString(PREF_BASELINE_REVISION, BUNDLED_REVISION)
            .apply()
    }

    private fun cacheRoot(): File = File(context.filesDir, CACHE_ROOT).apply { mkdirs() }

    private fun prefs() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        // This is the V5 asset snapshot embedded in the first hybrid APK. Update this only when a
        // future APK intentionally ships a newer V5 baseline.
        internal const val BUNDLED_REVISION = "bf78170de204d02b2c37d6746b5c399659ac4207"
        private const val CHANNEL_BRANCH = "textbook-v5-deep-book-engine"
        private const val RAW_BASE = "https://raw.githubusercontent.com/irewon1-lgtm/Myapp"
        private const val REPO_ASSET_PREFIX = "app/src/main/assets/"
        private const val CHANNEL_URL =
            "$RAW_BASE/$CHANNEL_BRANCH/${REPO_ASSET_PREFIX}textbook/v5/channel.json"
        private const val COMPARE_BASE = "https://api.github.com/repos/irewon1-lgtm/Myapp/compare"

        private const val CACHE_ROOT = "remote_textbook_v5"
        private const val ACTIVE_DIR = "active"
        private const val PREVIOUS_DIR = "previous"
        private const val STAGING_DIR = "staging"
        private const val TOMBSTONE_FILE = ".tombstones"
        private const val SOURCE_ID_EXTENSION_PATH = "textbook/v5/source_ids.json"
        private const val PREFS_NAME = "v5_remote_content"
        private const val PREF_ACTIVE_REVISION = "active_revision"
        private const val PREF_PREVIOUS_REVISION = "previous_revision"
        private const val PREF_BASELINE_REVISION = "baseline_revision"

        private const val MAX_CHANNEL_BYTES = 16 * 1024
        private const val MAX_COMPARE_BYTES = 4 * 1024 * 1024
        private const val MAX_CONTENT_FILE_BYTES = 2 * 1024 * 1024
        private const val MAX_COMPARE_FILES = 300
        private val REFRESH_LOCK = Any()
        private val MANIFEST_SHARD_REGEX = Regex("manifest_\\d{2}\\.json")
        private val TRACK_PATH_REGEX = Regex("^textbook/v5/track_(\\d{2})/")
        private val REVISION_REGEX = Regex("^[0-9a-f]{40}$")
        private val SAFE_PATH_REGEX = Regex("^[A-Za-z0-9._/-]+$")
        private val SOURCE_ID_REGEX = Regex("^[A-Z0-9][A-Z0-9._:-]{0,79}$")
        private val DOWNLOADABLE_STATUSES = setOf("added", "modified", "changed", "renamed")

        internal fun isValidRevision(value: String): Boolean = REVISION_REGEX.matches(value)

        internal fun isSafeAssetPath(path: String): Boolean {
            if (path.isBlank() || path.startsWith('/') || '\\' in path) return false
            if (!SAFE_PATH_REGEX.matches(path)) return false
            if (path.split('/').any { it.isBlank() || it == ".." || it == "." }) return false
            return path.startsWith("textbook/v5/")
        }
    }
}
