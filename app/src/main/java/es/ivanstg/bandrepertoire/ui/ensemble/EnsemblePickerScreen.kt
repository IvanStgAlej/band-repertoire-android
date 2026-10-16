package es.ivanstg.bandrepertoire.ui.ensemble

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.ivanstg.bandrepertoire.data.model.Ensemble
import es.ivanstg.bandrepertoire.ui.UiState
import es.ivanstg.bandrepertoire.ui.appViewModel
import es.ivanstg.bandrepertoire.ui.components.EmptyView
import es.ivanstg.bandrepertoire.ui.components.StateContent

@Composable
fun EnsemblePickerRoute(modifier: Modifier = Modifier) {
    val viewModel = appViewModel { EnsemblePickerViewModel(it.repository, it.ensembleSelection) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    EnsemblePickerScreen(state, onRetry = viewModel::retry, onSelect = viewModel::select, modifier = modifier)
}

@Composable
fun EnsemblePickerScreen(
    state: UiState<List<Ensemble>>,
    onRetry: () -> Unit,
    onSelect: (Ensemble) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Text(
            "Choose your ensemble",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
        )
        StateContent(state, onRetry) { ensembles ->
            if (ensembles.isEmpty()) {
                EmptyView("No ensembles yet", hint = "Create one in the API's Swagger UI, then retry.")
            } else {
                LazyColumn {
                    items(ensembles, key = { it.id }) { ensemble ->
                        ListItem(
                            headlineContent = { Text(ensemble.name) },
                            supportingContent = {
                                Text(listOfNotNull(ensemble.type.toDisplayType(), ensemble.city).joinToString(" · "))
                            },
                            modifier = Modifier.clickable { onSelect(ensemble) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

private fun String.toDisplayType() = lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
