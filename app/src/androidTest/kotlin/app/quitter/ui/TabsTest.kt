package app.quitter.ui

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import app.quitter.AppLanguageRule
import app.quitter.MainActivity
import app.quitter.QuitterApplication
import app.quitter.model.HabitDraft
import app.quitter.model.HabitKind
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** The Trophies and Insights tabs, on a habit with two slips and an urge. */
class TabsTest {
  @get:Rule(order = 0) val language = AppLanguageRule()
  @get:Rule(order = 1) val rule = createEmptyComposeRule()
  private lateinit var scenario: ActivityScenario<MainActivity>

  private fun daysAgo(days: Long, hour: Int) =
    LocalDate.now().minusDays(days).atTime(LocalTime.of(hour, 0)).atZone(ZoneId.systemDefault()).toInstant()

  @Before fun launchWithAHabit() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<QuitterApplication>()
    app.database.clearAllTables()
    // Streaks of 15 and 15 days, then 10 days and counting: 1 day and 1 week earned, 30 days next.
    val id = app.habitRepository.add(HabitDraft(HabitKind.Smoking, "Cigarettes", daysAgo(40, 20), null, null))
    app.habitRepository.logSlip(id, daysAgo(25, 20), null)
    app.habitRepository.logSlip(id, daysAgo(10, 20), null)
    app.habitRepository.logUrge(id, daysAgo(3, 21), daysAgo(3, 21).plusSeconds(300))
    scenario = ActivityScenario.launch(MainActivity::class.java)
  }

  @After fun close() = scenario.close()

  /** Scrolled-to nodes can sit under the bottom bar, so this taps through semantics instead of a touch. */
  private fun androidx.compose.ui.test.SemanticsNodeInteraction.tap() =
    performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)

  private fun waitFor(text: String) = rule.waitUntil(5_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

  private fun hasDescriptionStarting(prefix: String) =
    androidx.compose.ui.test.SemanticsMatcher("description starts with $prefix") { node ->
      node.config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription) { emptyList() }
        .any { it.startsWith(prefix) }
    }

  @Test fun trophiesShowEarnedAndNextAndTakeAPersonalGoal() {
    waitFor("Next trophy — 30 days")
    rule.onNodeWithText("Trophies").performClick()
    waitFor("All trophies")

    rule.onNode(hasDescriptionStarting("1 week, earned ")).assertExists()
    rule.onNode(hasContentDescription("30 days, not earned yet. Next")).assertExists()
    rule.onNode(hasContentDescription("1 year, not earned yet")).performScrollTo()

    // A goal at a standard trophy's days is refused inline.
    rule.onNodeWithText("Add personal goal").performScrollTo().tap()
    rule.onNodeWithText("New personal goal").assertExists()
    rule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("30")
    rule.onNodeWithText("Save").performClick()
    rule.onNodeWithText("There's already a trophy at 30 days.").assertExists()

    // 12 days was reached in the first streak, so the new goal is already earned.
    rule.onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("12")
    rule.onAllNodes(hasSetTextAction())[1].performTextInput("Beach trip")
    rule.onNodeWithText("Save").performClick()
    rule.waitUntil(5_000) {
      rule.onAllNodes(hasDescriptionStarting("Personal goal: Beach trip, 12 days, earned ")).fetchSemanticsNodes().isNotEmpty()
    }
    rule.onNodeWithText("New personal goal").assertDoesNotExist()

    // Tapping it opens the editor, where it can be deleted.
    rule.onNode(hasDescriptionStarting("Personal goal: Beach trip")).performScrollTo().tap()
    rule.onNodeWithText("Edit personal goal").assertExists()
    rule.onNodeWithText("Delete goal").performClick()
    rule.onNodeWithText("Delete this goal?").assertExists()
    rule.onAllNodesWithText("Delete goal")[1].performClick()
    rule.waitUntil(5_000) { rule.onAllNodes(hasDescriptionStarting("Personal goal:")).fetchSemanticsNodes().isEmpty() }
  }

  @Test fun insightsCountSlipsAndUrgesByTimeAndListStreaks() {
    waitFor("Next trophy — 30 days")
    rule.onNodeWithText("Insights").performClick()
    waitFor("When it's hardest")

    // Both slips at 20:00 and the urge at 21:00 are in the evening.
    rule.onNode(hasContentDescription("Evening: 2 slips, 1 urge")).assertExists()
    rule.onNode(hasContentDescription("Morning: 0 slips, 0 urges")).assertExists()
    rule.onNodeWithText("Your hardest time: Evening").assertExists()

    rule.onNodeWithText("Streak history").performScrollTo()
    rule.onNode(hasText("Current")).performScrollTo().assertExists()
    rule.onNode(hasText("Best")).performScrollTo().assertExists()
    rule.onNodeWithText("Past streaks show up here after a slip.").assertDoesNotExist()
  }
}
