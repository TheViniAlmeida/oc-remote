package dev.minios.ocremote.ui.screens.server

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.minios.ocremote.R
import dev.minios.ocremote.data.api.AgentInfo

internal fun agentCatalogGroups(agents: List<AgentInfo>): List<Pair<Int, List<AgentInfo>>> {
    val sorted = agents.distinctBy { it.name }.sortedBy { it.name.lowercase() }
    return listOf(
        R.string.agent_catalog_primary to sorted.filter { it.mode in setOf("primary", "all") },
        R.string.agent_catalog_subagents to sorted.filter { it.mode in setOf("subagent", "all") },
        R.string.agent_catalog_other to sorted.filter { it.mode !in setOf("primary", "subagent", "all") },
    ).filter { it.second.isNotEmpty() }
}

@Composable
fun ServerAgentsDialog(agents: List<AgentInfo>, isLoading: Boolean, error: String?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.agent_catalog_title)) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                when {
                    isLoading -> item { CircularProgressIndicator() }
                    error != null -> item { Text(error, color = MaterialTheme.colorScheme.error) }
                    agents.isEmpty() -> item { Text(stringResource(R.string.agent_catalog_empty)) }
                    else -> agentCatalogGroups(agents).forEach { (title, members) ->
                        item { Text(stringResource(title), Modifier.padding(top = 16.dp, bottom = 8.dp), style = MaterialTheme.typography.titleSmall) }
                        items(members, key = { "${title}:${it.name}" }) { agent ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Text(agent.name, style = MaterialTheme.typography.titleSmall)
                                if (agent.hidden) Text(stringResource(R.string.agent_catalog_hidden), style = MaterialTheme.typography.labelSmall)
                                if (agent.mode == "all") Text(stringResource(R.string.agent_catalog_both), style = MaterialTheme.typography.labelSmall)
                                if (agent.mode !in setOf("primary", "subagent", "all")) Text(agent.mode, style = MaterialTheme.typography.labelSmall)
                                agent.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.server_cancel)) } },
    )
}
