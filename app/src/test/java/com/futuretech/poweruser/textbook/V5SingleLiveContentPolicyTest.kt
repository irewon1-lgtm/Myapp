package com.futuretech.poweruser.textbook

import org.junit.Assert.assertEquals
import org.junit.Test

class V5SingleLiveContentPolicyTest {
    @Test
    fun oneCanonicalLiveBranchOwnsAllTrackContent() {
        assertEquals("textbook-v5-live", V5BookAssetRepository.LIVE_BRANCH)
        assertEquals(
            "app/src/main/assets/textbook/v5",
            V5BookAssetRepository.LIVE_TRACK_ROOT
        )
    }

    @Test
    fun everyTrackUsesTheSameLiveRoot() {
        assertEquals(
            "app/src/main/assets/textbook/v5/track_01",
            V5BookAssetRepository.liveTrackRepoPath(1)
        )
        assertEquals(
            "app/src/main/assets/textbook/v5/track_07",
            V5BookAssetRepository.liveTrackRepoPath(7)
        )
        assertEquals(
            "app/src/main/assets/textbook/v5/track_11",
            V5BookAssetRepository.liveTrackRepoPath(11)
        )
    }
}
