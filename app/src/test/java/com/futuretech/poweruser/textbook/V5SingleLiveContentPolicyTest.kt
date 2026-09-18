package com.futuretech.poweruser.textbook

import org.junit.Assert.assertEquals
import org.junit.Test

class V5SingleLiveContentPolicyTest {
    @Test
    fun oneCanonicalLiveBranchOwnsAllTrackContent() {
        assertEquals(
            "codingcoding-textbook-live",
            V5BookAssetRepository.LIVE_BRANCH
        )
        assertEquals(
            "app/src/main/assets/textbook/v5",
            V5BookAssetRepository.LIVE_TRACK_ROOT
        )
        assertEquals("codingcoding", V5BookAssetRepository.EXPECTED_PROJECT_ID)
        assertEquals("codingcoding-textbook-v5", V5BookAssetRepository.EXPECTED_CONTENT_ID)
        assertEquals(1, V5BookAssetRepository.EXPECTED_SCHEMA_VERSION)
        assertEquals("irewon1-lgtm/Myapp", V5BookAssetRepository.EXPECTED_REPOSITORY)
        assertEquals(
            "app/src/main/assets/textbook/v5/project_lock.json",
            V5BookAssetRepository.PROJECT_LOCK_REPO_PATH
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
