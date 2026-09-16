package com.futuretech.poweruser.textbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class V5SourceFreshnessContractTest {
    @Test
    fun tlsSpinesUseRfc9846NotObsoleteRfc8446() {
        assertTrue("RFC9846" in V5BookSourceRegistry.byId)
        assertFalse("RFC8446" in V5BookSourceRegistry.byId)
        assertTrue("RFC9846" in V5BookSourceRegistry.trackSourceSpines.getValue(6))
        assertTrue("RFC9846" in V5BookSourceRegistry.trackSourceSpines.getValue(9))
    }

    @Test
    fun ddiaReferenceIsSecondEditionReleasedIn2026() {
        val ddia = V5BookSourceRegistry.byId.getValue("DDIA")
        assertTrue(ddia.editionOrVersion.orEmpty().contains("2nd edition"))
        assertTrue(ddia.editionOrVersion.orEmpty().contains("2026"))
        assertTrue(ddia.authorOrOwner.contains("Chris Riccomini"))
    }

    @Test
    fun sourceSpinesAreNotSingleSourceClassEchoChambers() {
        V5BookSourceRegistry.trackSourceSpines.forEach { (track, ids) ->
            val sourceTypes = ids.map { V5BookSourceRegistry.byId.getValue(it).sourceType }.toSet()
            assertTrue("TRACK $track needs at least two independent source classes: $sourceTypes", sourceTypes.size >= 2)
        }
    }

    @Test
    fun openApi31IsPinnedToLatest31PatchUsedByThisBook() {
        assertEquals("3.1.2", V5BookSourceRegistry.byId.getValue("OPENAPI31").editionOrVersion)
    }
}
