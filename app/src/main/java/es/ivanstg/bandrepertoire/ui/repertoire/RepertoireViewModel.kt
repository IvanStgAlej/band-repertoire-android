package es.ivanstg.bandrepertoire.ui.repertoire

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.ivanstg.bandrepertoire.data.BandRepository
import es.ivanstg.bandrepertoire.data.model.Piece
import es.ivanstg.bandrepertoire.data.model.StalePiece
import es.ivanstg.bandrepertoire.ui.UiState
import es.ivanstg.bandrepertoire.ui.toUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class RepertoireViewModel(private val repository: BandRepository, private val ensembleId: Long) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _maxDifficulty = MutableStateFlow<Int?>(null)
    val maxDifficulty: StateFlow<Int?> = _maxDifficulty.asStateFlow()

    private val retries = MutableStateFlow(0)

    /**
     * Search results. Typing is debounced so a request is not sent per keystroke, and flatMapLatest cancels the
     * previous request, so a slow older response can never overwrite a newer one.
     */
    val pieces: StateFlow<UiState<List<Piece>>> =
        combine(_query.debounce(SEARCH_DEBOUNCE_MS), _maxDifficulty, retries) { query, difficulty, _ -> query to difficulty }
            .flatMapLatest { (query, difficulty) ->
                flow {
                    emit(UiState.Loading)
                    emit(repository.searchPieces(ensembleId, query, difficulty).toUiState())
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private val _stale = MutableStateFlow<UiState<List<StalePiece>>?>(null)

    /** Pieces not rehearsed for 4 weeks; null until the Forgotten tab is first opened. */
    val stale: StateFlow<UiState<List<StalePiece>>?> = _stale.asStateFlow()

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun onDifficultyChange(maxDifficulty: Int?) {
        _maxDifficulty.value = maxDifficulty
    }

    fun retry() {
        retries.value++
    }

    fun loadStale() {
        if (_stale.value == null) retryStale()
    }

    fun retryStale() {
        _stale.value = UiState.Loading
        viewModelScope.launch { _stale.value = repository.stalePieces(ensembleId).toUiState() }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
