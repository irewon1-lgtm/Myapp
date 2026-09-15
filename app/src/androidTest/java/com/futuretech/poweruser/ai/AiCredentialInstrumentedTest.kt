package com.futuretech.poweruser.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.futuretech.poweruser.data.SecureKeyStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiCredentialInstrumentedTest {
    @Test
    fun encryptedKeySaveReadClearLifecycle() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val storage = SecureKeyStorage(context)
        storage.clearApiKey()
        assertNull(storage.getApiKey())

        val fakeNonWorkingKey = "extreme-test-key-not-valid-2026"
        storage.saveApiKey(fakeNonWorkingKey)
        assertEquals(fakeNonWorkingKey, storage.getApiKey())

        storage.clearApiKey()
        assertNull(storage.getApiKey())
    }
}
