package es.ivanstg.bandrepertoire.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** The band-repertoire-api endpoints the app uses. Null query parameters are left out of the URL. */
interface ApiService {

    @GET("api/ensembles")
    suspend fun ensembles(@Query("size") size: Int = 100, @Query("sort") sort: String = "name"): PageDto<EnsembleDto>

    @GET("api/pieces")
    suspend fun searchPieces(
        @Query("ensembleId") ensembleId: Long,
        @Query("q") q: String?,
        @Query("maxDifficulty") maxDifficulty: Int?,
        @Query("size") size: Int = 100,
        @Query("sort") sort: String = "title",
    ): PageDto<PieceDto>

    @GET("api/ensembles/{id}/pieces/stale")
    suspend fun stalePieces(@Path("id") ensembleId: Long, @Query("weeks") weeks: Int = 4): List<StalePieceDto>

    @GET("api/ensembles/{id}/concerts")
    suspend fun concerts(
        @Path("id") ensembleId: Long,
        @Query("sort") sort: String = "date,desc",
        @Query("size") size: Int = 20,
    ): PageDto<ConcertDto>

    @GET("api/concerts/{id}/summary")
    suspend fun concertSummary(@Path("id") concertId: Long): ConcertSummaryDto

    @GET("api/ensembles/{id}/rehearsals")
    suspend fun rehearsals(@Path("id") ensembleId: Long, @Query("size") size: Int = 20): PageDto<RehearsalDto>

    @GET("api/ensembles/{id}/members")
    suspend fun members(@Path("id") ensembleId: Long): List<MemberDto>

    @PUT("api/rehearsals/{id}/attendance")
    suspend fun recordAttendance(@Path("id") rehearsalId: Long, @Body body: AttendanceRequestDto): RehearsalDto
}
