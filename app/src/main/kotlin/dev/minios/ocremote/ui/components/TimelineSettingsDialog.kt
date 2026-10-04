package dev.minios.ocremote.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.minios.ocremote.R
import dev.minios.ocremote.domain.model.*
import kotlin.math.roundToInt

@Composable
fun timelineCategoryLabel(category: TimelineCategory): String = stringResource(when (category) {
    TimelineCategory.SHELL -> R.string.timeline_shell
    TimelineCategory.EDITS -> R.string.timeline_edits
    TimelineCategory.REASONING -> R.string.timeline_reasoning
    TimelineCategory.SUBAGENTS -> R.string.timeline_subagents
    TimelineCategory.NOTICES -> R.string.timeline_notices
    TimelineCategory.OTHER_TOOLS -> R.string.timeline_other_tools
})

@Composable
fun timelinePresetLabel(preset: TimelinePreset): String = stringResource(when (preset) {
    TimelinePreset.MINIMAL -> R.string.timeline_minimal
    TimelinePreset.COMPACT -> R.string.timeline_compact
    TimelinePreset.BALANCED -> R.string.timeline_balanced
    TimelinePreset.DETAILED -> R.string.timeline_detailed
    TimelinePreset.CUSTOM -> R.string.timeline_custom
})

@Composable
fun TimelineSettingsDialog(
    title: String,
    value: TimelinePreferences?,
    inherited: TimelinePreferences = TimelinePreferences(),
    onSave: (TimelinePreferences?) -> Unit,
    onDismiss: () -> Unit,
    allowInherit: Boolean = true,
) {
    var inherit by remember(value) { mutableStateOf(allowInherit && value == null) }
    var draft by remember(value, inherited) { mutableStateOf((value ?: inherited).normalized()) }
    AppDialog(onDismissRequest = onDismiss, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.timeline_description), style = MaterialTheme.typography.bodySmall)
            if (allowInherit) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.timeline_inherit), Modifier.weight(1f))
                Switch(checked = inherit, onCheckedChange = { inherit = it; if(it) draft = inherited.normalized() })
            }
            Text(timelinePresetLabel(draft.preset), style = MaterialTheme.typography.titleMedium)
            val presets = TimelinePreset.entries
            Slider(
                value = presets.indexOf(draft.preset).toFloat(),
                onValueChange = { index ->
                    val preset = presets[index.roundToInt().coerceIn(presets.indices)]
                    draft = if (preset == TimelinePreset.CUSTOM) draft.copy(preset = preset) else TimelinePreferences.forPreset(preset)
                },
                enabled = !inherit,
                valueRange = 0f..4f,
                steps = 3,
            )
            Text(stringResource(R.string.timeline_advanced), style = MaterialTheme.typography.titleSmall)
            // Three rows per category allow labels to scale without cramped columns.
            TimelineCategory.entries.forEach { category ->
                val activity = draft.activity(category)
                Text(timelineCategoryLabel(category), style = MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TimelineToggle(stringResource(R.string.timeline_visible), activity.visible, !inherit) {
                        draft = draft.withActivity(category, activity.copy(visible = it))
                    }
                    TimelineToggle(stringResource(R.string.timeline_group), activity.grouped, !inherit) {
                        draft = draft.withActivity(category, activity.copy(grouped = it))
                    }
                    TimelineToggle(stringResource(R.string.timeline_collapse), activity.collapsed, !inherit) {
                        draft = draft.withActivity(category, activity.copy(collapsed = it))
                    }
                }
                HorizontalDivider()
            }
            if (allowInherit) TextButton(onClick = { onSave(null); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.timeline_reset))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.timeline_cancel)) }
                TextButton(onClick = { onSave(if (inherit) null else draft.normalized()); onDismiss() }) { Text(stringResource(R.string.timeline_save)) }
            }
        }
    }
}

@Composable
private fun TimelineToggle(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Switch(checked = checked, enabled = enabled, onCheckedChange = onChange)
    }
}
