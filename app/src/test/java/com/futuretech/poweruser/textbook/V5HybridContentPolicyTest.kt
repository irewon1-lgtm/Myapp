package com.futuretech.poweruser.textbook

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V5HybridContentPolicyTest {
    @Test
    fun fullGitRevisionIsRequired() {
        assertTrue(V5BookAssetRepository.isValidRevision("0123456789abcdef0123456789abcdef01234567"))
        assertFalse(V5BookAssetRepository.isValidRevision("main"))
        assertFalse(V5BookAssetRepository.isValidRevision("0123456"))
        assertFalse(V5BookAssetRepository.isValidRevision("0123456789ABCDEF0123456789ABCDEF01234567"))
    }

    @Test
    fun remoteAssetPathsCannotEscapeV5Root() {
        assertTrue(V5BookAssetRepository.isSafeAssetPath("textbook/v5/track_02/part_86.md"))
        assertFalse(V5BookAssetRepository.isSafeAssetPath("textbook/v5/../secrets.txt"))
        assertFalse(V5BookAssetRepository.isSafeAssetPath("/textbook/v5/track_02/part_86.md"))
        assertFalse(V5BookAssetRepository.isSafeAssetPath("textbook/v4/track_02/part_86.md"))
        assertFalse(V5BookAssetRepository.isSafeAssetPath("textbook/v5/track_02\\part_86.md"))
    }
}
