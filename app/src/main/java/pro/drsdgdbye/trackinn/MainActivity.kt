package pro.drsdgdbye.trackinn

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import pro.drsdgdbye.trackinn.data.settings.SettingsRepository
import pro.drsdgdbye.trackinn.data.settings.ThemeMode
import pro.drsdgdbye.trackinn.ui.theme.TrackinnTheme
import java.util.Locale

/**
 * Локализованный контекст, который при этом остаётся ActivityResultRegistryOwner,
 * чтобы rememberLauncherForActivityResult продолжал работать.
 */
private class LocalizedContext(
    base: Context,
    private val registry: ActivityResultRegistry
) : ContextWrapper(base), ActivityResultRegistryOwner {
    override val activityResultRegistry: ActivityResultRegistry
        get() = registry
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Применяем сохранённую локаль для системных API (Locale.getDefault, форматы дат)
        val settingsRepository = SettingsRepository(applicationContext)
        val lang = runBlocking { settingsRepository.language.first() }
        val localeList = if (lang != null) LocaleListCompat.forLanguageTags(lang) else LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(localeList)

        enableEdgeToEdge()
        setContent {
            val themeMode by settingsRepository.theme.collectAsState(initial = ThemeMode.SYSTEM)
            val language by settingsRepository.language.collectAsState(initial = null)
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            // Локализованный контекст: язык меняется мгновенно и без пересоздания
            // активности (без бликов), просто пересобирая композицию.
            val context = LocalContext.current
            val configuration = LocalConfiguration.current
            val registry = LocalActivityResultRegistryOwner.current?.activityResultRegistry
            val localizedContext = remember(context, language, configuration, registry) {
                if (language == null) context
                else {
                    val newConfig = Configuration(configuration)
                    newConfig.setLocales(android.os.LocaleList(Locale.forLanguageTag(language)))
                    LocalizedContext(
                        context.createConfigurationContext(newConfig),
                        registry ?: (context as? ActivityResultRegistryOwner)?.activityResultRegistry ?: (this@MainActivity).activityResultRegistry
                    )
                }
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
                TrackinnTheme(darkTheme = darkTheme) {
                    TrackinnApp()
                }
            }
        }
    }
}
