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

@Composable
fun appearanceAccentLabel(key: String): String = stringResource(when (key) {
    "red" -> R.string.appearance_red
    "orange" -> R.string.appearance_orange
    "amber" -> R.string.appearance_amber
    "green" -> R.string.appearance_green
    "teal" -> R.string.appearance_teal
    "blue" -> R.string.appearance_blue
    "violet" -> R.string.appearance_violet
    "pink" -> R.string.appearance_pink
    else -> R.string.appearance_default_color
})

@Composable
fun AppearanceSettingsDialog(
    title: String,
    value: AppearanceOverride?,
    inherited: ThemeAppearance,
    onSave: (AppearanceOverride?) -> Unit,
    onDismiss: () -> Unit,
    allowInherit: Boolean = true,
) {
    var draft by remember(value) { mutableStateOf(value?.normalized() ?: AppearanceOverride()) }
    AppDialog(onDismissRequest = onDismiss, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (allowInherit) Text(stringResource(R.string.appearance_inherit_desc), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(R.string.appearance_theme), style = MaterialTheme.typography.titleSmall)
            val themeChoices = (if (allowInherit) listOf<String?>(null) else emptyList()) + appThemeKeys
            themeChoices.forEach { theme ->
                val label = when(theme) {
                    null -> stringResource(R.string.appearance_inherit)
                    "light" -> stringResource(R.string.appearance_light)
                    "dark" -> stringResource(R.string.appearance_dark)
                    else -> stringResource(R.string.appearance_system)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = draft.theme == theme, onClick = { draft = draft.copy(theme = theme) })
                    TextButton(onClick = { draft = draft.copy(theme = theme) }) { Text(label) }
                }
            }
            Text(stringResource(R.string.appearance_accent), style = MaterialTheme.typography.titleSmall)
            val accents = (if (allowInherit) listOf<String?>(null) else emptyList()) + appAccentKeys
            // Two columns keep the full palette accessible on narrow phones.
            accents.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { accent ->
                        FilterChip(selected = draft.accent == accent,
                            onClick = { draft = draft.copy(accent = accent) },
                            label = { Text(if (accent == null) stringResource(R.string.appearance_inherit) else appearanceAccentLabel(accent)) },
                            modifier = Modifier.weight(1f).padding(end = 6.dp))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Text(stringResource(R.string.appearance_palette_priority), style = MaterialTheme.typography.bodySmall)
            AppearanceBooleanChoice(stringResource(R.string.appearance_dynamic), draft.dynamicColor, inherited.dynamicColor, allowInherit) {
                draft = draft.copy(dynamicColor = it)
            }
            AppearanceBooleanChoice(stringResource(R.string.appearance_amoled), draft.amoledDark, inherited.amoledDark, allowInherit) {
                draft = draft.copy(amoledDark = it)
            }
            if (allowInherit) TextButton(onClick = { onSave(null); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.appearance_reset))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.appearance_cancel)) }
                TextButton(onClick = { onSave(draft.normalized().takeUnless { it.isEmpty }); onDismiss() }) { Text(stringResource(R.string.appearance_save)) }
            }
        }
    }
}

@Composable
private fun AppearanceBooleanChoice(title: String, value: Boolean?, inherited: Boolean, allowInherit: Boolean, onChange: (Boolean?) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Switch(checked = value ?: inherited, onCheckedChange = onChange)
        }
        if (allowInherit) Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = value == null, onCheckedChange = { onChange(if (it) null else inherited) })
            Text(stringResource(R.string.appearance_inherit), style = MaterialTheme.typography.bodySmall)
        }
    }
}
