package es.ivanstg.bandrepertoire.ui.repertoire

import es.ivanstg.bandrepertoire.FakeBandRepository
import es.ivanstg.bandrepertoire.MainDispatcherRule
import es.ivanstg.bandrepertoire.data.ApiResult
import es.ivanstg.bandrepertoire.data.model.Piece
import es.ivanstg.bandrepertoire.data.model.StalePiece
import es.ivanstg.bandrepertoire.ui.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RepertoireViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeBandRepository()
    private val arsenal = Piece(1, "Arsenal", "Jan Van der Roost", 270, 3, "March")
    private val caminoReal = Piece(2, "El Camino Real", "Alfred Reed", 660, 4, "Concert band")

    /** The pieces flow only runs while someone collects it, like the screen does. */
    private fun TestScope.collecting(viewModel: RepertoireViewModel): RepertoireViewModel {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.pieces.collect {} }
        return viewModel
    }

    @Test
    fun typingIsDebounced() = runTest {
        val viewModel = collecting(RepertoireViewModel(repository, ensembleId = 1))

        viewModel.onQueryChange("r")
        advanceTimeBy(100)
        viewModel.onQueryChange("re")
        advanceTimeBy(100)
        viewModel.onQueryChange("reed")
        advanceUntilIdle()

        assertEquals(listOf("reed" to null), repository.searchCalls)
    }

    @Test
    fun aSlowOlderSearchNeverOverwritesANewerOne() = runTest {
        repository.searchHandler = { query, _ ->
            if (query == "old") {
                delay(1_000)
                ApiResult.Success(listOf(arsenal))
            } else {
                ApiResult.Success(listOf(caminoReal))
            }
        }
        val viewModel = collecting(RepertoireViewModel(repository, ensembleId = 1))

        viewModel.onQueryChange("old")
        advanceTimeBy(400) // debounce over, the slow "old" request is in flight
        viewModel.onQueryChange("new")
        advanceUntilIdle()

        assertEquals(UiState.Content(listOf(caminoReal)), viewModel.pieces.value)
    }

    @Test
    fun difficultyFilterIsSent() = runTest {
        val viewModel = collecting(RepertoireViewModel(repository, ensembleId = 1))

        viewModel.onDifficultyChange(3)
        advanceUntilIdle()

        assertEquals("" to 3, repository.searchCalls.last())
        assertEquals(3, viewModel.maxDifficulty.value)
    }

    @Test
    fun emptyResultShowsEmptyState() = runTest {
        val viewModel = collecting(RepertoireViewModel(repository, ensembleId = 1))
        advanceUntilIdle()

        assertEquals(UiState.Content(emptyList<Piece>()), viewModel.pieces.value)
    }

    @Test
    fun errorThenRetry() = runTest {
        var fail = true
        repository.searchHandler = { _, _ ->
            if (fail) ApiResult.Failure("Cannot reach the API") else ApiResult.Success(listOf(arsenal))
        }
        val viewModel = collecting(RepertoireViewModel(repository, ensembleId = 1))
        advanceUntilIdle()
        assertEquals(UiState.Error("Cannot reach the API"), viewModel.pieces.value)

        fail = false
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(UiState.Content(listOf(arsenal)), viewModel.pieces.value)
    }

    @Test
    fun forgottenTabLoadsStalePiecesOnce() = runTest {
        val never = StalePiece(3, "Lincolnshire Posy", lastRehearsedAt = null, weeksSince = null)
        repository.staleResult = ApiResult.Success(listOf(never))
        val viewModel = RepertoireViewModel(repository, ensembleId = 1)

        viewModel.loadStale()
        advanceUntilIdle()
        viewModel.loadStale()
        advanceUntilIdle()

        assertEquals(UiState.Content(listOf(never)), viewModel.stale.value)
        assertEquals(1, repository.staleCalls)

        viewModel.retryStale()
        advanceUntilIdle()
        assertEquals(2, repository.staleCalls)
    }
}
