package dev.minios.ocremote.ui.screens.sessions

import dev.minios.ocremote.domain.model.normalizeProjectDirectory
import dev.minios.ocremote.domain.model.parentProjectDirectory
import dev.minios.ocremote.domain.model.projectDirectoryName
import dev.minios.ocremote.domain.model.ProjectAppearance
import dev.minios.ocremote.domain.model.Project
import dev.minios.ocremote.domain.model.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectDirectoryPickerTest {
    @Test
    fun parentNavigationCrossesHomeAndStopsOnlyAtFilesystemRoot() {
        assertEquals("/home", parentProjectDirectory("/home/user/"))
        assertEquals("/", parentProjectDirectory("/home"))
        assertNull(parentProjectDirectory("/"))
        assertNull(parentProjectDirectory("////"))
    }

    @Test
    fun remotePathsNormalizeAndDisplayOnlyFolderNames() {
        assertEquals("/repo/mobile", normalizeProjectDirectory("/repo//./mobile/"))
        assertEquals("/repo", normalizeProjectDirectory("/repo/mobile/../"))
        assertEquals("/", normalizeProjectDirectory("/../../"))
        assertEquals("mobile", projectDirectoryName("/repo/mobile/"))
        assertEquals("/", projectDirectoryName("/"))
        assertEquals(DirectoryPathQuery("/repo", ""), parseDirectoryPathQuery("/repo/mobile/../", "/home/user"))
    }

    @Test
    fun normalizedDirectoriesShareOneGroupAndAppearance() {
        val appearance = ProjectAppearance("green", listOf("mobile"))
        val groups = buildProjectSessionGroups(
            listOf(item("first", "/repo//mobile/"), item("second", "/repo/mobile/./")),
            emptyList(), null, emptyMap(), "server",
            appearances = mapOf("directory:/repo/mobile" to appearance),
        )
        assertEquals(1, groups.size)
        assertEquals(2, groups.single().sessions.size)
        assertEquals(appearance, groups.single().appearance)
        assertEquals(1, recentSessionDirectories(groups.single().sessions).size)
    }

    @Test
    fun canonicalProjectAppearanceAppliesAcrossWorktreesAndRootProjectMatchesChildren() {
        val appearance = ProjectAppearance("violet", listOf("work"))
        val group = buildProjectSessionGroups(
            listOf(item("session", "/elsewhere")), listOf(Project("root", worktree = "/")),
            null, emptyMap(), "server", mapOf("root" to appearance),
        ).single()
        assertEquals("root", group.projectId)
        assertEquals(appearance, group.appearance)
    }

    private fun item(id: String, directory: String) = SessionItem(
        Session(id, directory = directory, time = Session.Time(created = 1, updated = 1)),
    )
}
