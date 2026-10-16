package es.ivanstg.bandrepertoire.ui.concert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.ivanstg.bandrepertoire.data.model.formatDuration
import es.ivanstg.bandrepertoire.ui.appViewModel
import es.ivanstg.bandrepertoire.ui.components.EmptyView
import es.ivanstg.bandrepertoire.ui.components.StateContent
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NextConcertRoute(ensembleId: Long) {
    val viewModel = appViewModel(key = "concert-$ensembleId") { NextConcertViewModel(it.repository, ensembleId, it.clock) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    StateContent(state, viewModel::retry) { next ->
        if (next == null) {
            EmptyView("No upcoming concerts", hint = "Add one in the API's Swagger UI.")
        } else {
            NextConcertScreen(next)
        }
    }
}

private val dateFormat = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH)

@Composable
fun NextConcertScreen(next: NextConcertUi) {
    val concert = next.concert
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column {
                Text(concert.title, style = MaterialTheme.typography.headlineSmall)
                Text("${concert.date.format(dateFormat)} · ${daysLabel(next.daysUntil)}", style = MaterialTheme.typography.bodyLarge)
                concert.venue?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("Pieces", concert.pieceCount.toString())
                Stat("Duration", concert.totalDuration)
                Stat("Avg. difficulty", concert.averageDifficulty?.toString() ?: "–")
            }
        }
        if (concert.warnings.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Not ready (${concert.warnings.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        concert.warnings.forEach { warning ->
                            Text(
                                "${warning.title}: ${warning.message}",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }
        }
        item { Text("Program", style = MaterialTheme.typography.titleMedium) }
        if (concert.program.isEmpty()) {
            item { Text("The program is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(concert.program, key = { it.position }) { entry ->
            ListItem(
                leadingContent = { Text("${entry.position}.", fontWeight = FontWeight.Bold) },
                headlineContent = { Text(entry.title) },
                trailingContent = { Text(formatDuration(entry.durationSeconds)) },
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun daysLabel(days: Long) = when (days) {
    0L -> "today"
    1L -> "tomorrow"
    else -> "in $days days"
}
