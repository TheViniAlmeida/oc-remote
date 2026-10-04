package dev.minios.ocremote.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.minios.ocremote.R
import dev.minios.ocremote.domain.model.FileDiff

data class SessionDiffState(val isLoading: Boolean = false, val files: List<FileDiff> = emptyList(), val error: String? = null)

@Composable
fun SessionDiffDialog(state: SessionDiffState, onRetry: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.session_diff_title)) },
        text = {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                when {
                    state.isLoading -> item { CircularProgressIndicator() }
                    state.error != null -> item { Text(state.error, color = MaterialTheme.colorScheme.error) }
                    state.files.isEmpty() -> item { Text(stringResource(R.string.session_diff_empty)) }
                    else -> itemsIndexed(state.files) { _, file ->
                        var expanded by remember(file) { mutableStateOf(false) }
                        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            TextButton(onClick = { expanded = !expanded }) {
                                Text("${file.file}  +${file.additions} / −${file.deletions}")
                            }
                            if (expanded) {
                                Text(stringResource(R.string.session_diff_before), style = MaterialTheme.typography.labelLarge)
                                Text(file.before, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(8.dp))
                                Text(stringResource(R.string.session_diff_after), style = MaterialTheme.typography.labelLarge)
                                Text(file.after, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { if (state.error != null) TextButton(onClick = onRetry) { Text(stringResource(R.string.session_diff_retry)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.server_cancel)) } },
    )
}
