package es.ivanstg.bandrepertoire

import android.content.Context
import es.ivanstg.bandrepertoire.data.BandRepository
import es.ivanstg.bandrepertoire.data.DataStoreEnsembleSelection
import es.ivanstg.bandrepertoire.data.EnsembleSelection
import es.ivanstg.bandrepertoire.data.NetworkBandRepository
import es.ivanstg.bandrepertoire.data.remote.ApiService
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Manual dependency injection: builds every long-lived object once, for the whole app. Small enough to read
 * top to bottom, which is why there is no DI framework.
 */
class AppContainer(context: Context, baseUrl: String = BuildConfig.API_BASE_URL, debug: Boolean = BuildConfig.DEBUG) {

    val repository: BandRepository =
        NetworkBandRepository(createApiService(baseUrl, buildOkHttpClient(debug)), baseUrl)

    val ensembleSelection: EnsembleSelection = DataStoreEnsembleSelection(context.applicationContext)
}

/** HTTP logging is only ever added to debug builds: release builds must not log request or response data. */
fun buildOkHttpClient(debug: Boolean): OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .apply {
        if (debug) addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
    }
    .build()

private val apiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

fun createApiService(baseUrl: String, client: OkHttpClient): ApiService = Retrofit.Builder()
    .baseUrl(baseUrl)
    .client(client)
    .addConverterFactory(apiJson.asConverterFactory("application/json".toMediaType()))
    .build()
    .create(ApiService::class.java)
