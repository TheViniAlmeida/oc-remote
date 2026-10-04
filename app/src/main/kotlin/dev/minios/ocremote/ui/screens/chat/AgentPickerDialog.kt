package dev.minios.ocremote.ui.screens.chat

import androidx.compose.foundation.clickable
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

/** Subagents cannot be selected as the primary conversation agent. */
internal fun selectableAgents(agents: List<AgentInfo>, search: String = ""): List<AgentInfo> = agents
    .filter { !it.hidden && it.mode in setOf("primary", "all") }
    .distinctBy { it.name }
    .filter { search.isBlank() || it.name.contains(search.trim(), true) || it.description.orEmpty().contains(search.trim(), true) }
    .sortedBy { it.name.lowercase() }

@Composable
fun AgentPickerDialog(agents: List<AgentInfo>, selectedAgent: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    var search by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.agent_picker_title)) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(search, { search = it }, label = { Text(stringResource(R.string.agent_picker_search)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                val options = remember(agents, search) { selectableAgents(agents, search) }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    if (options.isEmpty()) item { Text(stringResource(R.string.agent_catalog_empty), Modifier.padding(vertical = 16.dp)) }
                    items(options, key = { it.name }) { agent ->
                        Row(Modifier.fillMaxWidth().clickable { onSelect(agent.name) }.padding(vertical = 12.dp)) {
                            RadioButton(selectedAgent == agent.name, onClick = null)
                            Column(Modifier.padding(start = 8.dp)) {
                                Text(agent.name, style = MaterialTheme.typography.titleSmall)
                                agent.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.server_cancel)) } },
    )
}
