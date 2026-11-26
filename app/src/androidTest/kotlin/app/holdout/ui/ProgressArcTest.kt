package app.holdout.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.Locales
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.then
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.holdout.model.goalProgress
import app.holdout.ui.counter.CounterScreen
import app.holdout.ui.counter.CounterUiState
import app.holdout.ui.theme.HoldoutTheme
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProgressArcTest {
  @get:Rule val rule = createComposeRule()

  private val longRun = 755 * 86_400L + 13 * 3_600 + 36 * 60 + 42

  /** The device may be in another language, so the content is shown in English whatever it is. */
  private fun english(content: @Composable () -> Unit) = rule.setContent {
    DeviceConfigurationOverride(DeviceConfigurationOverride.Locales(LocaleList("en")), content)
  }

  /** The arc's one spoken sentence. */
  private fun description(): String =
    rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription)).fetchSemanticsNode()
      .config[SemanticsProperties.ContentDescription].single()

  @Test fun theBigNumberIsGroupedByLanguage() {
    assertEquals("9,999", formatDayCount(9_999, Locale.forLanguageTag("en")))
    assertEquals("9.999", formatDayCount(9_999, Locale.forLanguageTag("pt-BR")))
    assertEquals("9.999", formatDayCount(9_999, Locale.forLanguageTag("es")))
    assertEquals("755", formatDayCount(755, Locale.forLanguageTag("es")))
  }

  @Test fun readsAsOneSentence() {
    english { HoldoutTheme { ProgressArc(goalProgress(longRun)) } }
    assertEquals("755 days, 13 hours and 36 minutes. 69% of the way to 3 years.", description())
  }

  @Test fun underADayItReadsHoursAndMinutes() {
    english { HoldoutTheme { ProgressArc(goalProgress(2 * 3_600L + 5 * 60), size = ProgressArcSize.Compact) } }
    assertEquals("2 hours and 5 minutes. 8% of the way to 1 day.", description())
  }

  @Test fun nineThousandDaysAtTheLargestFontStillRenders() {
    rule.setContent {
      DeviceConfigurationOverride(
        DeviceConfigurationOverride.Locales(LocaleList("en")) then
          DeviceConfigurationOverride.FontScale(2f) then
          DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 700.dp)),
      ) {
        HoldoutTheme { ProgressArc(goalProgress(9_999 * 86_400L + 23 * 3_600)) }
      }
    }
    assertEquals("9999 days, 23 hours and 0 minutes. 97% of the way to 28 years.", description())
  }

  @Test fun counterStartedJustNowPromisesTheFirstTrophyAtOneWeek() {
    english { HoldoutTheme { CounterScreen(CounterUiState.Running("Cigarettes", goalProgress(3), startedJustNow = true), onGoToHabits = {}) } }
    rule.onNodeWithText("Your counter is running").assertExists()
    rule.onNodeWithText("From now on every minute counts for something. Your first trophy is at 1 day.").assertExists()
  }

  @Test fun counterStartedInThePastNamesTheNextTrophy() {
    english { HoldoutTheme { CounterScreen(CounterUiState.Running("Cigarettes", goalProgress(longRun), startedJustNow = false), onGoToHabits = {}) } }
    rule.onNodeWithText("You're already on your way").assertExists()
    rule.onNodeWithText("The time since you stopped already counts. Your next trophy is at 3 years.").assertExists()
    rule.onNodeWithText("Your counter is running").assertDoesNotExist()
  }
}
