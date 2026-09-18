package com.futuretech.poweruser.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.futuretech.poweruser.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Side-load updater for the personal Myapp distribution.
 *
 * Flow:
 * 1) Read the newest successful GitHub Release.
 * 2) Compare its semantic version with the installed app.
 * 3) Download the signed APK through Android DownloadManager.
 * 4) Hand the APK to Android's package installer.
 *
 * Android intentionally requires a final user confirmation for APK installation.
 */
object AppUpdateManager {
    private const val UPDATE_METADATA_URL =
        "https://raw.githubusercontent.com/irewon1-lgtm/Myapp/textbook-v5-live/app/src/main/assets/update/latest.json"
    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/irewon1-lgtm/Myapp/releases/latest"
    private const val PREFERRED_ASSET = "Myapp_AI_Coding_Textbook_latest.apk"
    private const val APK_MIME = "application/vnd.android.package-archive"
    private val USER_AGENT = "Myapp-AutoUpdater/${BuildConfig.VERSION_NAME}"

    data class UpdateInfo(
        val versionName: String,
        val tagName: String,
        val apkUrl: String,
        val releaseNotes: String
    )

    sealed interface CheckResult {
        data class Available(val info: UpdateInfo) : CheckResult
        data object UpToDate : CheckResult
        data class Unavailable(val reason: String) : CheckResult
    }

    suspend fun checkForUpdate(): CheckResult = withContext(Dispatchers.IO) {
        checkStaticMetadata() ?: checkReleaseApi()
    }

    private fun checkStaticMetadata(): CheckResult? {
        val cacheBust = System.currentTimeMillis()
        val body = fetchText("$UPDATE_METADATA_URL?ts=$cacheBust") ?: return null
        val info = parseStaticUpdateJson(body) ?: return null
        return if (isNewerVersion(info.versionName, BuildConfig.VERSION_NAME)) {
            CheckResult.Available(info)
        } else {
            CheckResult.UpToDate
        }
    }

    private fun checkReleaseApi(): CheckResult {
        val body = fetchText(LATEST_RELEASE_API)
            ?: return CheckResult.Unavailable("release_api_unavailable")
        val release = parseReleaseJson(body)
            ?: return CheckResult.Unavailable("release_json_invalid")
        val latestVersion = release.tagName.removePrefix("v").trim()
        if (latestVersion.isBlank()) return CheckResult.Unavailable("release_version_missing")

        val preferred = release.assets.firstOrNull {
            it.name == PREFERRED_ASSET && it.browserDownloadUrl.startsWith("https://", ignoreCase = true)
        }
        val fallback = release.assets.firstOrNull {
            it.name.endsWith(".apk", ignoreCase = true) &&
                it.browserDownloadUrl.startsWith("https://", ignoreCase = true)
        }
        val apkUrl = (preferred ?: fallback)?.browserDownloadUrl
            ?: return CheckResult.Unavailable("release_apk_missing")

        return if (!isNewerVersion(latestVersion, BuildConfig.VERSION_NAME)) {
            CheckResult.UpToDate
        } else {
            CheckResult.Available(
                UpdateInfo(
                    versionName = latestVersion,
                    tagName = release.tagName,
                    apkUrl = apkUrl,
                    releaseNotes = release.releaseNotes
                )
            )
        }
    }

    private fun fetchText(url: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
                useCaches = false
                setRequestProperty("Accept", "application/json,application/vnd.github+json")
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0")
                setRequestProperty("Pragma", "no-cache")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } catch (_: Throwable) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun downloadUpdate(context: Context, info: UpdateInfo): File? {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: return null
        val apkFile = File(downloadDir, "Myapp_${info.versionName}_update.apk")
        if (apkFile.exists()) apkFile.delete()

        val request = DownloadManager.Request(Uri.parse(info.apkUrl)).apply {
            setTitle("Myapp ${info.versionName} 업데이트")
            setDescription("검증된 최신 APK를 다운로드하는 중입니다.")
            setMimeType(APK_MIME)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                apkFile.name
            )
        }

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)
        val query = DownloadManager.Query().setFilterById(downloadId)
        val deadline = System.currentTimeMillis() + 5 * 60_000L

        while (System.currentTimeMillis() < deadline) {
            val status = withContext(Dispatchers.IO) {
                manager.query(query).use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    val column = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (column < 0) null else cursor.getInt(column)
                }
            }

            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> return apkFile.takeIf { it.exists() && it.length() > 0L }
                DownloadManager.STATUS_FAILED -> return null
            }
            delay(700L)
        }

        manager.remove(downloadId)
        return null
    }

    fun canRequestPackageInstalls(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()
    }

    fun installPermissionIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun launchInstaller(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() == 0L) return false
        return try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, APK_MIME)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Throwable) {
            false
        }
    }
}

internal fun parseStaticUpdateJson(body: String): AppUpdateManager.UpdateInfo? {
    if (body.isBlank() || body.length > 64 * 1024) return null
    return try {
        val json = JSONObject(body)
        val versionName = json.optString("versionName").trim()
        val tagName = json.optString("tagName").trim()
        val apkUrl = json.optString("apkUrl").trim()
        if (versionName.isBlank() || tagName.isBlank() || !apkUrl.startsWith("https://")) return null
        AppUpdateManager.UpdateInfo(
            versionName = versionName,
            tagName = tagName,
            apkUrl = apkUrl,
            releaseNotes = json.optString("releaseNotes").take(20_000)
        )
    } catch (_: Throwable) {
        null
    }
}

internal data class ReleaseAssetJson(
    val name: String,
    val browserDownloadUrl: String
)

internal data class ParsedReleaseJson(
    val tagName: String,
    val releaseNotes: String,
    val assets: List<ReleaseAssetJson>
)

internal fun parseReleaseJson(body: String): ParsedReleaseJson? {
    if (body.isBlank() || body.length > 512 * 1024) return null
    return try {
        val json = JSONObject(body)
        val tag = json.optString("tag_name").trim()
        val assetsJson = json.optJSONArray("assets") ?: return null
        if (tag.isBlank()) return null

        val assets = buildList {
            for (index in 0 until assetsJson.length()) {
                val asset = assetsJson.optJSONObject(index) ?: continue
                val name = asset.optString("name").trim()
                val url = asset.optString("browser_download_url").trim()
                if (name.isNotBlank() && url.isNotBlank()) {
                    add(ReleaseAssetJson(name, url))
                }
            }
        }
        ParsedReleaseJson(
            tagName = tag,
            releaseNotes = json.optString("body").take(20_000),
            assets = assets
        )
    } catch (_: Throwable) {
        null
    }
}

internal fun isNewerVersion(latest: String, current: String): Boolean {
    fun parts(value: String): List<Int> = value
        .trim()
        .removePrefix("v")
        .substringBefore('-')
        .split('.')
        .map { it.toIntOrNull() ?: 0 }

    val left = parts(latest)
    val right = parts(current)
    val size = maxOf(left.size, right.size)
    for (index in 0 until size) {
        val l = left.getOrElse(index) { 0 }
        val r = right.getOrElse(index) { 0 }
        if (l != r) return l > r
    }
    return false
}
