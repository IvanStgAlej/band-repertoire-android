package es.ivanstg.bandrepertoire.ui.concert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.ivanstg.bandrepertoire.data.ApiResult
import es.ivanstg.bandrepertoire.data.BandRepository
import es.ivanstg.bandrepertoire.data.model.ConcertSummary
import es.ivanstg.bandrepertoire.ui.UiState
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NextConcertUi(val concert: ConcertSummary, val daysUntil: Long)

/** The next concert and how ready it is. Content(null) means there is no upcoming concert. */
class NextConcertViewModel(
    private val repository: BandRepository,
    private val ensembleId: Long,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<NextConcertUi?>>(UiState.Loading)
    val state: StateFlow<UiState<NextConcertUi?>> = _state.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            val today = LocalDate.now(clock)
            _state.value = when (val result = repository.nextConcert(ensembleId, today)) {
                is ApiResult.Failure -> UiState.Error(result.message)
                is ApiResult.Success -> UiState.Content(
                    result.value?.let { NextConcertUi(it, ChronoUnit.DAYS.between(today, it.date)) },
                )
            }
        }
    }
}
