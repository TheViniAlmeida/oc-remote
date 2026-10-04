package dev.minios.ocremote.ui.screens.server

import com.composables.icons.lucide.*
import dev.minios.ocremote.ui.components.backIcon
import dev.minios.ocremote.ui.components.forwardChevronIcon

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import dev.minios.ocremote.data.api.AgentInfo
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.minios.ocremote.R
import dev.minios.ocremote.ui.components.PresentationPreferencesHost
import dev.minios.ocremote.ui.components.PresentationPreferenceKind
import dev.minios.ocremote.ui.components.PresentationPreferenceRequest
import dev.minios.ocremote.ui.components.AppCardShape
import dev.minios.ocremote.ui.components.appAmoledBorder
import dev.minios.ocremote.ui.components.isAmoledTheme
import dev.minios.ocremote.ui.components.ServerConnectionBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenProviders: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenMcp: () -> Unit,
    isServerConnected: Boolean = true,
    isServerConnecting: Boolean = false,
    onConnectServer: () -> Unit = {},
    onDuplicateServer: (() -> Unit)? = null,
    agents: List<AgentInfo> = emptyList(),
    isLoadingAgents: Boolean = false,
    agentsError: String? = null,
    serverId: String = "",
) {
    var presentationRequest by remember { mutableStateOf<PresentationPreferenceRequest?>(null) }
    PresentationPreferencesHost(presentationRequest, onDismiss = { presentationRequest = null })
    val isAmoled = isAmoledTheme()
    var showAgents by remember { mutableStateOf(false) }
    if (showAgents) ServerAgentsDialog(agents, isLoadingAgents, agentsError) { showAgents = false }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.server_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(backIcon(), contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    onDuplicateServer?.let { duplicate ->
                        IconButton(onClick = duplicate) {
                            Icon(Lucide.Copy, contentDescription = stringResource(R.string.server_duplicate))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (!isServerConnected) {
                ServerConnectionBanner(connecting = isServerConnecting, onConnect = onConnectServer)
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
            listOf(
                PresentationPreferenceKind.SERVER_TIMELINE to R.string.timeline_title,
                PresentationPreferenceKind.SERVER_NOTIFICATIONS to R.string.notification_preferences_title,
            ).forEach { (kind, label) ->
                Card(shape = AppCardShape, modifier = Modifier.fillMaxWidth().clickable(enabled = serverId.isNotBlank()) {
                    presentationRequest = PresentationPreferenceRequest(kind, serverId)
                }) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (kind == PresentationPreferenceKind.SERVER_TIMELINE) Lucide.List else Lucide.Bell, contentDescription = null)
                        Text(stringResource(label), Modifier.weight(1f).padding(horizontal = 12.dp), style = MaterialTheme.typography.titleSmall)
                        Icon(forwardChevronIcon(), contentDescription = null)
                    }
                }
            }
            Card(
                shape = AppCardShape,
                modifier = Modifier.fillMaxWidth().clickable(enabled = isServerConnected) { showAgents = true },
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.Bot, contentDescription = null)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(stringResource(R.string.agent_catalog_title), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.agent_catalog_desc), style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(forwardChevronIcon(), contentDescription = null)
                }
            }
            Card(
                shape = AppCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (isAmoled) Color.Black else MaterialTheme.colorScheme.surfaceContainer
                ),
                border = appAmoledBorder(0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenProviders)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Lucide.Plug, contentDescription = null)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.server_settings_providers),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(R.string.server_settings_providers_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    Icon(
                        forwardChevronIcon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(
                shape = AppCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (isAmoled) Color.Black else MaterialTheme.colorScheme.surfaceContainer
                ),
                border = appAmoledBorder(0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenModels)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Lucide.SlidersHorizontal, contentDescription = null)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.server_settings_models),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(R.string.server_settings_models_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    Icon(
                        forwardChevronIcon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(
                shape = AppCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (isAmoled) Color.Black else MaterialTheme.colorScheme.surfaceContainer
                ),
                border = appAmoledBorder(0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenMcp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Lucide.ServerCog, contentDescription = null)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.server_settings_mcp),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(R.string.server_settings_mcp_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    Icon(
                        forwardChevronIcon(),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            }
        }
    }
}
