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
        migrateOnceToIntegratedTextbookBaseline()
        applicationScope.launch {
            V5BookAssetRepository(this@PowerUserApplication).refreshRemoteContent()
        }
    }

    /**
     * Runs once for this recovery baseline. Any older verified overlay is discarded exactly once so
     * it cannot shadow the newer textbook bundled in the APK. After this marker is stored, future
     * verified remote overlays survive process restarts normally.
     */
    private fun migrateOnceToIntegratedTextbookBaseline() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (prefs.getBoolean(PREF_RECOVERY_MIGRATION_DONE, false)) return

        File(filesDir, CACHE_ROOT).deleteRecursively()
        prefs.edit()
            .clear()
            .putString(PREF_BASELINE_REVISION, READER_CACHE_BASELINE)
            .putString(PREF_ACTIVE_REVISION, INTEGRATED_BUNDLED_REVISION)
            .putBoolean(PREF_RECOVERY_MIGRATION_DONE, true)
            .apply()
    }

    companion object {
        private const val READER_CACHE_BASELINE = "bf78170de204d02b2c37d6746b5c399659ac4207"
        private const val INTEGRATED_BUNDLED_REVISION = "41184044e91c98ba9e49c45f71e2445c464b928d"

        private const val CACHE_ROOT = "remote_textbook_v5"
        private const val PREFS_NAME = "v5_remote_content"
        private const val PREF_ACTIVE_REVISION = "active_revision"
        private const val PREF_BASELINE_REVISION = "baseline_revision"
        private const val PREF_RECOVERY_MIGRATION_DONE = "recovery_41184044_migrated"
    }
}
