package es.ivanstg.bandrepertoire.ui

import es.ivanstg.bandrepertoire.data.ApiResult

/** What a screen shows while it loads data: a spinner, the data, or an error the user can retry. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

fun <T> ApiResult<T>.toUiState(): UiState<T> = when (this) {
    is ApiResult.Success -> UiState.Content(value)
    is ApiResult.Failure -> UiState.Error(message)
}
