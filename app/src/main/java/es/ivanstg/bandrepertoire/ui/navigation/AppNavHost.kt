package es.ivanstg.bandrepertoire.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import es.ivanstg.bandrepertoire.AppContainer
import es.ivanstg.bandrepertoire.data.model.SelectedEnsemble
import es.ivanstg.bandrepertoire.ui.components.EmptyView
import es.ivanstg.bandrepertoire.ui.components.LoadingView
import es.ivanstg.bandrepertoire.ui.ensemble.EnsemblePickerRoute
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Loading → picker when no ensemble is saved → the three tabs for the saved ensemble. */
@Composable
fun BandRepertoireAppUi(container: AppContainer) {
    // Wrapped so "not read yet" (null) differs from "nothing saved" (Selection(null)).
    val selection by remember { container.ensembleSelection.selected.map { Selection(it) } }
        .collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()

    when (val current = selection) {
        null -> LoadingView()
        else -> when (val ensemble = current.ensemble) {
            null -> Scaffold { padding -> EnsemblePickerRoute(Modifier.padding(padding)) }
            else -> MainScreen(ensemble, onChangeEnsemble = { scope.launch { container.ensembleSelection.clear() } })
        }
    }
}

private data class Selection(val ensemble: SelectedEnsemble?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(ensemble: SelectedEnsemble, onChangeEnsemble: () -> Unit) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ensemble.name) },
                actions = { TextButton(onClick = onChangeEnsemble) { Text("Change") } },
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = backStackEntry?.destination?.hasRoute(tab.route::class) == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        NavHost(navController, startDestination = Repertoire, modifier = Modifier.padding(padding)) {
            composable<Repertoire> { EmptyView("Repertoire") }
            composable<NextConcert> { EmptyView("Next concert") }
            composable<Attendance> { EmptyView("Attendance") }
        }
    }
}
