package es.ivanstg.bandrepertoire.ui.ensemble

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.ivanstg.bandrepertoire.data.BandRepository
import es.ivanstg.bandrepertoire.data.EnsembleSelection
import es.ivanstg.bandrepertoire.data.model.Ensemble
import es.ivanstg.bandrepertoire.data.model.SelectedEnsemble
import es.ivanstg.bandrepertoire.ui.UiState
import es.ivanstg.bandrepertoire.ui.toUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EnsemblePickerViewModel(
    private val repository: BandRepository,
    private val selection: EnsembleSelection,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<Ensemble>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Ensemble>>> = _state.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    fun select(ensemble: Ensemble) {
        viewModelScope.launch { selection.select(SelectedEnsemble(ensemble.id, ensemble.name)) }
    }

    private fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch { _state.value = repository.ensembles().toUiState() }
    }
}
