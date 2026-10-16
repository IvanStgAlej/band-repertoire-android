package es.ivanstg.bandrepertoire.ui.repertoire

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.ivanstg.bandrepertoire.data.model.Piece
import es.ivanstg.bandrepertoire.data.model.StalePiece
import es.ivanstg.bandrepertoire.data.model.formatDuration
import es.ivanstg.bandrepertoire.ui.UiState
import es.ivanstg.bandrepertoire.ui.appViewModel
import es.ivanstg.bandrepertoire.ui.components.EmptyView
import es.ivanstg.bandrepertoire.ui.components.LoadingView
import es.ivanstg.bandrepertoire.ui.components.StateContent

@Composable
fun RepertoireRoute(ensembleId: Long) {
    val viewModel = appViewModel(key = "repertoire-$ensembleId") { RepertoireViewModel(it.repository, ensembleId) }
    val query by viewModel.query.collectAsStateWithLifecycle()
    val maxDifficulty by viewModel.maxDifficulty.collectAsStateWithLifecycle()
    val pieces by viewModel.pieces.collectAsStateWithLifecycle()
    val stale by viewModel.stale.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Search") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Forgotten") })
        }
        if (tab == 0) {
            SearchTab(query, maxDifficulty, pieces, viewModel::onQueryChange, viewModel::onDifficultyChange, viewModel::retry)
        } else {
            LaunchedEffect(Unit) { viewModel.loadStale() }
            ForgottenTab(stale, viewModel::retryStale)
        }
    }
}

private val difficultyFilters = listOf<Pair<String, Int?>>("Any" to null, "≤ 2" to 2, "≤ 3" to 3, "≤ 4" to 4)

@Composable
private fun SearchTab(
    query: String,
    maxDifficulty: Int?,
    pieces: UiState<List<Piece>>,
    onQueryChange: (String) -> Unit,
    onDifficultyChange: (Int?) -> Unit,
    onRetry: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Title or composer") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            difficultyFilters.forEach { (label, value) ->
                FilterChip(selected = maxDifficulty == value, onClick = { onDifficultyChange(value) }, label = { Text(label) })
            }
        }
        StateContent(pieces, onRetry) { list ->
            if (list.isEmpty()) {
                EmptyView("No pieces match", hint = "Try another search or difficulty.")
            } else {
                LazyColumn {
                    items(list, key = { it.id }) { piece ->
                        ListItem(
                            headlineContent = { Text(piece.title) },
                            supportingContent = { Text(listOfNotNull(piece.composer, piece.genre).joinToString(" · ")) },
                            trailingContent = {
                                Column {
                                    Text(formatDuration(piece.durationSeconds), style = MaterialTheme.typography.labelLarge)
                                    Text(difficultyDots(piece.difficulty), style = MaterialTheme.typography.labelSmall)
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ForgottenTab(stale: UiState<List<StalePiece>>?, onRetry: () -> Unit) {
    if (stale == null) {
        LoadingView()
        return
    }
    StateContent(stale, onRetry) { list ->
        if (list.isEmpty()) {
            EmptyView("Nothing forgotten", hint = "Every piece was rehearsed in the last 4 weeks.")
        } else {
            LazyColumn {
                items(list, key = { it.id }) { piece ->
                    ListItem(
                        headlineContent = { Text(piece.title) },
                        supportingContent = {
                            Text(
                                when (val weeks = piece.weeksSince) {
                                    null -> "Never rehearsed"
                                    1L -> "Last rehearsed 1 week ago"
                                    else -> "Last rehearsed $weeks weeks ago"
                                },
                            )
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun difficultyDots(difficulty: Int) = "●".repeat(difficulty) + "○".repeat(5 - difficulty)
