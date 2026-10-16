package es.ivanstg.bandrepertoire

import es.ivanstg.bandrepertoire.data.ApiResult
import es.ivanstg.bandrepertoire.data.BandRepository
import es.ivanstg.bandrepertoire.data.EnsembleSelection
import es.ivanstg.bandrepertoire.data.model.AttendanceMark
import es.ivanstg.bandrepertoire.data.model.AttendanceRecord
import es.ivanstg.bandrepertoire.data.model.ConcertSummary
import es.ivanstg.bandrepertoire.data.model.Ensemble
import es.ivanstg.bandrepertoire.data.model.Member
import es.ivanstg.bandrepertoire.data.model.Piece
import es.ivanstg.bandrepertoire.data.model.Rehearsal
import es.ivanstg.bandrepertoire.data.model.SelectedEnsemble
import es.ivanstg.bandrepertoire.data.model.StalePiece
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow

/** Configurable in-memory repository that records how it was called. */
class FakeBandRepository : BandRepository {

    var ensemblesResult: ApiResult<List<Ensemble>> = ApiResult.Success(emptyList())
    var ensemblesCalls = 0

    var searchHandler: suspend (query: String, maxDifficulty: Int?) -> ApiResult<List<Piece>> =
        { _, _ -> ApiResult.Success(emptyList()) }
    val searchCalls = mutableListOf<Pair<String, Int?>>()

    var staleResult: ApiResult<List<StalePiece>> = ApiResult.Success(emptyList())
    var staleCalls = 0

    var nextConcertResult: ApiResult<ConcertSummary?> = ApiResult.Success(null)
    val nextConcertCalls = mutableListOf<LocalDate>()

    var rehearsalsResult: ApiResult<List<Rehearsal>> = ApiResult.Success(emptyList())
    var membersResult: ApiResult<List<Member>> = ApiResult.Success(emptyList())

    /** When null, a save succeeds and echoes the marks back as the rehearsal's attendance. */
    var saveFailure: ApiResult.Failure? = null
    val savedMarks = mutableListOf<Pair<Long, List<AttendanceMark>>>()

    override suspend fun ensembles(): ApiResult<List<Ensemble>> {
        ensemblesCalls++
        return ensemblesResult
    }

    override suspend fun searchPieces(ensembleId: Long, query: String, maxDifficulty: Int?): ApiResult<List<Piece>> {
        searchCalls += query to maxDifficulty
        return searchHandler(query, maxDifficulty)
    }

    override suspend fun stalePieces(ensembleId: Long): ApiResult<List<StalePiece>> {
        staleCalls++
        return staleResult
    }

    override suspend fun nextConcert(ensembleId: Long, today: LocalDate): ApiResult<ConcertSummary?> {
        nextConcertCalls += today
        return nextConcertResult
    }

    override suspend fun rehearsals(ensembleId: Long) = rehearsalsResult

    override suspend fun members(ensembleId: Long) = membersResult

    override suspend fun saveAttendance(rehearsalId: Long, marks: List<AttendanceMark>): ApiResult<Rehearsal> {
        savedMarks += rehearsalId to marks
        saveFailure?.let { return it }
        val rehearsal = (rehearsalsResult as ApiResult.Success).value.first { it.id == rehearsalId }
        return ApiResult.Success(rehearsal.copy(attendance = marks.map { AttendanceRecord(it.musicianId, it.attended, it.reason) }))
    }
}

class FakeEnsembleSelection(initial: SelectedEnsemble? = null) : EnsembleSelection {

    override val selected = MutableStateFlow(initial)

    override suspend fun select(ensemble: SelectedEnsemble) {
        selected.value = ensemble
    }

    override suspend fun clear() {
        selected.value = null
    }
}
