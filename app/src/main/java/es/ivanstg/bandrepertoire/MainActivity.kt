package es.ivanstg.bandrepertoire

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import es.ivanstg.bandrepertoire.ui.navigation.BandRepertoireAppUi
import es.ivanstg.bandrepertoire.ui.theme.BandRepertoireTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as BandRepertoireApp).container
        setContent {
            BandRepertoireTheme {
                BandRepertoireAppUi(container)
            }
        }
    }
}
