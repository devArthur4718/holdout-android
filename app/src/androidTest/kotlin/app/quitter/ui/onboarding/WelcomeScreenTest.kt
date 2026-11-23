package app.quitter.ui.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.quitter.AppLanguageRule
import androidx.compose.ui.test.performClick
import app.quitter.ui.theme.QuitterTheme
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
      QuitterTheme { WelcomeScreen(uiState = uiState, onAddHabit = { tapped = true }) }
    }

    rule.onNodeWithText("Trophies at every hour and day milestone").assertExists()
    rule.onNodeWithText("Skip to the demo data").assertDoesNotExist()
    rule.onNodeWithText("Add my first habit").performClick()
    assertTrue(tapped)
  }
}
