package es.ivanstg.bandrepertoire.ui.attendance

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.ivanstg.bandrepertoire.data.model.Rehearsal
import es.ivanstg.bandrepertoire.ui.appViewModel
import es.ivanstg.bandrepertoire.ui.components.EmptyView
import es.ivanstg.bandrepertoire.ui.components.StateContent
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AttendanceRoute(ensembleId: Long, snackbarHostState: SnackbarHostState) {
    val viewModel = appViewModel(key = "attendance-$ensembleId") { AttendanceViewModel(it.repository, ensembleId, it.clock) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    StateContent(state, viewModel::retry) { ui ->
        LaunchedEffect(ui.message) {
            ui.message?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.messageShown()
            }
        }
        when {
            ui.selectedRehearsalId == null -> EmptyView("No rehearsals yet", hint = "Schedule one in the API's Swagger UI.")
            ui.rows.isEmpty() -> EmptyView("This ensemble has no members", hint = "Add members in the API's Swagger UI.")
            else -> AttendanceScreen(
                ui = ui,
                onSelectRehearsal = viewModel::selectRehearsal,
                onAttendedChange = viewModel::setAttended,
                onReasonChange = viewModel::setReason,
                onSave = viewModel::save,
            )
        }
        if (ui.pendingRehearsalId != null) {
            AlertDialog(
                onDismissRequest = viewModel::cancelRehearsalSwitch,
                title = { Text("Discard changes?") },
                text = { Text("The attendance you marked for this rehearsal has not been saved.") },
                confirmButton = { TextButton(onClick = viewModel::confirmRehearsalSwitch) { Text("Discard") } },
                dismissButton = { TextButton(onClick = viewModel::cancelRehearsalSwitch) { Text("Keep editing") } },
            )
        }
    }
}

private val rehearsalFormat = DateTimeFormatter.ofPattern("EEE d MMM · HH:mm", Locale.ENGLISH)

private fun Rehearsal.label() = listOfNotNull(startsAt.format(rehearsalFormat), location).joinToString(" · ")

@Composable
fun AttendanceScreen(
    ui: AttendanceUi,
    onSelectRehearsal: (Long) -> Unit,
    onAttendedChange: (Long, Boolean) -> Unit,
    onReasonChange: (Long, String) -> Unit,
    onSave: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        RehearsalPicker(ui.rehearsals, ui.selectedRehearsalId, onSelectRehearsal)
        LazyColumn(Modifier.weight(1f)) {
            items(ui.rows, key = { it.musicianId }) { row ->
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(row.name, style = MaterialTheme.typography.bodyLarge)
                            Text(row.instrument, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(if (row.attended) "Present" else "Absent", style = MaterialTheme.typography.labelMedium)
                        Switch(
                            checked = row.attended,
                            onCheckedChange = { onAttendedChange(row.musicianId, it) },
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    if (!row.attended) {
                        OutlinedTextField(
                            value = row.reason,
                            onValueChange = { onReasonChange(row.musicianId, it) },
                            label = { Text("Reason (optional)") },
                            supportingText = { Text("${row.reason.length}/${AttendanceViewModel.MAX_REASON_LENGTH}") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                HorizontalDivider()
            }
        }
        Button(onClick = onSave, enabled = ui.canSave, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(if (ui.saving) "Saving…" else "Save attendance")
        }
    }
}

@Composable
private fun RehearsalPicker(rehearsals: List<Rehearsal>, selectedId: Long?, onSelect: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = rehearsals.firstOrNull { it.id == selectedId }
    Box(Modifier.fillMaxWidth().padding(16.dp)) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected?.label() ?: "Choose a rehearsal")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            rehearsals.forEach { rehearsal ->
                DropdownMenuItem(
                    text = { Text(rehearsal.label()) },
                    onClick = {
                        expanded = false
                        onSelect(rehearsal.id)
                    },
                )
            }
        }
    }
}
