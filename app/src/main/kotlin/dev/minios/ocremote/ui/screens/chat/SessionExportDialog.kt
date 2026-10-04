package dev.minios.ocremote.ui.screens.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.minios.ocremote.R
import dev.minios.ocremote.domain.export.SessionExportFormat

@Composable
fun SessionExportDialog(onCopy: (SessionExportFormat) -> Unit, onDownload: (SessionExportFormat) -> Unit, onDismiss: () -> Unit) {
    var format by remember { mutableStateOf(SessionExportFormat.MARKDOWN) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.session_export_title)) },
        text = {
            Column {
                SessionExportFormat.entries.forEach { option ->
                    Row(Modifier.fillMaxWidth().clickable { format = option }.padding(vertical = 4.dp)) {
                        RadioButton(format == option, onClick = null)
                        Text(when (option) { SessionExportFormat.MARKDOWN -> "Markdown (.md)"; SessionExportFormat.JSON -> "JSON (.json)"; SessionExportFormat.HTML -> "HTML (.html)" }, modifier = Modifier.padding(top = 12.dp))
                    }
                }
                Text(stringResource(R.string.session_export_full_history), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = { onDownload(format) }) { Text(stringResource(R.string.session_export_download)) } },
        dismissButton = {
            Row {
                TextButton(onClick = { onCopy(format) }) { Text(stringResource(R.string.session_export_copy)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.server_cancel)) }
            }
        },
    )
}
