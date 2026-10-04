package dev.minios.ocremote.ui.components

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.minios.ocremote.R
import dev.minios.ocremote.domain.model.NotificationCategory
import dev.minios.ocremote.domain.model.NotificationPreferences
import dev.minios.ocremote.domain.model.NotificationSound
import dev.minios.ocremote.domain.model.NotificationTiming
import dev.minios.ocremote.service.ensurePreferenceNotificationChannel
import dev.minios.ocremote.service.notificationCategoryLabel

@Composable
fun NotificationPreferencesDialog(
    preferences: NotificationPreferences,
    inherited: Boolean = false,
    allowInheritance: Boolean = false,
    onSave: (NotificationPreferences?) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(preferences) }
    var useInherited by remember { mutableStateOf(inherited) }
    val context = LocalContext.current
    AppDialog(onDismissRequest = onDismiss, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.notification_preferences_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.notification_preferences_description), style = MaterialTheme.typography.bodySmall)
            if (allowInheritance) PreferenceSwitch(stringResource(R.string.notification_preferences_inherit), useInherited) { useInherited = it }
            val editable = !allowInheritance || !useInherited
            NotificationCategory.entries.forEach { category ->
                val rule = draft.rule(category)
                HorizontalDivider()
                Text(stringResource(notificationCategoryLabel(category)), style = MaterialTheme.typography.titleMedium)
                PreferenceSwitch(stringResource(R.string.notification_preferences_enabled), rule.enabled, editable) {
                    draft = draft.withRule(category, rule.copy(enabled = it))
                }
                val categoryEnabled = editable && rule.enabled
                PreferenceChoice(
                    stringResource(R.string.notification_preferences_sound),
                    listOf(NotificationSound.DEFAULT to R.string.notification_preferences_default_sound, NotificationSound.SILENT to R.string.notification_preferences_silent),
                    rule.sound, categoryEnabled,
                ) { draft = draft.withRule(category, rule.copy(sound = it)) }
                PreferenceSwitch(stringResource(R.string.notification_preferences_vibrate), rule.vibrate, categoryEnabled) {
                    draft = draft.withRule(category, rule.copy(vibrate = it))
                }
                PreferenceChoice(
                    stringResource(R.string.notification_preferences_when),
                    listOf(NotificationTiming.ALWAYS to R.string.notification_preferences_always,
                        NotificationTiming.OUTSIDE_ACTIVE_SESSION to R.string.notification_preferences_outside_session,
                        NotificationTiming.BACKGROUND_ONLY to R.string.notification_preferences_background),
                    rule.timing, categoryEnabled,
                ) { draft = draft.withRule(category, rule.copy(timing = it)) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    TextButton(enabled = categoryEnabled, onClick = {
                        val channelId = ensurePreferenceNotificationChannel(context, category, rule)
                        context.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
                        })
                    }) { Text(stringResource(R.string.notification_preferences_device_sound)) }
                }
            }
            Text(stringResource(R.string.notification_preferences_system_note), style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.notification_preferences_cancel)) }
                TextButton(onClick = { onSave(if (allowInheritance && useInherited) null else draft); onDismiss() }) {
                    Text(stringResource(R.string.notification_preferences_save))
                }
            }
        }
    }
}

@Composable
private fun PreferenceSwitch(label: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, enabled = enabled, onCheckedChange = onChange)
    }
}

@Composable
private fun <T> PreferenceChoice(label: String, choices: List<Pair<T, Int>>, value: T, enabled: Boolean, onChange: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f))
            TextButton(enabled = enabled, onClick = { expanded = true }) { Text(stringResource(choices.first { it.first == value }.second)) }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { (choice, resource) ->
                DropdownMenuItem(text = { Text(stringResource(resource)) }, onClick = { onChange(choice); expanded = false })
            }
        }
    }
}
