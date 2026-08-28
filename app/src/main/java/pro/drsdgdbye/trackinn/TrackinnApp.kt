package pro.drsdgdbye.trackinn

import android.app.Application
import pro.drsdgdbye.trackinn.data.di.AppContainer

class TrackinnApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
