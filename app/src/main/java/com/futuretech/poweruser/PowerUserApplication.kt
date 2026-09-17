package com.futuretech.poweruser

import android.app.Application
import com.futuretech.poweruser.textbook.V5BookAssetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/** Starts a best-effort V5 content refresh without delaying app startup or offline reading. */
class PowerUserApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        migrateHybridBaselineToP105()
        applicationScope.launch {
            V5BookAssetRepository(this@PowerUserApplication).refreshRemoteContent()
        }
    }

    private fun migrateHybridBaselineToP105() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val activeRevision = prefs.getString(PREF_ACTIVE_REVISION, null)
        if (activeRevision == null || activeRevision == OLD_BUNDLED_REVISION) {
            File(filesDir, CACHE_ROOT).deleteRecursively()
            prefs.edit()
                .putString(PREF_BASELINE_REVISION, OLD_BUNDLED_REVISION)
                .putString(PREF_ACTIVE_REVISION, P105_BUNDLED_REVISION)
                .remove(PREF_PREVIOUS_REVISION)
                .apply()
        }
    }

    companion object {
        private const val OLD_BUNDLED_REVISION = "bf78170de204d02b2c37d6746b5c399659ac4207"
        private const val P105_BUNDLED_REVISION = "4e3c7c49311ebd9338bf4f9ce64e9e6760d06f99"
        private const val CACHE_ROOT = "remote_textbook_v5"
        private const val PREFS_NAME = "v5_remote_content"
        private const val PREF_ACTIVE_REVISION = "active_revision"
        private const val PREF_PREVIOUS_REVISION = "previous_revision"
        private const val PREF_BASELINE_REVISION = "baseline_revision"
    }
}
