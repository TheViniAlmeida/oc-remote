package dev.minios.ocremote.ui.screens.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.minios.ocremote.R
import dev.minios.ocremote.domain.model.SessionControls
import dev.minios.ocremote.ui.components.AppDialog
import kotlin.math.roundToInt

@Composable
fun SessionControlsDialog(
    controls: SessionControls,
    onChange: (SessionControls) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(controls.normalized()) }
    AppDialog(onDismissRequest = onDismiss, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.session_controls_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.session_controls_scope), style = MaterialTheme.typography.bodySmall)
            SessionControlToggle(
                stringResource(R.string.session_controls_auto_approve),
                stringResource(R.string.session_controls_auto_approve_description),
                draft.autoApprovePermissions,
            ) { draft = draft.copy(autoApprovePermissions = it) }
            SessionControlToggle(
                stringResource(R.string.session_controls_auto_compact),
                stringResource(R.string.session_controls_auto_compact_description),
                draft.autoCompact,
            ) { draft = draft.copy(autoCompact = it) }
            if (draft.autoCompact) {
                Column {
                    Text(stringResource(R.string.session_controls_threshold, draft.compactionThresholdPercent))
                    Slider(
                        value = draft.compactionThresholdPercent.toFloat(),
                        onValueChange = { draft = draft.copy(compactionThresholdPercent = it.roundToInt()) },
                        valueRange = 70f..95f,
                        steps = 24,
                    )
                }
            }
            SessionControlToggle(
                stringResource(R.string.session_controls_mute),
                stringResource(R.string.session_controls_mute_description),
                draft.muted,
            ) { draft = draft.copy(muted = it) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.session_controls_cancel)) }
                TextButton(onClick = { onChange(draft.normalized()); onDismiss() }) {
                    Text(stringResource(R.string.session_controls_save))
                }
            }
        }
    }
}

@Composable
private fun SessionControlToggle(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
