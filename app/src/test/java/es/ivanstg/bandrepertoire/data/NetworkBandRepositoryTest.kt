package es.ivanstg.bandrepertoire.data

import es.ivanstg.bandrepertoire.buildOkHttpClient
import es.ivanstg.bandrepertoire.createApiService
import es.ivanstg.bandrepertoire.data.model.AttendanceMark
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NetworkBandRepositoryTest {

    private val server = MockWebServer()
    private lateinit var repository: NetworkBandRepository

    @Before
    fun setUp() {
        server.start()
        val baseUrl = server.url("/").toString()
        repository = NetworkBandRepository(createApiService(baseUrl, buildOkHttpClient(debug = false)), baseUrl)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun json(body: String, code: Int = 200, contentType: String = "application/json") {
        server.enqueue(MockResponse.Builder().code(code).addHeader("Content-Type", contentType).body(body).build())
    }

    @Test
    fun parsesThePiecePageAndSendsTheFilters() = runTest {
        json(
            """
            {"content": [{"id": 5, "ensembleId": 1, "title": "El Camino Real", "composer": "Alfred Reed",
              "arranger": null, "durationSeconds": 660, "difficulty": 4, "genre": "Concert band", "extra": true}],
             "page": 0, "size": 100, "totalElements": 1, "totalPages": 1}
            """,
        )

        val result = repository.searchPieces(ensembleId = 1, query = " reed ", maxDifficulty = 4)

        val pieces = (result as ApiResult.Success).value
        assertEquals(1, pieces.size)
        assertEquals("El Camino Real", pieces[0].title)
        assertEquals("Alfred Reed", pieces[0].composer)
        assertEquals(660, pieces[0].durationSeconds)
        val url = server.takeRequest().url
        assertEquals("/api/pieces", url.encodedPath)
        assertEquals("1", url.queryParameter("ensembleId"))
        assertEquals("reed", url.queryParameter("q"))
        assertEquals("4", url.queryParameter("maxDifficulty"))
    }

    @Test
    fun emptyQueryAndNoDifficultyAreNotSent() = runTest {
        json("""{"content": [], "page": 0, "size": 100, "totalElements": 0, "totalPages": 0}""")

        repository.searchPieces(ensembleId = 1, query = "  ", maxDifficulty = null)

        val url = server.takeRequest().url
        assertNull(url.queryParameter("q"))
        assertNull(url.queryParameter("maxDifficulty"))
    }

    @Test
    fun problemDetailBecomesItsDetailMessage() = runTest {
        json(
            """{"type": "about:blank", "title": "Unprocessable Content", "status": 422,
               "detail": "Musician 9 is not a member of ensemble 1", "instance": "/api/rehearsals/3/attendance"}""",
            code = 422,
            contentType = "application/problem+json",
        )

        val result = repository.saveAttendance(3, listOf(AttendanceMark(9, attended = true, reason = null)))

        assertEquals(ApiResult.Failure("Musician 9 is not a member of ensemble 1"), result)
    }

    @Test
    fun httpErrorWithoutProblemDetailIsGeneric() = runTest {
        json("<html>Bad gateway</html>", code = 502, contentType = "text/html")

        assertEquals(ApiResult.Failure("Server error (HTTP 502)"), repository.ensembles())
    }

    @Test
    fun unreachableServerGivesAHelpfulMessage() = runTest {
        val baseUrl = server.url("/").toString()
        server.close()

        val result = repository.ensembles()

        assertEquals(ApiResult.Failure("Cannot reach the API at $baseUrl. Is it running?"), result)
    }

    @Test
    fun unexpectedExceptionNeverLeaksDetails() = runTest {
        json("""{"content": "this is not a list"}""")

        val result = repository.ensembles() as ApiResult.Failure

        assertEquals("Something went wrong. Please try again.", result.message)
        assertFalse(result.message.contains("Exception"))
    }

    @Test
    fun nextConcertPicksTheEarliestUpcomingOne() = runTest {
        json(
            """
            {"content": [
              {"id": 3, "ensembleId": 1, "title": "Christmas Concert", "date": "2026-12-01", "venue": "Cathedral", "program": []},
              {"id": 2, "ensembleId": 1, "title": "Autumn Concert", "date": "2026-11-01", "venue": "Palau",
               "program": [{"position": 1, "pieceId": 5, "title": "Arsenal", "durationSeconds": 270, "difficulty": 3}]},
              {"id": 1, "ensembleId": 1, "title": "Summer Concert", "date": "2026-09-01", "venue": null, "program": []}
            ], "page": 0, "size": 20, "totalElements": 3, "totalPages": 1}
            """,
        )
        json(
            """
            {"concertId": 2, "title": "Autumn Concert", "date": "2026-11-01", "pieceCount": 1,
             "totalDurationSeconds": 270, "totalDuration": "4:30", "averageDifficulty": 3.0,
             "warnings": [{"pieceId": 5, "title": "Arsenal", "lastRehearsedAt": null,
                           "message": "Never rehearsed before the concert"}]}
            """,
        )

        val concert = (repository.nextConcert(1, LocalDate.of(2026, 10, 16)) as ApiResult.Success).value!!

        assertEquals("date,desc", server.takeRequest().url.queryParameter("sort"))
        assertEquals("/api/concerts/2/summary", server.takeRequest().url.encodedPath)
        assertEquals("Autumn Concert", concert.title)
        assertEquals(LocalDate.of(2026, 11, 1), concert.date)
        assertEquals("Palau", concert.venue)
        assertEquals("4:30", concert.totalDuration)
        assertEquals(listOf("Arsenal"), concert.program.map { it.title })
        assertEquals("Never rehearsed before the concert", concert.warnings.single().message)
    }

    @Test
    fun nextConcertIsNullWhenAllAreInThePast() = runTest {
        json(
            """{"content": [{"id": 1, "ensembleId": 1, "title": "Summer Concert", "date": "2026-09-01",
                "venue": null, "program": []}], "page": 0, "size": 20, "totalElements": 1, "totalPages": 1}""",
        )

        val result = repository.nextConcert(1, LocalDate.of(2026, 10, 16))

        assertEquals(ApiResult.Success(null), result)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun savesAttendanceWithThePutBody() = runTest {
        json(
            """{"id": 3, "ensembleId": 1, "startsAt": "2026-10-10T19:30:00", "location": "Music school",
                "notes": null, "pieces": [], "attendance": [
                  {"musicianId": 8, "name": "Hugo Castro", "attended": false, "reason": "Work shift"}]}""",
        )

        val result = repository.saveAttendance(
            3,
            listOf(AttendanceMark(1, attended = true, reason = null), AttendanceMark(8, attended = false, reason = "Work shift")),
        )

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/rehearsals/3/attendance", request.url.encodedPath)
        val body = request.body!!.utf8()
        assertTrue(body, body.contains(""""musicianId":8""") && body.contains(""""reason":"Work shift""""))
        assertFalse(body, body.contains("null"))
        val rehearsal = (result as ApiResult.Success).value
        assertEquals(false, rehearsal.attendance.single().attended)
    }
}
