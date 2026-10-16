package es.ivanstg.bandrepertoire.ui.ensemble

import es.ivanstg.bandrepertoire.FakeBandRepository
import es.ivanstg.bandrepertoire.FakeEnsembleSelection
import es.ivanstg.bandrepertoire.MainDispatcherRule
import es.ivanstg.bandrepertoire.data.ApiResult
import es.ivanstg.bandrepertoire.data.model.Ensemble
import es.ivanstg.bandrepertoire.data.model.SelectedEnsemble
import es.ivanstg.bandrepertoire.ui.UiState
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class EnsemblePickerViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeBandRepository()
    private val selection = FakeEnsembleSelection()
    private val band = Ensemble(1, "Riverside Concert Band", "BAND", "Valencia")
    private val bigBand = Ensemble(2, "Night Owls Big Band", "BIG_BAND", "Valencia")

    @Test
    fun loadsEnsembles() = runTest {
        repository.ensemblesResult = ApiResult.Success(listOf(band, bigBand))

        val viewModel = EnsemblePickerViewModel(repository, selection)
        assertEquals(UiState.Loading, viewModel.state.value)
        advanceUntilIdle()

        assertEquals(UiState.Content(listOf(band, bigBand)), viewModel.state.value)
    }

    @Test
    fun showsErrorAndRetryReloads() = runTest {
        repository.ensemblesResult = ApiResult.Failure("Cannot reach the API")
        val viewModel = EnsemblePickerViewModel(repository, selection)
        advanceUntilIdle()
        assertEquals(UiState.Error("Cannot reach the API"), viewModel.state.value)

        repository.ensemblesResult = ApiResult.Success(listOf(band))
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(UiState.Content(listOf(band)), viewModel.state.value)
        assertEquals(2, repository.ensemblesCalls)
    }

    @Test
    fun selectingSavesTheEnsemble() = runTest {
        repository.ensemblesResult = ApiResult.Success(listOf(band, bigBand))
        val viewModel = EnsemblePickerViewModel(repository, selection)
        advanceUntilIdle()

        viewModel.select(bigBand)
        advanceUntilIdle()

        assertEquals(SelectedEnsemble(2, "Night Owls Big Band"), selection.selected.value)
    }
}
