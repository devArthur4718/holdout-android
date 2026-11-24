package app.quitter

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import androidx.compose.ui.test.performScrollTo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OnboardingFlowTest {

  @get:Rule(order = 0) val language = AppLanguageRule()
  @get:Rule(order = 1) val rule = createEmptyComposeRule()
  private lateinit var scenario: ActivityScenario<MainActivity>

  @Before fun launchWithNoHabits() {
    // Onboarding only shows when nothing is saved, so start from an empty database.
    ApplicationProvider.getApplicationContext<QuitterApplication>().database.clearAllTables()
    scenario = ActivityScenario.launch(MainActivity::class.java)
  }

  @After fun close() = scenario.close()

  @Test
  fun welcome_newHabit_counter_home() {
    rule.onNodeWithText("Add my first habit").performClick()
    rule.onNodeWithText("What are you quitting?").assertExists()
    rule.onNodeWithText("Cigarettes").assertExists()

    // Picking another kind swaps the default name.
    rule.onNodeWithText("Gambling").performClick()
    rule.onNodeWithText("Cigarettes").assertDoesNotExist()

    // A blank name disables Start.
    rule.onNodeWithText("Name").performTextClearance()
    rule.onNodeWithText("Start the counter").assertIsNotEnabled()
    rule.onNodeWithText("Name").performTextInput("Online poker")

    rule.onNodeWithText("Just now").assertExists()
    // The keyboard may cover the button, so scroll to it first.
    rule.onNodeWithText("Start the counter").performScrollTo().performClick()

    // The counter reads the saved habit back.
    rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Your counter is running") }
    rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Online poker") }

    rule.onNodeWithText("Go to my habits").performClick()
    rule.onAllNodesWithText("Today").onFirst().assertExists() // title and tab
    rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("BET-FREE FOR") }
    rule.onNodeWithText("Online poker").assertExists() // its chip
    rule.onNodeWithText("Next trophy — 1 day").assertExists()

    // Logging a slip asks first, then restarts the counter.
    rule.onNodeWithText("Reset Counter").performClick()
    rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Log a slip") }
    rule.onNodeWithText("Stress").performClick()
    rule.onNodeWithText("Reset counter").performClick()
    rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Counter reset", substring = true) }
    rule.onNodeWithText("Log a slip").assertDoesNotExist()
    val dao = ApplicationProvider.getApplicationContext<QuitterApplication>().database.habitDao()
    val trigger = kotlinx.coroutines.runBlocking {
      val habit = dao.observeAll().first().single()
      dao.slips(habit.id).single().trigger
    }
    org.junit.Assert.assertEquals("Stress", trigger)

    // The habit's detail: tabs, and a reason.
    rule.onNodeWithText("Next trophy — 1 day").performClick()
    rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Tracking since", substring = true) }
    rule.onNodeWithText("Calendar").performClick()
    rule.onNodeWithText("Clean").assertExists()
    rule.onNodeWithText("Reasons").performClick()
    rule.onNodeWithText("Add a reason").performClick()
    rule.onNode(androidx.compose.ui.test.hasSetTextAction()).performTextInput("For my lungs")
    rule.onNodeWithText("Save").performClick()
    rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("For my lungs") }
    rule.onNodeWithContentDescription("Back").performClick()
    rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Today") }

    // Settings: deleting everything returns to the welcome screen.
    rule.onNodeWithContentDescription("Settings").performClick()
    rule.onNodeWithText("Export as CSV").assertExists()
    rule.onNodeWithText("Delete all data").performClick()
    rule.onNodeWithText("Delete everything").performClick()
    rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Add my first habit") }
  }

  @Test
  fun editAndDeleteAHabit() {
    rule.onNodeWithText("Add my first habit").performClick()
    rule.onNodeWithText("Start the counter").performScrollTo().performClick()
    rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Go to my habits") }
    rule.onNodeWithText("Go to my habits").performClick()
    rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Next trophy", substring = true) }

    rule.onNodeWithText("Next trophy — 1 day").performClick()
    rule.onNodeWithContentDescription("More options").performClick()
    rule.onNodeWithText("Edit habit").performClick()
    rule.onNodeWithText("Last time you did it").assertDoesNotExist()
    rule.onNodeWithText("Name").performTextClearance()
    rule.onNodeWithText("Name").performTextInput("Rolling tobacco")
    rule.onNodeWithText("Save").performScrollTo().performClick()
    rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Rolling tobacco") }

    rule.onNodeWithContentDescription("More options").performClick()
    rule.onNodeWithText("Delete habit").performClick()
    rule.onNodeWithText("Delete").performClick()
    rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("No habits yet") }
  }

  @Test
  fun relaunchWithSavedHabit_skipsOnboarding() {
    rule.onNodeWithText("Add my first habit").performClick()
    rule.onNodeWithText("Start the counter").performClick()
    rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Your counter is running") }

    scenario.close()
    scenario = ActivityScenario.launch(MainActivity::class.java)
    rule.waitUntil(5_000) { rule.onAllNodesWithTextExists("Today") }
    rule.onNodeWithText("Add my first habit").assertDoesNotExist()
  }
}

private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTextExists(text: String, substring: Boolean = false) =
  onAllNodes(androidx.compose.ui.test.hasText(text, substring)).fetchSemanticsNodes().isNotEmpty()
