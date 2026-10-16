package es.ivanstg.bandrepertoire.ui.concert

import es.ivanstg.bandrepertoire.FakeBandRepository
import es.ivanstg.bandrepertoire.MainDispatcherRule
import es.ivanstg.bandrepertoire.data.ApiResult
import es.ivanstg.bandrepertoire.data.model.ConcertSummary
import es.ivanstg.bandrepertoire.data.model.PreparationWarning
import es.ivanstg.bandrepertoire.data.model.ProgramEntry
import es.ivanstg.bandrepertoire.ui.UiState
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NextConcertViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val zone = ZoneId.of("Europe/Madrid")
    private val today = LocalDate.of(2026, 10, 16)
    private val clock = Clock.fixed(today.atTime(18, 0).atZone(zone).toInstant(), zone)
    private val repository = FakeBandRepository()

    private fun concertOn(date: LocalDate) = ConcertSummary(
        id = 1,
        title = "Autumn Concert",
        date = date,
        venue = "Palau de la Musica",
        pieceCount = 2,
        totalDuration = "15:00",
        averageDifficulty = 3.5,
        warnings = listOf(PreparationWarning(3, "Amparito Roca", "Never rehearsed before the concert")),
        program = listOf(ProgramEntry(1, "El Camino Real", 660, 4), ProgramEntry(2, "Amparito Roca", 240, 3)),
    )

    @Test
    fun showsTheSummaryOfTheNextConcert() = runTest {
        val concert = concertOn(LocalDate.of(2026, 10, 29))
        repository.nextConcertResult = ApiResult.Success(concert)

        val viewModel = NextConcertViewModel(repository, ensembleId = 1, clock = clock)
        advanceUntilIdle()

        assertEquals(UiState.Content(NextConcertUi(concert, daysUntil = 13)), viewModel.state.value)
        assertEquals(listOf(today), repository.nextConcertCalls)
    }

    @Test
    fun noUpcomingConcertShowsEmptyState() = runTest {
        repository.nextConcertResult = ApiResult.Success(null)

        val viewModel = NextConcertViewModel(repository, ensembleId = 1, clock = clock)
        advanceUntilIdle()

        assertEquals(UiState.Content(null), viewModel.state.value)
    }

    @Test
    fun aConcertTodayIsZeroDaysAway() = runTest {
        repository.nextConcertResult = ApiResult.Success(concertOn(today))

        val viewModel = NextConcertViewModel(repository, ensembleId = 1, clock = clock)
        advanceUntilIdle()

        assertEquals(0L, (viewModel.state.value as UiState.Content<NextConcertUi?>).data!!.daysUntil)
    }

    @Test
    fun errorThenRetry() = runTest {
        repository.nextConcertResult = ApiResult.Failure("Cannot reach the API")
        val viewModel = NextConcertViewModel(repository, ensembleId = 1, clock = clock)
        advanceUntilIdle()
        assertEquals(UiState.Error("Cannot reach the API"), viewModel.state.value)

        repository.nextConcertResult = ApiResult.Success(null)
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(UiState.Content(null), viewModel.state.value)
        assertEquals(2, repository.nextConcertCalls.size)
    }
}
