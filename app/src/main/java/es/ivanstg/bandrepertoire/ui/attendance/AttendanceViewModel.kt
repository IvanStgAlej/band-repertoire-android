package es.ivanstg.bandrepertoire.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.ivanstg.bandrepertoire.data.ApiResult
import es.ivanstg.bandrepertoire.data.BandRepository
import es.ivanstg.bandrepertoire.data.model.AttendanceMark
import es.ivanstg.bandrepertoire.data.model.Member
import es.ivanstg.bandrepertoire.data.model.Rehearsal
import es.ivanstg.bandrepertoire.ui.UiState
import java.time.Clock
import java.time.LocalDateTime
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One member's line on the attendance sheet. */
data class MemberRow(
    val musicianId: Long,
    val name: String,
    val instrument: String,
    val attended: Boolean,
    val reason: String,
)

data class AttendanceUi(
    val rehearsals: List<Rehearsal>,
    /** Null when the ensemble has no rehearsals yet. */
    val selectedRehearsalId: Long?,
    val rows: List<MemberRow>,
    /** The rows as they are on the server, to know whether there is anything to save. */
    val savedRows: List<MemberRow>,
    val saving: Boolean = false,
    /** Set while the "discard unsaved changes?" dialog is open. */
    val pendingRehearsalId: Long? = null,
    /** A one-off message for the snackbar. */
    val message: String? = null,
) {
    val hasChanges: Boolean get() = rows != savedRows
    val canSave: Boolean get() = selectedRehearsalId != null && hasChanges && !saving
}

/**
 * The attendance sheet of one rehearsal. Unsaved marks live here, not in the composable, so they survive a
 * configuration change such as rotating the phone.
 */
class AttendanceViewModel(
    private val repository: BandRepository,
    private val ensembleId: Long,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<AttendanceUi>>(UiState.Loading)
    val state: StateFlow<UiState<AttendanceUi>> = _state.asStateFlow()

    private var members: List<Member> = emptyList()

    init {
        load()
    }

    fun retry() = load()

    fun setAttended(musicianId: Long, attended: Boolean) = updateRow(musicianId) { it.copy(attended = attended) }

    fun setReason(musicianId: Long, reason: String) =
        updateRow(musicianId) { it.copy(reason = reason.take(MAX_REASON_LENGTH)) }

    fun selectRehearsal(rehearsalId: Long) {
        val ui = currentUi() ?: return
        if (ui.hasChanges) {
            updateUi { it.copy(pendingRehearsalId = rehearsalId) }
        } else {
            switchTo(rehearsalId)
        }
    }

    fun confirmRehearsalSwitch() {
        currentUi()?.pendingRehearsalId?.let(::switchTo)
    }

    fun cancelRehearsalSwitch() = updateUi { it.copy(pendingRehearsalId = null) }

    fun messageShown() = updateUi { it.copy(message = null) }

    fun save() {
        val ui = currentUi() ?: return
        val rehearsalId = ui.selectedRehearsalId ?: return
        if (!ui.canSave) return
        val marks = ui.rows.map { row ->
            AttendanceMark(row.musicianId, row.attended, if (row.attended) null else row.reason.trim().ifEmpty { null })
        }
        updateUi { it.copy(saving = true) }
        viewModelScope.launch {
            when (val result = repository.saveAttendance(rehearsalId, marks)) {
                is ApiResult.Success -> updateUi { current ->
                    val rehearsals = current.rehearsals.map { if (it.id == rehearsalId) result.value else it }
                    val rows = rowsFor(result.value)
                    current.copy(rehearsals = rehearsals, rows = rows, savedRows = rows, saving = false, message = "Attendance saved")
                }
                is ApiResult.Failure -> updateUi { it.copy(saving = false, message = result.message) }
            }
        }
    }

    private fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            val rehearsalsCall = async { repository.rehearsals(ensembleId) }
            val membersCall = async { repository.members(ensembleId) }
            val rehearsals = rehearsalsCall.await()
            val loadedMembers = membersCall.await()
            _state.value = when {
                rehearsals is ApiResult.Failure -> UiState.Error(rehearsals.message)
                loadedMembers is ApiResult.Failure -> UiState.Error(loadedMembers.message)
                else -> {
                    members = (loadedMembers as ApiResult.Success).value
                    val list = (rehearsals as ApiResult.Success).value
                    val selected = defaultRehearsal(list)
                    val rows = selected?.let(::rowsFor) ?: emptyList()
                    UiState.Content(AttendanceUi(list, selected?.id, rows, rows))
                }
            }
        }
    }

    /** The latest rehearsal that has already started, or the next one if none has started yet. */
    private fun defaultRehearsal(rehearsals: List<Rehearsal>): Rehearsal? {
        val now = LocalDateTime.now(clock)
        return rehearsals.filter { !it.startsAt.isAfter(now) }.maxByOrNull { it.startsAt }
            ?: rehearsals.minByOrNull { it.startsAt }
    }

    /** Existing records prefill the sheet; members without a record start as present. */
    private fun rowsFor(rehearsal: Rehearsal): List<MemberRow> = members.map { member ->
        val record = rehearsal.attendance.firstOrNull { it.musicianId == member.musicianId }
        MemberRow(member.musicianId, member.name, member.instrument, record?.attended ?: true, record?.reason.orEmpty())
    }

    private fun switchTo(rehearsalId: Long) = updateUi { ui ->
        val rehearsal = ui.rehearsals.firstOrNull { it.id == rehearsalId } ?: return@updateUi ui.copy(pendingRehearsalId = null)
        val rows = rowsFor(rehearsal)
        ui.copy(selectedRehearsalId = rehearsalId, rows = rows, savedRows = rows, pendingRehearsalId = null)
    }

    private fun updateRow(musicianId: Long, change: (MemberRow) -> MemberRow) = updateUi { ui ->
        ui.copy(rows = ui.rows.map { if (it.musicianId == musicianId) change(it) else it })
    }

    private fun currentUi(): AttendanceUi? = (_state.value as? UiState.Content)?.data

    private fun updateUi(change: (AttendanceUi) -> AttendanceUi) = _state.update { state ->
        if (state is UiState.Content) UiState.Content(change(state.data)) else state
    }

    companion object {
        /** The API's limit for an absence reason. */
        const val MAX_REASON_LENGTH = 200
    }
}
