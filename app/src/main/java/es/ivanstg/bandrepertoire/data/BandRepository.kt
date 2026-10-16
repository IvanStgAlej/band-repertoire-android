package es.ivanstg.bandrepertoire.data

import es.ivanstg.bandrepertoire.data.model.AttendanceMark
import es.ivanstg.bandrepertoire.data.model.ConcertSummary
import es.ivanstg.bandrepertoire.data.model.Ensemble
import es.ivanstg.bandrepertoire.data.model.Member
import es.ivanstg.bandrepertoire.data.model.Piece
import es.ivanstg.bandrepertoire.data.model.Rehearsal
import es.ivanstg.bandrepertoire.data.model.StalePiece
import java.time.LocalDate

/** Outcome of a repository call. A failure carries a message that is safe to show to the user. */
sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val message: String) : ApiResult<Nothing>
}

interface BandRepository {
    suspend fun ensembles(): ApiResult<List<Ensemble>>
    suspend fun searchPieces(ensembleId: Long, query: String, maxDifficulty: Int?): ApiResult<List<Piece>>
    suspend fun stalePieces(ensembleId: Long): ApiResult<List<StalePiece>>

    /** The earliest concert dated today or later, or null when there is none. */
    suspend fun nextConcert(ensembleId: Long, today: LocalDate): ApiResult<ConcertSummary?>
    suspend fun rehearsals(ensembleId: Long): ApiResult<List<Rehearsal>>
    suspend fun members(ensembleId: Long): ApiResult<List<Member>>
    suspend fun saveAttendance(rehearsalId: Long, marks: List<AttendanceMark>): ApiResult<Rehearsal>
}
