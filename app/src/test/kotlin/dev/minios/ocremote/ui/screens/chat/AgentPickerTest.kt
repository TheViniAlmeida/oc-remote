package dev.minios.ocremote.ui.screens.chat

import dev.minios.ocremote.data.api.AgentInfo
import org.junit.Assert.*
import org.junit.Test

class AgentPickerTest {
    @Test fun primaryPickerRejectsSubagentsHiddenAndUnknownModes() {
        val agents = listOf(AgentInfo("build"), AgentInfo("all", mode = "all"),
            AgentInfo("worker", mode = "subagent"), AgentInfo("hidden", hidden = true), AgentInfo("future", mode = "other"))
        assertEquals(listOf("all", "build"), selectableAgents(agents).map { it.name })
    }
    @Test fun searchIncludesDescriptionAndDeduplicates() {
        val agents = listOf(AgentInfo("plan", "Architecture"), AgentInfo("plan"), AgentInfo("build"))
        assertEquals(listOf("plan"), selectableAgents(agents, "ARCHI").map { it.name })
    }
}
