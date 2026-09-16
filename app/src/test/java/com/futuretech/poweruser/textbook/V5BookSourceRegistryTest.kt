package com.futuretech.poweruser.textbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V5BookSourceRegistryTest {
    @Test
    fun sourceIdsAreUniqueAndNonBlank() {
        val sources = V5BookSourceRegistry.sources
        assertEquals(sources.size, sources.map { it.id }.toSet().size)
        assertTrue(sources.all { it.id.isNotBlank() && it.title.isNotBlank() && it.authorOrOwner.isNotBlank() })
    }

    @Test
    fun everyTrackHasMultipleIndependentEvidenceAnchors() {
        assertEquals((1..11).toSet(), V5BookSourceRegistry.trackSourceSpines.keys)
        V5BookSourceRegistry.trackSourceSpines.forEach { (track, ids) ->
            assertTrue("TRACK $track needs multiple evidence anchors, got $ids", ids.size >= 3)
            assertTrue("TRACK $track contains unknown source IDs", ids.all(V5BookSourceRegistry.byId::containsKey))
            val types = ids.map { V5BookSourceRegistry.byId.getValue(it).sourceType }.toSet()
            assertTrue("TRACK $track must not depend on one source class only: $types", types.size >= 2)
        }
    }

    @Test
    fun sourceUrlsUseCanonicalHttpsWhereAUrlIsRegistered() {
        val bad = V5BookSourceRegistry.sources
            .filter { it.canonicalUrl != null }
            .filterNot { it.canonicalUrl!!.startsWith("https://") }
        assertTrue("Non-HTTPS source URLs: $bad", bad.isEmpty())
    }
}
