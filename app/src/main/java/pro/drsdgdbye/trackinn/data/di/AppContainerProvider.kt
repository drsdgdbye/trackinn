package pro.drsdgdbye.trackinn.data.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import pro.drsdgdbye.trackinn.TrackinnApp

fun CreationExtras.appContainer(): AppContainer {
    val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
        ?: error("AppContainer requires an Application in CreationExtras")
    return (app as TrackinnApp).container
}
