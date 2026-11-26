package app.holdout

import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.holdout.ui.theme.HoldoutTheme
import java.util.Locale

/**
 * Dates, month names and weekday letters are formatted with the default locale. When the phone's
 * language isn't one the app is translated into, the text falls back to English, so the default
 * locale is switched to the app's language too. The phone's region stays, for currency and week start.
 */
private fun ComponentActivity.useAppLanguageForFormatting() {
  val app = resources.configuration.locales[0]
  // A language picked just for this app ("English") can come without a region. The currency
  // and week start then come from the phone's own region, not a blank one ("¤320").
  val region = app.country.ifEmpty { Resources.getSystem().configuration.locales[0].country }
  val language = getString(R.string.app_language)
  val locale = if (app.language == language && app.country.isNotEmpty()) app
  else Locale.Builder().setLanguage(language).setRegion(region).build()
  Locale.setDefault(locale)
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    // Before super: shows the launch screen and then switches to the app's theme.
    installSplashScreen()
    super.onCreate(savedInstanceState)
    useAppLanguageForFormatting()

    enableEdgeToEdge()
    setContent {
      HoldoutTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }
}
