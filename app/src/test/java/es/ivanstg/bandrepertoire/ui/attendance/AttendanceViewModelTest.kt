package es.ivanstg.bandrepertoire.ui.attendance

import es.ivanstg.bandrepertoire.FakeBandRepository
import es.ivanstg.bandrepertoire.MainDispatcherRule
import es.ivanstg.bandrepertoire.data.ApiResult
import es.ivanstg.bandrepertoire.data.model.AttendanceMark
import es.ivanstg.bandrepertoire.data.model.AttendanceRecord
import es.ivanstg.bandrepertoire.data.model.Member
import es.ivanstg.bandrepertoire.data.model.Rehearsal
import es.ivanstg.bandrepertoire.ui.UiState
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AttendanceViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val zone = ZoneId.of("Europe/Madrid")
    private val now = LocalDateTime.of(2026, 10, 16, 12, 0)
    private val clock = Clock.fixed(now.atZone(zone).toInstant(), zone)
    private val repository = FakeBandRepository()

    private val ana = Member(1, "Ana Torres", "Trumpet")
    private val hugo = Member(8, "Hugo Castro", "Percussion")

    private val twelveDaysAgo = Rehearsal(3, now.minusDays(12), "Town hall", listOf(AttendanceRecord(1, false, "Sick")))
    private val fiveDaysAgo = Rehearsal(5, now.minusDays(5), "Music school", listOf(AttendanceRecord(8, false, "Work shift")))
    private val inThreeDays = Rehearsal(6, now.plusDays(3), "Town hall", emptyList())

    private fun TestScope.loaded(
        rehearsals: List<Rehearsal> = listOf(inThreeDays, fiveDaysAgo, twelveDaysAgo),
        members: List<Member> = listOf(ana, hugo),
    ): AttendanceViewModel {
        repository.rehearsalsResult = ApiResult.Success(rehearsals)
        repository.membersResult = ApiResult.Success(members)
        val viewModel = AttendanceViewModel(repository, ensembleId = 1, clock = clock)
        advanceUntilIdle()
        return viewModel
    }

    private fun AttendanceViewModel.ui() = (state.value as UiState.Content<AttendanceUi>).data

    @Test
    fun defaultsToTheLatestStartedRehearsal() = runTest {
        assertEquals(5L, loaded().ui().selectedRehearsalId)
    }

    @Test
    fun defaultsToTheNextOneWhenNoneHasStarted() = runTest {
        val inTenDays = Rehearsal(7, now.plusDays(10), null, emptyList())

        assertEquals(6L, loaded(rehearsals = listOf(inTenDays, inThreeDays)).ui().selectedRehearsalId)
    }

    @Test
    fun existingRecordsPrefillTheSwitches() = runTest {
        val rows = loaded().ui().rows

        assertEquals(
            listOf(
                MemberRow(1, "Ana Torres", "Trumpet", attended = true, reason = ""),
                MemberRow(8, "Hugo Castro", "Percussion", attended = false, reason = "Work shift"),
            ),
            rows,
        )
    }

    @Test
    fun saveIsDisabledUntilSomethingChanges() = runTest {
        val viewModel = loaded()
        assertFalse(viewModel.ui().canSave)

        viewModel.setAttended(1, false)
        assertTrue(viewModel.ui().canSave)

        viewModel.setAttended(1, true)
        assertFalse(viewModel.ui().canSave)
    }

    @Test
    fun savingSendsOneEntryPerMemberWithReasons() = runTest {
        val viewModel = loaded()

        viewModel.setAttended(1, false)
        viewModel.setReason(1, "  Sick  ")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(
            listOf(5L to listOf(AttendanceMark(1, false, "Sick"), AttendanceMark(8, false, "Work shift"))),
            repository.savedMarks,
        )
        assertEquals("Attendance saved", viewModel.ui().message)
        assertFalse(viewModel.ui().canSave)
    }

    @Test
    fun presentMembersAreSentWithoutAReason() = runTest {
        val viewModel = loaded()

        viewModel.setAttended(8, true)
        viewModel.save()
        advanceUntilIdle()

        assertEquals(listOf(AttendanceMark(1, true, null), AttendanceMark(8, true, null)), repository.savedMarks.single().second)
    }

    @Test
    fun reasonIsTrimmedAndCappedAt200Characters() = runTest {
        val viewModel = loaded()

        viewModel.setAttended(1, false)
        viewModel.setReason(1, " " + "x".repeat(250))

        assertEquals(200, viewModel.ui().rows.first { it.musicianId == 1L }.reason.length)
        viewModel.save()
        advanceUntilIdle()
        assertEquals("x".repeat(199), repository.savedMarks.single().second.first().reason)
    }

    @Test
    fun apiRuleErrorIsShownToTheUser() = runTest {
        val viewModel = loaded()
        repository.saveFailure = ApiResult.Failure("Musician 1 is not a member of ensemble 1")

        viewModel.setAttended(1, false)
        viewModel.save()
        advanceUntilIdle()

        assertEquals("Musician 1 is not a member of ensemble 1", viewModel.ui().message)
        assertTrue("unsaved marks are kept so the user can retry", viewModel.ui().canSave)

        viewModel.messageShown()
        assertNull(viewModel.ui().message)
    }

    @Test
    fun switchingRehearsalDiscardsUnsavedMarksAfterConfirmation() = runTest {
        val viewModel = loaded()
        viewModel.setAttended(1, false)

        viewModel.selectRehearsal(3)
        assertEquals(3L, viewModel.ui().pendingRehearsalId)
        assertEquals(5L, viewModel.ui().selectedRehearsalId)

        viewModel.cancelRehearsalSwitch()
        assertNull(viewModel.ui().pendingRehearsalId)
        assertTrue(viewModel.ui().canSave)

        viewModel.selectRehearsal(3)
        viewModel.confirmRehearsalSwitch()
        assertEquals(3L, viewModel.ui().selectedRehearsalId)
        assertEquals("Sick", viewModel.ui().rows.first { it.musicianId == 1L }.reason)
        assertFalse(viewModel.ui().canSave)
    }

    @Test
    fun switchingWithoutChangesNeedsNoConfirmation() = runTest {
        val viewModel = loaded()

        viewModel.selectRehearsal(6)

        assertEquals(6L, viewModel.ui().selectedRehearsalId)
        assertNull(viewModel.ui().pendingRehearsalId)
    }

    @Test
    fun noRehearsalsAndNoMembersShowEmptyStates() = runTest {
        val noRehearsals = loaded(rehearsals = emptyList()).ui()
        assertNull(noRehearsals.selectedRehearsalId)
        assertFalse(noRehearsals.canSave)

        val noMembers = loaded(members = emptyList()).ui()
        assertTrue(noMembers.rows.isEmpty())
        assertFalse(noMembers.canSave)
    }

    @Test
    fun errorThenRetry() = runTest {
        repository.rehearsalsResult = ApiResult.Success(listOf(fiveDaysAgo))
        repository.membersResult = ApiResult.Failure("Cannot reach the API")
        val viewModel = AttendanceViewModel(repository, ensembleId = 1, clock = clock)
        advanceUntilIdle()
        assertEquals(UiState.Error("Cannot reach the API"), viewModel.state.value)

        repository.membersResult = ApiResult.Success(listOf(ana))
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(5L, viewModel.ui().selectedRehearsalId)
    }
}
