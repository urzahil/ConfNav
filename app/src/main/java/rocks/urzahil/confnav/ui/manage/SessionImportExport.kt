package rocks.urzahil.confnav.ui.manage

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import rocks.urzahil.confnav.data.model.ImportReport
import rocks.urzahil.confnav.ui.ConferenceViewModel
import rocks.urzahil.confnav.ui.ImportOutcome
import rocks.urzahil.confnav.ui.icons.filled.FileDownload

private const val EXPORT_FILE_NAME = "conference_sessions.json"
private const val MAX_IMPORT_CHARS = 5_000_000

/** Entry points for the JSON export/import buttons. */
class SessionImportExport(
    val export: () -> Unit,
    val import: () -> Unit
)

/**
 * Wires up the file pickers, parsing, the "add or replace" dialog and the result messages for
 * JSON export and import, so screens only need to call [SessionImportExport.export] and
 * [SessionImportExport.import].
 */
@Composable
fun rememberSessionImportExport(
    viewModel: ConferenceViewModel,
    sessionCount: Int
): SessionImportExport {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<ImportReport?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(viewModel.exportSessionsJson().toByteArray(Charsets.UTF_8))
                out.flush()
            }
            Toast.makeText(context, "Saved $EXPORT_FILE_NAME successfully!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = try {
            context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }.orEmpty()
        } catch (e: Exception) {
            toast("Failed to read file: ${e.message}")
            return@rememberLauncherForActivityResult
        }
        if (text.isBlank()) {
            toast("Selected file is empty")
            return@rememberLauncherForActivityResult
        }
        if (text.length > MAX_IMPORT_CHARS) {
            toast("That file is too large to be a sessions file")
            return@rememberLauncherForActivityResult
        }

        val result = viewModel.parseImport(text)
        val report = result.getOrNull()
        when {
            report == null ->
                toast("Import failed: ${result.exceptionOrNull()?.message ?: "Invalid JSON"}")

            report.sessions.isEmpty() ->
                toast("No valid sessions found" + (report.skipped.firstOrNull()?.let { ": $it" } ?: ""))

            sessionCount == 0 ->
                viewModel.importSessions(report.sessions, replaceAll = true) { outcome ->
                    toast(summary("Imported", outcome, report))
                }

            else -> pending = report
        }
    }

    pending?.let { report ->
        ImportChoiceDialog(
            report = report,
            existingCount = sessionCount,
            onDismiss = { pending = null },
            onAdd = {
                viewModel.importSessions(report.sessions, replaceAll = false) { outcome ->
                    toast(summary("Added", outcome, report))
                    pending = null
                }
            },
            onReplace = {
                viewModel.importSessions(report.sessions, replaceAll = true) { outcome ->
                    toast(summary("Replaced agenda with", outcome, report))
                    pending = null
                }
            }
        )
    }

    return remember(sessionCount) {
        SessionImportExport(
            export = {
                if (sessionCount == 0) {
                    Toast.makeText(context, "No sessions to export. Add sessions first.", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    exportLauncher.launch(EXPORT_FILE_NAME)
                }
            },
            import = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) }
        )
    }
}

private fun summary(verb: String, outcome: ImportOutcome, report: ImportReport): String {
    val parts = mutableListOf("$verb ${outcome.added} session${if (outcome.added == 1) "" else "s"}")
    if (outcome.duplicates > 0) parts.add("${outcome.duplicates} already in your agenda")
    if (report.skipped.isNotEmpty()) parts.add("${report.skipped.size} invalid skipped")
    if (report.withoutLocation > 0) parts.add("${report.withoutLocation} without a map location")
    return parts.joinToString(", ")
}

@Composable
private fun ImportChoiceDialog(
    report: ImportReport,
    existingCount: Int,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onReplace: () -> Unit
) {
    val count = report.sessions.size
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.FileDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Import $count Sessions") },
        text = {
            val skippedNote = if (report.skipped.isNotEmpty()) {
                "\n\n${report.skipped.size} row(s) in the file were invalid and will be skipped, e.g. ${report.skipped.first()}"
            } else {
                ""
            }
            Text(
                "Found $count sessions in the loaded file.\n\nYou currently have $existingCount session(s) in your agenda. " +
                        "Would you like to add these to your existing agenda or replace everything?" + skippedNote
            )
        },
        confirmButton = {
            Button(onClick = onAdd) { Text("Add to Agenda") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = onReplace) {
                    Text("Replace All", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}
