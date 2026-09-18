package com.futuretech.poweruser.textbook

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Book-scale metadata. Learner text is read from the CodingCoding LIVE textbook path. */
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

/** Hard identity boundary for the CodingCoding textbook content source. */
data class V5ProjectLock(
    val projectId: String,
    val contentId: String,
    val schemaVersion: Int,
    val repository: String,
    val branch: String,
    val contentRoot: String,
    val trackNumbers: List<Int>
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
    data class Updated(val signature: String, val changedFiles: Int) : V5RemoteRefreshResult
    data class Skipped(val reason: String) : V5RemoteRefreshResult
}

/**
 * CodingCoding single-source textbook repository.
 *
 * Runtime learner content is isolated to:
 *   irewon1-lgtm/Myapp / codingcoding-project-textbook-live / app/src/main/assets/textbook/v5/
 *
 * A project_lock.json identity check is mandatory before a TRACK is accepted. This prevents
 * another chat, recovery branch, preview branch, experiment, or legacy cache from being displayed.
 *
 * The APK carries a same-project bundled copy so the book can open offline on first launch.
 * Network refreshes are staged in a temporary directory and replace the cache only after every
 * file has downloaded successfully. A failed refresh therefore never destroys the last good book.
 */
class V5BookAssetRepository(
    private val context: Context,
    private val gson: Gson = Gson()
) {
    private data class RemoteFile(
        val name: String,
        val path: String,
        val sha: String
    )

    fun loadManifest(trackNumber: Int): V5BookManifest {
        // Never wait for network to open the book. The exact project-bundled TRACK is seeded first.
        if (isConfiguredLiveTrack(trackNumber)) {
            seedBundledTrackIfMissing(trackNumber)
        }
        val dir = trackCacheDir(trackNumber)
        val names = dir.list().orEmpty()
            .filter { it == "manifest.json" || it.matches(MANIFEST_SHARD_REGEX) }
            .sortedWith(compareBy<String> { if (it == "manifest.json") 0 else 1 }.thenBy { it })

        require(names.firstOrNull() == "manifest.json") {
            "CodingCoding LIVE manifest is not cached for TRACK $trackNumber"
        }

        val fragments = names.map { name ->
            File(dir, name).bufferedReader(Charsets.UTF_8).use { reader ->
                gson.fromJson(reader, V5BookManifest::class.java)
            }
        }
        val base = fragments.first()
        require(base.trackNumber == trackNumber) {
            "Manifest track mismatch: expected=$trackNumber actual=${base.trackNumber}"
        }
        fragments.forEach { fragment ->
            require(fragment.trackNumber == base.trackNumber)
            require(fragment.trackId == base.trackId)
            require(fragment.title == base.title)
        }
        val merged = base.copy(parts = fragments.flatMap { it.parts }.sortedBy { it.order })
        require(merged.parts.map { it.order } == (1..merged.parts.size).toList()) {
            "Part order is not contiguous for TRACK $trackNumber"
        }
        require(merged.parts.map { it.id }.distinct().size == merged.parts.size) {
            "Duplicate part ids for TRACK $trackNumber"
        }
        return merged
    }

    fun loadPart(part: V5BookPartRef): List<TextbookBlock> {
        val markdown = liveFile(part.assetPath).readText(Charsets.UTF_8)
        return TextbookMarkdownParser.parse(markdown)
    }

    fun loadSourceMap(part: V5BookPartRef): V5PartSourceMap {
        return liveFile(part.sourceMapPath).bufferedReader(Charsets.UTF_8).use { reader ->
            gson.fromJson(reader, V5PartSourceMap::class.java)
        }
    }

    /**
     * Makes the APK-bundled CodingCoding textbook readable immediately.
     *
     * This never waits for GitHub/network. Only TRACKs explicitly allowed by the bundled project
     * lock are seeded. The reader calls this first, renders the local book, then refreshes LIVE in
     * the background.
     */
    fun isConfiguredLiveTrack(trackNumber: Int): Boolean {
        val projectLock = loadBundledProjectLock() ?: return false
        if (validateProjectLock(projectLock) != null) return false
        return trackNumber in projectLock.trackNumbers
    }

    suspend fun prepareBundledContent(trackNumber: Int): Boolean =
        withContext(Dispatchers.IO) {
            if (!isConfiguredLiveTrack(trackNumber)) return@withContext false
            if (!seedBundledTrackIfMissing(trackNumber)) return@withContext false
            runCatching { loadManifest(trackNumber) }.isSuccess
        }

    suspend fun refreshRemoteContent(trackNumber: Int): V5RemoteRefreshResult =
        withContext(Dispatchers.IO) {
            synchronized(REFRESH_LOCK) {
                refreshRemoteContentBlocking(trackNumber)
            }
        }

    internal fun liveSignature(trackNumber: Int): String? =
        prefs().getString(signatureKey(trackNumber), null)

    private fun refreshRemoteContentBlocking(trackNumber: Int): V5RemoteRefreshResult {
        require(trackNumber in 1..11) { "trackNumber out of range: $trackNumber" }

        val projectLock = fetchProjectLock() ?: loadBundledProjectLock()
            ?: return V5RemoteRefreshResult.Skipped("project_lock_unavailable")
        validateProjectLock(projectLock)?.let { reason ->
            return V5RemoteRefreshResult.Skipped(reason)
        }
        if (trackNumber !in projectLock.trackNumbers) {
            return V5RemoteRefreshResult.Skipped("track_not_live:$trackNumber")
        }

        seedBundledTrackIfMissing(trackNumber)

        val listing = fetchText(liveDirectoryUrl(trackNumber))
            ?: return V5RemoteRefreshResult.Skipped("live_directory_unavailable")

        val remoteFiles = parseDirectoryListing(listing, trackNumber)
        if (remoteFiles.none { it.name == "manifest.json" }) {
            return V5RemoteRefreshResult.Skipped("live_manifest_missing")
        }

        val signature = signature(remoteFiles)
        val dir = trackCacheDir(trackNumber)
        if (liveSignature(trackNumber) == signature && File(dir, "manifest.json").isFile) {
            return V5RemoteRefreshResult.UpToDate
        }

        val staging = File(
            cacheRoot(),
            liveTrackAssetPath(trackNumber) + ".__next"
        )
        staging.deleteRecursively()
        staging.mkdirs()

        var downloaded = 0
        for (remote in remoteFiles) {
            val body = fetchText(rawUrl(remote.path))
                ?: run {
                    staging.deleteRecursively()
                    return V5RemoteRefreshResult.Skipped("live_file_unavailable:${remote.name}")
                }
            File(staging, remote.name).writeText(body, Charsets.UTF_8)
            downloaded += 1
        }

        val stagedManifest = File(staging, "manifest.json")
        if (!stagedManifest.isFile) {
            staging.deleteRecursively()
            return V5RemoteRefreshResult.Skipped("staged_manifest_missing")
        }

        dir.deleteRecursively()
        dir.parentFile?.mkdirs()
        if (!staging.renameTo(dir)) {
            dir.mkdirs()
            staging.copyRecursively(dir, overwrite = true)
            staging.deleteRecursively()
        }

        prefs().edit().putString(signatureKey(trackNumber), signature).apply()
        return V5RemoteRefreshResult.Updated(signature, downloaded)
    }

    private fun seedBundledTrackIfMissing(trackNumber: Int): Boolean {
        val dir = trackCacheDir(trackNumber)
        if (File(dir, "manifest.json").isFile) return true

        val assetDir = liveTrackAssetPath(trackNumber)
        val names = context.assets.list(assetDir).orEmpty()
            .filter { it.endsWith(".md") || it.endsWith(".json") }
        if ("manifest.json" !in names) return false

        val staging = File(cacheRoot(), assetDir + ".__bundle")
        staging.deleteRecursively()
        staging.mkdirs()

        return runCatching {
            names.forEach { name ->
                context.assets.open("$assetDir/$name").use { input ->
                    File(staging, name).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            dir.deleteRecursively()
            dir.parentFile?.mkdirs()
            if (!staging.renameTo(dir)) {
                dir.mkdirs()
                staging.copyRecursively(dir, overwrite = true)
                staging.deleteRecursively()
            }
            File(dir, "manifest.json").isFile
        }.getOrElse {
            staging.deleteRecursively()
            false
        }
    }

    private fun fetchProjectLock(): V5ProjectLock? {
        val body = fetchText(rawUrl(PROJECT_LOCK_REPO_PATH)) ?: return null
        return runCatching {
            gson.fromJson(body, V5ProjectLock::class.java)
        }.getOrNull()
    }

    private fun loadBundledProjectLock(): V5ProjectLock? =
        runCatching {
            context.assets.open(PROJECT_LOCK_ASSET_PATH).bufferedReader(Charsets.UTF_8).use { reader ->
                gson.fromJson(reader, V5ProjectLock::class.java)
            }
        }.getOrNull()

    private fun validateProjectLock(lock: V5ProjectLock): String? {
        val allowedTracks = runCatching { lock.trackNumbers.toSet() }.getOrNull()
            ?: return "project_lock_invalid_tracks"
        return when {
            lock.projectId != EXPECTED_PROJECT_ID -> "project_lock_project_mismatch"
            lock.contentId != EXPECTED_CONTENT_ID -> "project_lock_content_mismatch"
            lock.schemaVersion != EXPECTED_SCHEMA_VERSION -> "project_lock_schema_mismatch"
            lock.repository != EXPECTED_REPOSITORY -> "project_lock_repository_mismatch"
            lock.branch != LIVE_BRANCH -> "project_lock_branch_mismatch"
            lock.contentRoot != LIVE_TRACK_ROOT -> "project_lock_root_mismatch"
            allowedTracks.isEmpty() -> "project_lock_track_set_empty"
            allowedTracks.any { it !in 1..11 } -> "project_lock_track_out_of_range"
            allowedTracks.size != lock.trackNumbers.size -> "project_lock_duplicate_track"
            else -> null
        }
    }

    private fun parseDirectoryListing(body: String, trackNumber: Int): List<RemoteFile> {
        val array = JSONArray(body)
        val prefix = "${REPO_ASSET_PREFIX}${liveTrackAssetPath(trackNumber)}/"
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                if (item.optString("type") != "file") continue
                val name = item.optString("name").trim()
                val path = item.optString("path").trim()
                val sha = item.optString("sha").trim()
                if (!path.startsWith(prefix) || name.isBlank() || sha.isBlank()) continue
                if (!name.endsWith(".md") && !name.endsWith(".json")) continue
                add(RemoteFile(name = name, path = path, sha = sha))
            }
        }.sortedBy { it.path }
    }

    private fun signature(files: List<RemoteFile>): String {
        val payload = files.joinToString("\n") { "${it.path}\t${it.sha}" }
        return MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }
    }

    private fun fetchText(url: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7_000
                readTimeout = 20_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/vnd.github+json,text/plain,*/*")
                setRequestProperty("User-Agent", "CodingCoding-Textbook-Live/1")
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Throwable) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun liveDirectoryUrl(trackNumber: Int): String =
        "$CONTENTS_API/${liveTrackRepoPath(trackNumber)}?ref=$LIVE_BRANCH"

    private fun rawUrl(repoPath: String): String =
        "$RAW_BASE/$LIVE_BRANCH/$repoPath"

    private fun liveFile(assetPath: String): File {
        require(assetPath.startsWith("textbook/v5/track_")) { "Not a CodingCoding LIVE path: $assetPath" }
        val file = File(cacheRoot(), assetPath)
        require(file.isFile) { "CodingCoding LIVE textbook file is not cached: $assetPath" }
        return file
    }

    private fun trackCacheDir(trackNumber: Int): File =
        File(cacheRoot(), liveTrackAssetPath(trackNumber)).apply { mkdirs() }

    private fun cacheRoot(): File =
        File(context.filesDir, CACHE_ROOT).apply { mkdirs() }

    private fun prefs() =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun signatureKey(trackNumber: Int): String =
        "track_${trackNumber.toString().padStart(2, '0')}_signature"

    companion object {
        internal const val LIVE_BRANCH = "codingcoding-project-textbook-live"
        internal const val LIVE_TRACK_ROOT = "app/src/main/assets/textbook/v5"
        internal const val EXPECTED_PROJECT_ID = "codingcoding"
        internal const val EXPECTED_CONTENT_ID = "codingcoding-textbook-v5"
        internal const val EXPECTED_SCHEMA_VERSION = 1
        internal const val EXPECTED_REPOSITORY = "irewon1-lgtm/Myapp"
        internal const val PROJECT_LOCK_REPO_PATH =
            "app/src/main/assets/textbook/v5/project_lock.json"

        private const val PROJECT_LOCK_ASSET_PATH = "textbook/v5/project_lock.json"
        private const val REPO_ASSET_PREFIX = "app/src/main/assets/"
        private const val RAW_BASE = "https://raw.githubusercontent.com/irewon1-lgtm/Myapp"
        private const val CONTENTS_API =
            "https://api.github.com/repos/irewon1-lgtm/Myapp/contents"
        private const val CACHE_ROOT = "codingcoding_project_textbook_live_v1"
        private const val PREFS_NAME = "codingcoding_project_textbook_live_content_v1"
        private val REFRESH_LOCK = Any()
        private val MANIFEST_SHARD_REGEX = Regex("manifest_\\d{2}\\.json")

        internal fun liveTrackAssetPath(trackNumber: Int): String {
            require(trackNumber in 1..11)
            return "textbook/v5/track_${trackNumber.toString().padStart(2, '0')}"
        }

        internal fun liveTrackRepoPath(trackNumber: Int): String =
            "$REPO_ASSET_PREFIX${liveTrackAssetPath(trackNumber)}"
    }
}
