package app.quitter.ui

import android.content.res.Configuration
import android.content.res.Resources
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/** Goal names and the arc's spoken sentence in each of the app's languages. */
class GoalLabelsTest {
  private fun resources(tag: String): Resources {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
    return context.createConfigurationContext(config).resources
  }

  private val ladder = listOf(1, 7, 30, 60, 90, 180, 365, 545, 730, 1095)

  @Test fun english() {
    assertEquals(
      listOf("1 day", "1 week", "30 days", "60 days", "90 days", "6 months", "1 year", "1½ years", "2 years", "3 years"),
      ladder.map { resources("en").goalLabel(it) },
    )
  }

  @Test fun brazilianPortuguese() {
    assertEquals(
      listOf("1 dia", "1 semana", "30 dias", "60 dias", "90 dias", "6 meses", "1 ano", "1 ano e meio", "2 anos", "3 anos"),
      ladder.map { resources("pt-BR").goalLabel(it) },
    )
  }

  @Test fun spanish() {
    assertEquals(
      listOf("1 día", "1 semana", "30 días", "60 días", "90 días", "6 meses", "1 año", "1 año y medio", "2 años", "3 años"),
      ladder.map { resources("es").goalLabel(it) },
    )
  }

  @Test fun spokenDuration() {
    val seconds = 755 * 86_400L + 13 * 3_600 + 36 * 60 + 42
    assertEquals("755 days, 13 hours and 36 minutes", resources("en").spokenDuration(seconds))
    assertEquals("755 dias, 13 horas e 36 minutos", resources("pt-BR").spokenDuration(seconds))
    assertEquals("755 días, 13 horas y 36 minutos", resources("es").spokenDuration(seconds))
    assertEquals("1 hour and 1 minute", resources("en").spokenDuration(3_660))
    assertEquals("4 minutes and 10 seconds", resources("en").spokenDuration(250))
  }
}
