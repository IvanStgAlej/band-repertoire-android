package es.ivanstg.bandrepertoire.data

import es.ivanstg.bandrepertoire.data.model.AttendanceMark
import es.ivanstg.bandrepertoire.data.model.AttendanceRecord
import es.ivanstg.bandrepertoire.data.model.ConcertSummary
import es.ivanstg.bandrepertoire.data.model.Ensemble
import es.ivanstg.bandrepertoire.data.model.Member
import es.ivanstg.bandrepertoire.data.model.Piece
import es.ivanstg.bandrepertoire.data.model.PreparationWarning
import es.ivanstg.bandrepertoire.data.model.ProgramEntry
import es.ivanstg.bandrepertoire.data.model.Rehearsal
import es.ivanstg.bandrepertoire.data.model.StalePiece
import es.ivanstg.bandrepertoire.data.remote.ApiService
import es.ivanstg.bandrepertoire.data.remote.AttendanceEntryDto
import es.ivanstg.bandrepertoire.data.remote.AttendanceRequestDto
import es.ivanstg.bandrepertoire.data.remote.ProblemDetailDto
import es.ivanstg.bandrepertoire.data.remote.RehearsalDto
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

class NetworkBandRepository(private val api: ApiService, private val baseUrl: String) : BandRepository {

    override suspend fun ensembles() = call {
        api.ensembles().content.map { Ensemble(it.id, it.name, it.type, it.city) }
    }

    override suspend fun searchPieces(ensembleId: Long, query: String, maxDifficulty: Int?) = call {
        api.searchPieces(ensembleId, query.trim().ifEmpty { null }, maxDifficulty).content.map {
            Piece(it.id, it.title, it.composer, it.durationSeconds, it.difficulty, it.genre)
        }
    }

    override suspend fun stalePieces(ensembleId: Long) = call {
        api.stalePieces(ensembleId).map {
            StalePiece(it.pieceId, it.title, it.lastRehearsedAt?.let(LocalDateTime::parse), it.weeksSinceLastRehearsal)
        }
    }

    // The API has no "upcoming" filter, so the client picks it from the 20 most recent concerts (see README).
    override suspend fun nextConcert(ensembleId: Long, today: LocalDate) = call {
        val next = api.concerts(ensembleId).content
            .map { it to LocalDate.parse(it.date) }
            .filter { (_, date) -> !date.isBefore(today) }
            .minByOrNull { (_, date) -> date }
            ?: return@call null
        val (concert, date) = next
        val summary = api.concertSummary(concert.id)
        ConcertSummary(
            id = concert.id,
            title = concert.title,
            date = date,
            venue = concert.venue,
            pieceCount = summary.pieceCount,
            totalDuration = summary.totalDuration,
            averageDifficulty = summary.averageDifficulty,
            warnings = summary.warnings.map { PreparationWarning(it.pieceId, it.title, it.message) },
            program = concert.program.map { ProgramEntry(it.position, it.title, it.durationSeconds, it.difficulty) },
        )
    }

    override suspend fun rehearsals(ensembleId: Long) = call {
        api.rehearsals(ensembleId).content.map { it.toRehearsal() }
    }

    override suspend fun members(ensembleId: Long) = call {
        api.members(ensembleId).map { Member(it.musicianId, it.name, it.instrument) }
    }

    override suspend fun saveAttendance(rehearsalId: Long, marks: List<AttendanceMark>) = call {
        val body = AttendanceRequestDto(marks.map { AttendanceEntryDto(it.musicianId, it.attended, it.reason) })
        api.recordAttendance(rehearsalId, body).toRehearsal()
    }

    private fun RehearsalDto.toRehearsal() = Rehearsal(
        id = id,
        startsAt = LocalDateTime.parse(startsAt),
        location = location,
        attendance = attendance.map { AttendanceRecord(it.musicianId, it.attended, it.reason) },
    )

    /** Runs an API call and turns every failure into a message that is safe to show: no stack traces, no internals. */
    private suspend fun <T> call(block: suspend () -> T): ApiResult<T> = try {
        ApiResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        ApiResult.Failure(problemDetail(e) ?: "Server error (HTTP ${e.code()})")
    } catch (e: IOException) {
        ApiResult.Failure("Cannot reach the API at $baseUrl. Is it running?")
    } catch (e: Exception) {
        ApiResult.Failure("Something went wrong. Please try again.")
    }

    private fun problemDetail(e: HttpException): String? {
        val body = e.response()?.errorBody()?.string() ?: return null
        return runCatching { errorJson.decodeFromString<ProblemDetailDto>(body).detail }.getOrNull()
    }

    private companion object {
        val errorJson = Json { ignoreUnknownKeys = true }
    }
}
