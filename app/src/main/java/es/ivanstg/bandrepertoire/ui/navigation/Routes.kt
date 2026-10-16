package es.ivanstg.bandrepertoire.ui.navigation

import androidx.annotation.DrawableRes
import es.ivanstg.bandrepertoire.R
import kotlinx.serialization.Serializable

// Type-safe navigation destinations, one per bottom-bar tab.

@Serializable
data object Repertoire

@Serializable
data object NextConcert

@Serializable
data object Attendance

enum class Tab(val route: Any, val label: String, @param:DrawableRes val icon: Int) {
    REPERTOIRE(Repertoire, "Repertoire", R.drawable.ic_tab_repertoire),
    CONCERT(NextConcert, "Concert", R.drawable.ic_tab_concert),
    ATTENDANCE(Attendance, "Attendance", R.drawable.ic_tab_attendance),
}
