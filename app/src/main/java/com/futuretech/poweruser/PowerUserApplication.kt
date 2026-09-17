package com.futuretech.poweruser

import android.app.Application
import com.futuretech.poweruser.textbook.V5BookAssetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Starts a best-effort V5 content refresh without delaying app startup or offline reading. */
class PowerUserApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            V5BookAssetRepository(this@PowerUserApplication).refreshRemoteContent()
        }
    }
}
