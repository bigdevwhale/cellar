package app.cellar

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import app.cellar.data.Cellar
import app.cellar.data.CellarDatabase
import app.cellar.data.DemoSeed
import app.cellar.data.SettingsStore
import app.cellar.domain.Shared
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** A wish shared in from a shop, with the picture that came along, waiting for the laying form. */
data class Incoming(val shared: Shared, val photo: String?)

class AppContainer(context: Context) {
    val db = CellarDatabase.create(context)
    val settings = SettingsStore(context)
    val cellar = Cellar(context, db)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val incoming = MutableStateFlow<Incoming?>(null)

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }
}

class CellarApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        if (BuildConfig.DEMO) container.scope.launch { DemoSeed.seedIfEmpty(this@CellarApp, container) }
    }
}

val Context.container: AppContainer get() = (applicationContext as CellarApp).container

/** Resources in the in-app language, which AppCompat doesn't localise outside activities before Android 13. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}

/** Is this context in Russian? */
val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"
