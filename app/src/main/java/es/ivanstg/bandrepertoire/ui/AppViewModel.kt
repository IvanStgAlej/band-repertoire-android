package es.ivanstg.bandrepertoire.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import es.ivanstg.bandrepertoire.AppContainer
import es.ivanstg.bandrepertoire.BandRepertoireApp

/** Creates a ViewModel with its dependencies taken from the app's [AppContainer]. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(key: String? = null, crossinline create: (AppContainer) -> VM): VM {
    val container = (LocalContext.current.applicationContext as BandRepertoireApp).container
    return viewModel(key = key, factory = viewModelFactory { initializer { create(container) } })
}
