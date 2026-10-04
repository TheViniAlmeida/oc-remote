package dev.minios.ocremote.domain

import dev.minios.ocremote.domain.model.MAX_PROJECT_TAG_LENGTH
import dev.minios.ocremote.domain.model.MAX_PROJECT_TAGS
import dev.minios.ocremote.domain.model.ProjectAppearance
import dev.minios.ocremote.domain.model.projectAppearanceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectAppearanceTest {
    @Test
    fun tagsTrimDiscardEmptyAndDeduplicateIgnoringCase() {
        assertEquals(
            listOf("Android", "personal"),
            ProjectAppearance(tags = listOf(" Android ", "", "android", " personal ", "PERSONAL")).normalized().tags,
        )
    }

    @Test
    fun tagsHaveBoundedCountAndLengthAndNormalizeIdempotently() {
        val normalized = ProjectAppearance(tags = (1..20).map { "$it-" + "x".repeat(80) }).normalized()
        assertEquals(MAX_PROJECT_TAGS, normalized.tags.size)
        assertEquals(MAX_PROJECT_TAG_LENGTH, normalized.tags.first().length)
        assertEquals(normalized, normalized.normalized())
        assertNull(ProjectAppearance(color = "unknown").normalized().color)
    }

    @Test
    fun stableProjectIdWinsOverDirectoryWhileFallbackNormalizesPath() {
        assertEquals("project", projectAppearanceId("project", "/moved"))
        assertEquals("directory:/repo", projectAppearanceId("", "/repo/sub/../"))
        assertEquals("directory:/repo", projectAppearanceId("directory:/repo/", "/repo//"))
    }
}
