package app.quitter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import app.quitter.ui.theme.QuitterTheme
import java.util.Locale

/**
 * Dates, month names and weekday letters are formatted with the default locale. When the phone's
 * language isn't one the app is translated into, the text falls back to English, so the default
 * locale is switched to the app's language too. The phone's region stays, for currency and week start.
 */
private fun ComponentActivity.useAppLanguageForFormatting() {
  val device = resources.configuration.locales[0]
  val language = getString(R.string.app_language)
  val locale = if (device.language == language) device else Locale.Builder().setLanguage(language).setRegion(device.country).build()
  Locale.setDefault(locale)
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    useAppLanguageForFormatting()

    enableEdgeToEdge()
    setContent {
      QuitterTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }
}
