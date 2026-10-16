package es.ivanstg.bandrepertoire.data.model

import java.time.LocalDate
import java.time.LocalDateTime

// Domain models: what the screens work with. They never see the API's DTOs.

data class Ensemble(val id: Long, val name: String, val type: String, val city: String?)

data class Piece(
    val id: Long,
    val title: String,
    val composer: String?,
    val durationSeconds: Int,
    val difficulty: Int,
    val genre: String?,
)

/** {@code lastRehearsedAt} and {@code weeksSince} are null when the piece was never rehearsed. */
data class StalePiece(val id: Long, val title: String, val lastRehearsedAt: LocalDateTime?, val weeksSince: Long?)

data class ProgramEntry(val position: Int, val title: String, val durationSeconds: Int, val difficulty: Int)

data class PreparationWarning(val pieceId: Long, val title: String, val message: String)

data class ConcertSummary(
    val id: Long,
    val title: String,
    val date: LocalDate,
    val venue: String?,
    val pieceCount: Int,
    val totalDuration: String,
    val averageDifficulty: Double?,
    val warnings: List<PreparationWarning>,
    val program: List<ProgramEntry>,
)

data class AttendanceRecord(val musicianId: Long, val attended: Boolean, val reason: String?)

data class Rehearsal(
    val id: Long,
    val startsAt: LocalDateTime,
    val location: String?,
    val attendance: List<AttendanceRecord>,
)

data class Member(val musicianId: Long, val name: String, val instrument: String)

/** One line of an attendance save: {@code reason} is only meaningful when the musician was absent. */
data class AttendanceMark(val musicianId: Long, val attended: Boolean, val reason: String?)

/** The ensemble the app is working with, remembered between launches. */
data class SelectedEnsemble(val id: Long, val name: String)

/** "M:SS", or "H:MM:SS" from one hour up, like the API's own format. */
fun formatDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = totalSeconds % 3600 / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}
