package dev.minios.ocremote.ui.screens.server

import dev.minios.ocremote.R
import dev.minios.ocremote.data.api.AgentInfo
import org.junit.Assert.*
import org.junit.Test

class AgentCatalogTest {
    @Test fun bothModeAppearsInBothGroupsAndHiddenAgentsRemainInspectable() {
        val groups = agentCatalogGroups(listOf(AgentInfo("both", mode = "all"), AgentInfo("worker", mode = "subagent", hidden = true))).toMap()
        assertEquals(listOf("both"), groups[R.string.agent_catalog_primary]!!.map { it.name })
        assertEquals(listOf("both", "worker"), groups[R.string.agent_catalog_subagents]!!.map { it.name })
        assertTrue(groups[R.string.agent_catalog_subagents]!!.last().hidden)
    }
}
