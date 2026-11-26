package app.holdout.ui.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.holdout.AppLanguageRule
import androidx.compose.ui.test.performClick
import app.holdout.ui.theme.HoldoutTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WelcomeScreenTest {

  @get:Rule(order = 0) val language = AppLanguageRule()
  @get:Rule(order = 1) val rule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun showsContent_andAddHabitButtonCallsBack() {
    var tapped = false
    val uiState = WelcomeViewModel().uiState.value
    rule.setContent {
      HoldoutTheme { WelcomeScreen(uiState = uiState, onAddHabit = { tapped = true }) }
    }

    rule.onNodeWithText("Holdout is a simple app", substring = true).assertExists()
    rule.onNodeWithText("See the money and time you didn't spend add up, live").assertDoesNotExist()
    rule.onNodeWithText("A trophy at every goal: 1 day, 1 week, 30 days, 6 months, 1 year…").assertExists()
    rule.onNodeWithText("Skip to the demo data").assertDoesNotExist()
    rule.onNodeWithText("Add my first habit").performClick()
    assertTrue(tapped)
  }
}
