package es.ivanstg.bandrepertoire

import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Security check S4: release builds must never log HTTP traffic. */
class AppContainerTest {

    @Test
    fun loggingIsOffInRelease() {
        val client = buildOkHttpClient(debug = false)

        assertFalse(client.interceptors.any { it is HttpLoggingInterceptor })
        assertFalse(client.networkInterceptors.any { it is HttpLoggingInterceptor })
    }

    @Test
    fun loggingIsOnInDebug() {
        assertTrue(buildOkHttpClient(debug = true).interceptors.any { it is HttpLoggingInterceptor })
    }
}
