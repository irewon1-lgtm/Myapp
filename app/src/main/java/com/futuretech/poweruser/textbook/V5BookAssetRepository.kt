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

/** Book-scale metadata. Learner text is read from the single live textbook path. */
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
 * Single-live-path V5 textbook repository.
 *
 * There is exactly one learner-content source:
 *   irewon1-lgtm/Myapp / codingcoding-textbook-live / app/src/main/assets/textbook/v5/
 *
 * A project_lock.json identity check is mandatory before any TRACK is accepted. This prevents
 * content from another chat, branch, project, or legacy cache from being displayed by mistake.
 *
 * No channel revision, compare window, previous snapshot, rollback snapshot, staging promotion,
 * bundled-content fallback, file-count gate, evidence gate, or source-registry gate is involved in
 * deciding what the learner sees. When a TRACK directory changes, the current files from that one
 * live path replace the device cache for that TRACK.
 *
 * The cache is only a transport cache for the current live files. It is never treated as a previous
 * version. If the live directory cannot be refreshed, the caller receives Skipped and can show the
 * connection error instead of silently displaying an older textbook.
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
        val dir = trackCacheDir(trackNumber)
        val names = dir.list().orEmpty()
            .filter { it == "manifest.json" || it.matches(MANIFEST_SHARD_REGEX) }
            .sortedWith(compareBy<String> { if (it == "manifest.json") 0 else 1 }.thenBy { it })

        require(names.firstOrNull() == "manifest.json") {
            "Live manifest is not cached for TRACK $trackNumber"
        }

        val fragments = names.map { name ->
            File(dir, name).bufferedReader(Charsets.UTF_8).use { reader ->
                gson.fromJson(reader, V5BookManifest::class.java)
            }
        }
        val base = fragments.first()
        return base.copy(parts = fragments.flatMap { it.parts }.sortedBy { it.order })
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
     * Refreshes one TRACK directly from the canonical live branch.
     *
     * Git blob SHAs from the directory listing are the change detector. A one-character edit changes
     * the file SHA, therefore the TRACK signature changes and the current TRACK is downloaded again.
     */
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

        val projectLock = fetchProjectLock()
            ?: return V5RemoteRefreshResult.Skipped("project_lock_unavailable")
        validateProjectLock(projectLock)?.let { reason ->
            return V5RemoteRefreshResult.Skipped(reason)
        }
        if (trackNumber !in projectLock.trackNumbers) {
            return V5RemoteRefreshResult.Skipped("track_not_allowed_by_project_lock:$trackNumber")
        }

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

        // Deliberately destructive: the live TRACK is the only version.
        dir.deleteRecursively()
        dir.mkdirs()

        var downloaded = 0
        for (remote in remoteFiles) {
            val body = fetchText(rawUrl(remote.path))
                ?: return V5RemoteRefreshResult.Skipped("live_file_unavailable:${remote.name}")
            val relative = remote.path.removePrefix(REPO_ASSET_PREFIX)
            val target = File(cacheRoot(), relative)
            target.parentFile?.mkdirs()
            target.writeText(body, Charsets.UTF_8)
            downloaded += 1
        }

        prefs().edit().putString(signatureKey(trackNumber), signature).apply()
        return V5RemoteRefreshResult.Updated(signature, downloaded)
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

    private fun fetchProjectLock(): V5ProjectLock? {
        val body = fetchText(rawUrl(PROJECT_LOCK_REPO_PATH)) ?: return null
        return runCatching {
            gson.fromJson(body, V5ProjectLock::class.java)
        }.getOrNull()
    }

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
            allowedTracks != (1..11).toSet() -> "project_lock_track_set_mismatch"
            else -> null
        }
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
        require(assetPath.startsWith("textbook/v5/track_")) { "Not a live TRACK path: $assetPath" }
        val file = File(cacheRoot(), assetPath)
        require(file.isFile) { "Live textbook file is not cached: $assetPath" }
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
        /** The one and only CodingCoding learner-content branch. */
        internal const val LIVE_BRANCH = "codingcoding-textbook-live"
        internal const val LIVE_TRACK_ROOT = "app/src/main/assets/textbook/v5"
        internal const val EXPECTED_PROJECT_ID = "codingcoding"
        internal const val EXPECTED_CONTENT_ID = "codingcoding-textbook-v5"
        internal const val EXPECTED_SCHEMA_VERSION = 1
        internal const val EXPECTED_REPOSITORY = "irewon1-lgtm/Myapp"
        internal const val PROJECT_LOCK_REPO_PATH =
            "app/src/main/assets/textbook/v5/project_lock.json"

        private const val REPO_ASSET_PREFIX = "app/src/main/assets/"
        private const val RAW_BASE = "https://raw.githubusercontent.com/irewon1-lgtm/Myapp"
        private const val CONTENTS_API =
            "https://api.github.com/repos/irewon1-lgtm/Myapp/contents"
        private const val CACHE_ROOT = "codingcoding_textbook_v5_live_v1"
        private const val PREFS_NAME = "codingcoding_v5_live_content_v1"
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
