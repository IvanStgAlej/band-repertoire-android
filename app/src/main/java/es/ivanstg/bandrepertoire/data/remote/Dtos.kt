package es.ivanstg.bandrepertoire.data.remote

import kotlinx.serialization.Serializable

// Mirrors of the API's JSON. Dates stay as ISO strings here and are parsed when mapping to domain models.

@Serializable
data class PageDto<T>(val content: List<T>, val page: Int, val size: Int, val totalElements: Long, val totalPages: Int)

@Serializable
data class EnsembleDto(val id: Long, val name: String, val type: String, val city: String? = null)

@Serializable
data class PieceDto(
    val id: Long,
    val ensembleId: Long,
    val title: String,
    val composer: String? = null,
    val arranger: String? = null,
    val durationSeconds: Int,
    val difficulty: Int,
    val genre: String? = null,
)

@Serializable
data class StalePieceDto(
    val pieceId: Long,
    val title: String,
    val lastRehearsedAt: String? = null,
    val weeksSinceLastRehearsal: Long? = null,
)

@Serializable
data class ProgramEntryDto(val position: Int, val pieceId: Long, val title: String, val durationSeconds: Int, val difficulty: Int)

@Serializable
data class ConcertDto(
    val id: Long,
    val ensembleId: Long,
    val title: String,
    val date: String,
    val venue: String? = null,
    val program: List<ProgramEntryDto> = emptyList(),
)

@Serializable
data class PreparationWarningDto(val pieceId: Long, val title: String, val lastRehearsedAt: String? = null, val message: String)

@Serializable
data class ConcertSummaryDto(
    val concertId: Long,
    val title: String,
    val date: String,
    val pieceCount: Int,
    val totalDurationSeconds: Int,
    val totalDuration: String,
    val averageDifficulty: Double? = null,
    val warnings: List<PreparationWarningDto> = emptyList(),
)

@Serializable
data class PieceRefDto(val id: Long, val title: String)

@Serializable
data class AttendanceDto(val musicianId: Long, val name: String, val attended: Boolean, val reason: String? = null)

@Serializable
data class RehearsalDto(
    val id: Long,
    val ensembleId: Long,
    val startsAt: String,
    val location: String? = null,
    val notes: String? = null,
    val pieces: List<PieceRefDto> = emptyList(),
    val attendance: List<AttendanceDto> = emptyList(),
)

@Serializable
data class MemberDto(val musicianId: Long, val name: String, val email: String, val instrument: String, val joinedOn: String)

@Serializable
data class AttendanceEntryDto(val musicianId: Long, val attended: Boolean, val reason: String? = null)

@Serializable
data class AttendanceRequestDto(val entries: List<AttendanceEntryDto>)

/** RFC 9457 problem detail, the API's error body. */
@Serializable
data class ProblemDetailDto(val title: String? = null, val status: Int? = null, val detail: String? = null)
