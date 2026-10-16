package es.ivanstg.bandrepertoire

import android.app.Application

class BandRepertoireApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
