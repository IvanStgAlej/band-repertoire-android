package es.ivanstg.bandrepertoire.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import es.ivanstg.bandrepertoire.data.model.SelectedEnsemble
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The ensemble the user works with. Remembered across launches; null until one is picked. */
interface EnsembleSelection {
    val selected: Flow<SelectedEnsemble?>
    suspend fun select(ensemble: SelectedEnsemble)
    suspend fun clear()
}

private val Context.settings by preferencesDataStore(name = "settings")

class DataStoreEnsembleSelection(private val context: Context) : EnsembleSelection {

    private val idKey = longPreferencesKey("ensemble_id")
    private val nameKey = stringPreferencesKey("ensemble_name")

    override val selected: Flow<SelectedEnsemble?> = context.settings.data.map { preferences ->
        preferences[idKey]?.let { SelectedEnsemble(it, preferences[nameKey].orEmpty()) }
    }

    override suspend fun select(ensemble: SelectedEnsemble) {
        context.settings.edit {
            it[idKey] = ensemble.id
            it[nameKey] = ensemble.name
        }
    }

    override suspend fun clear() {
        context.settings.edit { it.clear() }
    }
}
