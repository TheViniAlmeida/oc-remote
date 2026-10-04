package dev.minios.ocremote.domain.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class TimelinePreferencesTest {
    private fun text(id: String) = Part.Text(id, "session", "message", "Always visible")
    private fun tool(id: String, name: String) = Part.Tool(id, "session", "message", "call-$id", name, ToolState.Pending())

    @Test fun minimalNeverHidesMessagesAttachmentsOrRequiredInteractions() {
        val minimal = TimelinePreferences.forPreset(TimelinePreset.MINIMAL)
        assertFalse(isTimelinePartVisible(tool("tool", "bash"), minimal))
        assertTrue(isTimelinePartVisible(text("text"), minimal))
        assertTrue(isTimelinePartVisible(Part.File("file", "session", "message", "text/plain"), minimal))
        val hideAll = TimelineCategory.entries.fold(minimal) { prefs, category -> prefs.withActivity(category, TimelineActivity(visible = false)) }
        assertTrue(isTimelinePartVisible(Part.Permission("permission", "session", "message"), hideAll))
        assertTrue(isTimelinePartVisible(Part.Question("question", "session", "message"), hideAll))
        assertTrue(isTimelinePartVisible(tool("user", "bash"), hideAll, isUser = true))
    }
    @Test fun classificationUsesToolAliasesAndUnknownToolsStayControllable() {
        assertEquals(TimelineCategory.SHELL, timelineCategory(tool("1", "functions.exec_command")))
        assertEquals(TimelineCategory.EDITS, timelineCategory(tool("2", "apply_patch")))
        assertEquals(TimelineCategory.SUBAGENTS, timelineCategory(tool("3", "spawn_agent")))
        assertEquals(TimelineCategory.NOTICES, timelineCategory(tool("4", "warning")))
        assertEquals(TimelineCategory.OTHER_TOOLS, timelineCategory(tool("5", "mcp__custom")))
        assertNull(timelineCategory(text("6")))
        assertEquals(TimelineCategory.NOTICES, timelineCategory(tool("7", "bash").copy(state = ToolState.Error(error = "Failed"))))
    }
    @Test fun groupingNeverMovesActivityAcrossTextOrAttachments() {
        val prefs = TimelinePreferences.forPreset(TimelinePreset.COMPACT)
        val parts = listOf(tool("1", "bash"), tool("2", "bash"), text("3"), tool("4", "bash"), tool("5", "edit"))
        val groups = groupTimelineParts(parts, prefs)
        assertEquals(listOf(listOf("1", "2"), listOf("3"), listOf("4"), listOf("5")), groups.map { it.parts.map(Part::id) })
        assertFalse(groups[1].grouped)
        assertEquals(parts, groups.flatMap { it.parts })
        assertEquals(parts.size, groupTimelineParts(parts, prefs, isUser = true).size)
    }
    @Test fun manualChangePromotesPresetToCustomAndPreservesOtherCategories() {
        val original = TimelinePreferences.forPreset(TimelinePreset.DETAILED)
        val custom = original.withActivity(TimelineCategory.SHELL, TimelineActivity(visible = false, grouped = true))
        assertEquals(TimelinePreset.CUSTOM, custom.preset)
        assertEquals(original.edits, custom.edits)
        assertEquals(custom, custom.normalized())
        assertEquals(custom, Json.decodeFromString<TimelinePreferences>(Json.encodeToString(custom)))
    }
    @Test fun fixedPresetNormalizationAndInheritanceAreDeterministic() {
        val malformedPreset = TimelinePreferences(preset = TimelinePreset.DETAILED, shell = TimelineActivity(visible = false))
        assertTrue(malformedPreset.normalized().shell.visible)
        assertFalse(malformedPreset.normalized().shell.collapsed)
        val server = TimelinePreferences.forPreset(TimelinePreset.MINIMAL)
        val session = TimelinePreferences.forPreset(TimelinePreset.DETAILED)
        assertEquals(server, resolveTimeline(server, null))
        assertEquals(session, resolveTimeline(server, session))
    }
}
